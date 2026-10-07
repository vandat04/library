package com.ulms.config;

import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

@Component
public class CustomPasswordEncoder implements PasswordEncoder {

    private final BCryptPasswordEncoder bcrypt = new BCryptPasswordEncoder();

    @Override
    public String encode(CharSequence rawPassword) {
        return bcrypt.encode(rawPassword);
    }

    @Override
    public boolean matches(CharSequence rawPassword, String encodedPassword) {
        if (rawPassword == null || encodedPassword == null || encodedPassword.isEmpty()) {
            return false;
        }

        String raw = rawPassword.toString().trim();
        String encoded = encodedPassword.trim();

        // Nếu chuỗi mật khẩu trong DB đã là dạng mã hóa BCrypt (bắt đầu bằng $2a$, $2b$, $2y$)
        if (encoded.startsWith("$2a$") || encoded.startsWith("$2b$") || encoded.startsWith("$2y$")) {
            return bcrypt.matches(raw, encoded);
        }

        // Ngược lại, so khớp trực tiếp bằng plaintext dành cho các tài khoản seed sẵn
        return raw.equals(encoded);
    }
}
