package com.ulms.util;

import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Service;

@Service
public class EmailServiceImpl implements EmailService {

    private final JavaMailSender mailSender;

    public EmailServiceImpl(JavaMailSender mailSender) {
        this.mailSender = mailSender;
    }

    @Override
    public void sendOtpEmail(String toEmail, String otp) {

        SimpleMailMessage message = new SimpleMailMessage();

        message.setTo(toEmail);
        message.setSubject("Mã OTP xác thực tài khoản");
        message.setText(
                "Xin chào,\n\n" +
                        "Mã OTP của bạn là: " + otp + "\n\n" +
                        "Mã OTP có hiệu lực trong 5 phút.\n" +
                        "Vui lòng không chia sẻ mã này cho người khác.\n\n" +
                        "Trân trọng."
        );

        mailSender.send(message);
    }
}