package com.ulms.auth;

import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;

@Getter
@Setter
public class LoginResponse {
    private String accessToken;
    private String tokenType;
    private Long userId;
    private String fullName;
    private String phone;
    private String avatarUrl;
    private String address;
    private String role;
    private LocalDateTime lastLoginAt;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    private Byte status;
}
