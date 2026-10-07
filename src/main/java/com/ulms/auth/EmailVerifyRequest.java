package com.ulms.auth;

import com.fasterxml.jackson.annotation.JsonAlias;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class EmailVerifyRequest {
    private String email;
    private Boolean tokenType;

    @JsonAlias({"otp", "OTP"})
    private String OTP;
}
