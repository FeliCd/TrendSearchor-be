package com.fpt.swp.security;

import com.fpt.swp.repository.InvalidatedTokenRepository;
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

@Component
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    private static final Logger log = LoggerFactory.getLogger(JwtAuthenticationFilter.class);

    private final JwtTokenProvider jwtTokenProvider;
    private final CustomUserDetailsService customUserDetailsService;
    private final InvalidatedTokenRepository invalidatedTokenRepository;

    public JwtAuthenticationFilter(JwtTokenProvider jwtTokenProvider,
                                   CustomUserDetailsService customUserDetailsService,
                                   InvalidatedTokenRepository invalidatedTokenRepository) {
        this.jwtTokenProvider = jwtTokenProvider;
        this.customUserDetailsService = customUserDetailsService;
        this.invalidatedTokenRepository = invalidatedTokenRepository;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain filterChain) throws ServletException, IOException {

        String origin = request.getHeader("Origin");
        log.debug("[JWT-FILTER] Request: {} {}, Origin: {}", request.getMethod(), request.getRequestURI(), origin);

        // Get JWT token from HTTP request
        String token = getTokenFromRequest(request);

        // Validate token chữ ký & hạn sử dụng
        if (StringUtils.hasText(token) && jwtTokenProvider.validateToken(token)) {

            // FR-01.3: Từ chối token đã bị invalidate (blacklist)
            String jti = jwtTokenProvider.getJti(token);
            if (invalidatedTokenRepository.existsByJti(jti)) {
                log.debug("[JWT-FILTER] Token {} is invalidated, skipping auth", jti);
                filterChain.doFilter(request, response);
                return;
            }

            // Get username from token
            String username = jwtTokenProvider.getUsername(token);
            log.debug("[JWT-FILTER] Token valid for user: {}", username);

            // Load user associated with token
            UserDetails userDetails = customUserDetailsService.loadUserByUsername(username);

            UsernamePasswordAuthenticationToken authenticationToken = new UsernamePasswordAuthenticationToken(
                    userDetails,
                    null,
                    userDetails.getAuthorities()
            );

            authenticationToken.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));

            SecurityContextHolder.getContext().setAuthentication(authenticationToken);
            log.debug("[JWT-FILTER] Authentication set for user: {}", username);
        } else {
            log.debug("[JWT-FILTER] No valid token found, continuing without auth");
        }

        filterChain.doFilter(request, response);
    }

    private String getTokenFromRequest(HttpServletRequest request) {
        String bearerToken = request.getHeader("Authorization");

        if (StringUtils.hasText(bearerToken) && bearerToken.startsWith("Bearer ")) {
            return bearerToken.substring(7);
        }

        return null;
    }
}
