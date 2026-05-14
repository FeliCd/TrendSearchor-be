package com.fpt.swp.journaltrendtracker.model;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(name = "users") // Đặt tên bảng là users để tránh trùng từ khóa hệ thống
public class User {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true)
    private String username;

    @Column(nullable = false)
    private String password;

    private LocalDate dob;

    @Column(nullable = false, unique = true)
    private String mail;

    private String phone; // sdt

    @Enumerated(EnumType.STRING)
    private Gender gender; // giới tính

    @Enumerated(EnumType.STRING)
    private AcademicTitle academicTitle; // học vị

    private String workplace; // đơn vị công tác

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private Role role; // quyền người dùng
}
