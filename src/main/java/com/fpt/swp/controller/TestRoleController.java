package com.fpt.swp.controller;

import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/test-roles")
public class TestRoleController {

    // ─────────────────────────────────────────────
    // Endpoint chỉ dành cho Admin
    // ─────────────────────────────────────────────
    @GetMapping("/admin")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<String> adminAccess() {
        return ResponseEntity.ok("Admin Content - Bạn đã truy cập thành công với quyền ADMIN.");
    }

    // ─────────────────────────────────────────────
    // Endpoint dành cho Researcher
    // ─────────────────────────────────────────────
    @GetMapping("/researcher")
    @PreAuthorize("hasRole('RESEARCHER')")
    public ResponseEntity<String> researcherAccess() {
        return ResponseEntity.ok("Researcher Content - Bạn đã truy cập thành công với quyền RESEARCHER.");
    }

    // ─────────────────────────────────────────────
    // Endpoint dành cho Lecturer hoặc Student (cả 2 đều truy cập được)
    // ─────────────────────────────────────────────
    @GetMapping("/academic")
    @PreAuthorize("hasAnyRole('LECTURER', 'STUDENT')")
    public ResponseEntity<String> academicAccess() {
        return ResponseEntity.ok("Academic Content - Bạn đã truy cập thành công với quyền LECTURER hoặc STUDENT.");
    }

    // ─────────────────────────────────────────────
    // Endpoint dành cho mọi User đã đăng nhập
    // ─────────────────────────────────────────────
    @GetMapping("/user")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<String> userAccess() {
        return ResponseEntity.ok("User Content - Bất kỳ tài khoản nào đăng nhập cũng thấy được.");
    }
}
