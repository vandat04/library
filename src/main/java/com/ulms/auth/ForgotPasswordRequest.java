package com.ulms.auth;

import com.fasterxml.jackson.annotation.JsonAlias;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class ForgotPasswordRequest {
    private String email;
    private Boolean tokenType = true;
    @JsonAlias({"otp", "OTP"})
    private String OTP;
    private String newPassword;
}
