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

    @Value("${admin.mail:admin@mail.com}")
    private String adminMail;

    @Value("${admin.password:1}")
    private String adminPassword;

    @Value("${admin.dob:2000-01-01}")
    private String adminDob;

    @Value("${admin.phone:0123456789}")
    private String adminPhone;

    @Value("${admin.gender:MALE}")
    private String adminGender;

    @Value("${admin.workplace:System}")
    private String adminWorkplace;

    @Value("${admin.fullName:Administrator}")
    private String adminFullName;

    public DataInitializer(UserRepository userRepository, PasswordEncoder passwordEncoder) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
    }

    @Override
    public void run(String... args) throws Exception {
        String email = adminMail != null ? adminMail.trim() : "admin@mail.com";
        String password = adminPassword != null ? adminPassword.trim() : "1";
        String fullName = adminFullName != null ? adminFullName.trim() : "Administrator";

        if (!userRepository.existsByMail(email)) {
            User admin = User.builder()
                    .mail(email)
                    .password(passwordEncoder.encode(password))
                    .fullName(fullName)
                    .dob(LocalDate.parse(adminDob))
                    .phone(adminPhone)
                    .gender(Gender.valueOf(adminGender.toUpperCase()))
                    .workplace(adminWorkplace)
                    .role(Role.ADMIN)
                    .status(UserStatus.ACTIVE)
                    .builtin(true)
                    .build();
            userRepository.save(admin);
            System.out.println("Default Admin user created: " + email + " / " + password);
        } else {
            System.out.println("Admin user already exists.");
        }
    }
}
