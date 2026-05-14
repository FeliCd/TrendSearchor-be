package com.fpt.swp.journaltrendtracker.config;

import com.fpt.swp.journaltrendtracker.model.AcademicTitle;
import com.fpt.swp.journaltrendtracker.model.Gender;
import com.fpt.swp.journaltrendtracker.model.Role;
import com.fpt.swp.journaltrendtracker.model.User;
import com.fpt.swp.journaltrendtracker.repository.UserRepository;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.CommandLineRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

import java.time.LocalDate;

@Component
public class DataInitializer implements CommandLineRunner {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    @Value("${admin.username}")
    private String adminUsername;

    @Value("${admin.password}")
    private String adminPassword;

    @Value("${admin.dob}")
    private String adminDob;

    @Value("${admin.mail}")
    private String adminMail;

    @Value("${admin.phone}")
    private String adminPhone;

    @Value("${admin.gender}")
    private String adminGender;

    @Value("${admin.academicTitle}")
    private String adminAcademicTitle;

    @Value("${admin.workplace}")
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
                    .academicTitle(AcademicTitle.valueOf(adminAcademicTitle))
                    .workplace(adminWorkplace)
                    .role(Role.ADMIN)
                    .build();
            userRepository.save(admin);
            System.out.println("Default Admin user created successfully.");
        } else {
            System.out.println("Admin user already exists or email is taken.");
        }
    }
}
