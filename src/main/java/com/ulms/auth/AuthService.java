package com.ulms.auth;

public interface  AuthService {
    void register(RegisterRequest registerRequest);
    void resendEmailVerification(EmailVerifyRequest request);
    void verifyEmail(EmailVerifyRequest request);
    void resetPassword(ForgotPasswordRequest request);
    LoginResponse login(LoginRequest request);
    LoginResponse loginWithGoogle(GoogleLoginRequest request);
}
