package com.ulms.controller;

import com.ulms.exception.ApiResponse;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/api/test")
public class TestRoleController {

    // 1. API dành cho mọi tài khoản đã đăng nhập (Có token là vào được)
    @GetMapping("/profile")
    public ResponseEntity<ApiResponse<Map<String, Object>>> getProfile() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();

        Map<String, Object> data = new HashMap<>();
        data.put("email", auth.getName());
        data.put("roles", auth.getAuthorities().toString());
        data.put("message", "Token hợp lệ! Bạn đã đăng nhập thành công.");

        return ResponseEntity.ok(new ApiResponse<>(true, "Xác thực thành công", data));
    }

    // 2. API chỉ dành cho Độc giả (READER)
    @GetMapping("/reader-only")
    @PreAuthorize("hasRole('READER')")
    public ResponseEntity<ApiResponse<String>> testReaderOnly() {
        return ResponseEntity.ok(new ApiResponse<>(true, "Thành công: Bạn đang truy cập với quyền READER (Độc giả)", null));
    }

    // 3. API chỉ dành cho Thủ thư (LIBRARIAN)
    @GetMapping("/librarian-only")
    @PreAuthorize("hasRole('LIBRARIAN')")
    public ResponseEntity<ApiResponse<String>> testLibrarianOnly() {
        return ResponseEntity.ok(new ApiResponse<>(true, "Thành công: Bạn đang truy cập với quyền LIBRARIAN (Thủ thư)", null));
    }

    // 4. API chỉ dành cho Quản trị viên (ADMIN)
    @GetMapping("/admin-only")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<String>> testAdminOnly() {
        return ResponseEntity.ok(new ApiResponse<>(true, "Thành công: Bạn đang truy cập với quyền ADMIN (Quản trị viên)", null));
    }
}
