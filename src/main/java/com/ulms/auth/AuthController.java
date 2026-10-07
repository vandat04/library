package com.ulms.auth;

import com.ulms.exception.ApiResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/home/auth")
@RequiredArgsConstructor
public class AuthController {

    private final AuthService authService;

    // =========================
    // REGISTER
    @PostMapping("/register")
    public ResponseEntity<ApiResponse<Void>> register(
            @RequestBody RegisterRequest request) {

        authService.register(request);

        ApiResponse<Void> response = new ApiResponse<>(
                true,
                "Đăng ký tài khoản thành công. Vui lòng xác thực email",
                null
        );

        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    // =========================
    // RESEND VERIFICATION MAIL
    @GetMapping("/resend-OTP-verify")
    public ResponseEntity<ApiResponse<Void>> resendOTP(
            @RequestBody EmailVerifyRequest request) {
        authService.resendEmailVerification(request);

        ApiResponse<Void> response = new ApiResponse<>(
                true,
                "Gửi mã thành công. Vui lòng kiểm tra mail",
                null
        );

        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(response);
    }

    // =========================
    // VERIFY ACCOUNT
    @PostMapping("/verify-email")
    public ResponseEntity<ApiResponse<Void>> verificationMail(
            @RequestBody EmailVerifyRequest request) {
        authService.verifyEmail(request);

        ApiResponse<Void> response = new ApiResponse<>(
                true,
                "Xác thực tài khoản thành công. Vui lòng đăng nhập lại",
                null
        );

        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(response);
    }

    // =========================
    // FORGOT PASSWORD
    @PostMapping("/reset-password")
    public ResponseEntity<ApiResponse<Void>> resetPassword(
            @RequestBody ForgotPasswordRequest request) {
        authService.resetPassword(request);

        ApiResponse<Void> response = new ApiResponse<>(
                true,
                "Đã đặt lại mật khẩu thành công. Vui lòng đăng nhập lại",
                null
        );

        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(response);
    }


    // =========================
    // LOGIN
    @PostMapping("/login")
    public ResponseEntity<ApiResponse<LoginResponse>> login(
            @RequestBody LoginRequest request) {

        LoginResponse loginResponse = authService.login(request);

        ApiResponse<LoginResponse> response = new ApiResponse<>(
                true,
                "Đăng nhập thành công",
                loginResponse
        );

        return ResponseEntity.ok(response);
    }

    // =========================
    // LOGIN GOOGLE
    @PostMapping("/google")
    public ResponseEntity<ApiResponse<LoginResponse>> loginGoogle(
            @RequestBody GoogleLoginRequest request) {

        LoginResponse loginResponse = authService.loginWithGoogle(request);

        ApiResponse<LoginResponse> response = new ApiResponse<>(
                true,
                "Đăng nhập bằng Google thành công",
                loginResponse
        );

        return ResponseEntity.ok(response);
    }
}
