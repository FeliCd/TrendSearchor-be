package com.fpt.swp.config;

import com.fpt.swp.model.Gender;
import com.fpt.swp.model.Role;
import com.fpt.swp.model.User;
import com.fpt.swp.model.UserStatus;
import com.fpt.swp.repository.UserRepository;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.CommandLineRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

import java.time.LocalDate;

@Component
public class DataInitializer implements CommandLineRunner {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    @Value("${admin.username:admin}")
    private String adminUsername;

    @Value("${admin.password:admin}")
    private String adminPassword;

    @Value("${admin.dob:2000-01-01}")
    private String adminDob;

    @Value("${admin.mail:admin@system.com}")
    private String adminMail;

    @Value("${admin.phone:0123456789}")
    private String adminPhone;

    @Value("${admin.gender:MALE}")
    private String adminGender;

    @Value("${admin.workplace:System}")
    private String adminWorkplace;

    public DataInitializer(UserRepository userRepository, PasswordEncoder passwordEncoder) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
    }

    @Override
    public void run(String... args) throws Exception {
        String uName = adminUsername != null ? adminUsername.trim() : "admin";
        String uPass = adminPassword != null ? adminPassword.trim() : "admin";
        String uMail = adminMail != null ? adminMail.trim() : "admin@system.com";

        if (!userRepository.existsByUsername(uName) && !userRepository.existsByMail(uMail)) {
            User admin = User.builder()
                    .username(uName)
                    .password(passwordEncoder.encode(uPass))
                    .dob(LocalDate.parse(adminDob))
                    .mail(uMail)
                    .phone(adminPhone)
                    .gender(Gender.valueOf(adminGender.toUpperCase()))
                    .workplace(adminWorkplace)
                    .role(Role.ADMIN)
                    .status(UserStatus.ACTIVE)
                    .build();
            userRepository.save(admin);
            System.out.println("Default Admin user created successfully.");
        } else {
            System.out.println("Admin user already exists or email is taken.");
        }
    }
}
