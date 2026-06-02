package com.fpt.swp.security;

import com.fpt.swp.model.User;
import com.fpt.swp.repository.InvalidatedTokenRepository;
import com.fpt.swp.repository.UserRepository;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

@Component
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    private static final Logger log = LoggerFactory.getLogger(JwtAuthenticationFilter.class);

    private final JwtTokenProvider jwtTokenProvider;
    private final CustomUserDetailsService customUserDetailsService;
    private final InvalidatedTokenRepository invalidatedTokenRepository;
    private final UserRepository userRepository;

    public JwtAuthenticationFilter(JwtTokenProvider jwtTokenProvider,
                                  CustomUserDetailsService customUserDetailsService,
                                  InvalidatedTokenRepository invalidatedTokenRepository,
                                  UserRepository userRepository) {
        this.jwtTokenProvider = jwtTokenProvider;
        this.customUserDetailsService = customUserDetailsService;
        this.invalidatedTokenRepository = invalidatedTokenRepository;
        this.userRepository = userRepository;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain filterChain) throws ServletException, IOException {

        String origin = request.getHeader("Origin");
        log.debug("[JWT-FILTER] Request: {} {}, Origin: {}", request.getMethod(), request.getRequestURI(), origin);

        String token = getTokenFromRequest(request);

        if (StringUtils.hasText(token) && jwtTokenProvider.validateToken(token)) {

            String jti = jwtTokenProvider.getJti(token);
            if (invalidatedTokenRepository.existsByJti(jti)) {
                log.debug("[JWT-FILTER] Token {} is invalidated, skipping auth", jti);
                filterChain.doFilter(request, response);
                return;
            }

            String email = jwtTokenProvider.getSubject(token);
            Integer tokenVersion = jwtTokenProvider.getTokenVersion(token);

            if (tokenVersion != null) {
                User user = userRepository.findByMail(email).orElse(null);
                if (user != null) {
                    Integer dbVersion = user.getTokenVersion();
                    if (dbVersion != null && !dbVersion.equals(tokenVersion)) {
                        log.warn("[JWT-FILTER] Token version mismatch for {}: token={}, db={}. Rejecting.",
                                email, tokenVersion, dbVersion);
                        sendUnauthorizedResponse(response, "Session invalidated. Please log in again.");
                        return;
                    }
                }
            }

            log.debug("[JWT-FILTER] Token valid for email: {}", email);

            UserDetails userDetails = customUserDetailsService.loadUserByMail(email);

            UsernamePasswordAuthenticationToken authenticationToken = new UsernamePasswordAuthenticationToken(
                    userDetails,
                    null,
                    userDetails.getAuthorities()
            );

            authenticationToken.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));

            SecurityContextHolder.getContext().setAuthentication(authenticationToken);
            log.debug("[JWT-FILTER] Authentication set for email: {}", email);
        } else {
            log.debug("[JWT-FILTER] No valid token found, continuing without auth");
        }

        filterChain.doFilter(request, response);
    }

    private void sendUnauthorizedResponse(HttpServletResponse response, String message) throws IOException {
        response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
        response.setContentType("application/json");
        response.setCharacterEncoding("UTF-8");
        Map<String, Object> body = new HashMap<>();
        body.put("status", 401);
        body.put("message", message);
        body.put("code", "TOKEN_INVALIDATED");
        response.getWriter().write(
                "{\"status\":401,\"message\":\"" + message.replace("\"", "\\\"") + "\",\"code\":\"TOKEN_INVALIDATED\"}"
        );
    }

    private String getTokenFromRequest(HttpServletRequest request) {
        String bearerToken = request.getHeader("Authorization");

        if (StringUtils.hasText(bearerToken) && bearerToken.startsWith("Bearer ")) {
            return bearerToken.substring(7);
        }

        return null;
    }
}
