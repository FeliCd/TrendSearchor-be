package com.fpt.swp.journaltrendtracker.controller;

import com.fpt.swp.journaltrendtracker.dto.JwtAuthResponse;
import com.fpt.swp.journaltrendtracker.dto.LoginRequest;
import com.fpt.swp.journaltrendtracker.dto.RegisterRequest;
import com.fpt.swp.journaltrendtracker.model.Role;
import com.fpt.swp.journaltrendtracker.model.User;
import com.fpt.swp.journaltrendtracker.repository.UserRepository;
import com.fpt.swp.journaltrendtracker.security.JwtTokenProvider;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.password.PasswordEncoder;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/auth")
public class AuthController {

    private final AuthenticationManager authenticationManager;
    private final JwtTokenProvider jwtTokenProvider;
    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    public AuthController(AuthenticationManager authenticationManager, 
                          JwtTokenProvider jwtTokenProvider, 
                          UserRepository userRepository, 
                          PasswordEncoder passwordEncoder) {
        this.authenticationManager = authenticationManager;
        this.jwtTokenProvider = jwtTokenProvider;
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
    }

    @PostMapping("/login")
    public ResponseEntity<JwtAuthResponse> authenticateUser(@Valid @RequestBody LoginRequest loginRequest) {

        Authentication authentication = authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(
                        loginRequest.getUsername(),
                        loginRequest.getPassword()
                )
        );

        SecurityContextHolder.getContext().setAuthentication(authentication);

        String token = jwtTokenProvider.generateToken(authentication);

        return ResponseEntity.ok(new JwtAuthResponse(token));
    }

    @PostMapping("/register")
    public ResponseEntity<?> registerUser(@Valid @RequestBody RegisterRequest registerRequest) {
        // Kiểm tra xem username đã tồn tại chưa
        if (userRepository.existsByUsername(registerRequest.getUsername())) {
            return new ResponseEntity<>("Username is already taken!", HttpStatus.BAD_REQUEST);
        }

        // Kiểm tra xem email đã tồn tại chưa
        if (userRepository.existsByMail(registerRequest.getMail())) {
            return new ResponseEntity<>("Email is already taken!", HttpStatus.BAD_REQUEST);
        }

        // Sử dụng Lombok Builder để tạo đối tượng User dễ dàng và gọn gàng
        User user = User.builder()
                .username(registerRequest.getUsername())
                .password(passwordEncoder.encode(registerRequest.getPassword())) // Mã hóa mật khẩu
                .dob(registerRequest.getDob())
                .mail(registerRequest.getMail())
                .phone(registerRequest.getPhone())
                .gender(registerRequest.getGender())
                .academicTitle(registerRequest.getAcademicTitle())
                .workplace(registerRequest.getWorkplace())
                .role(registerRequest.getRole() != null ? registerRequest.getRole() : Role.USER) // Default to USER
                .build();

        userRepository.save(user);

        return new ResponseEntity<>("User registered successfully", HttpStatus.CREATED);
    }
}
