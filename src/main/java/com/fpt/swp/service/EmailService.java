package com.fpt.swp.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

/**
 * Email service mock – trong production, replace bằng implementation thực
 * (JavaMailSender, SendGrid, AWS SES, v.v.)
 */
@Service
public class EmailService {

    private static final Logger log = LoggerFactory.getLogger(EmailService.class);

    /**
     * Gửi email reset mật khẩu.
     * Hiện tại chỉ log nội dung, chưa thực sự gửi email.
     *
     * @param to      địa chỉ email người nhận
     * @param subject tiêu đề
     * @param body    nội dung
     */
    public void sendEmail(String to, String subject, String body) {
        log.info("[EmailService] Sending email to: {}", to);
        log.info("[EmailService] Subject: {}", subject);
        log.info("[EmailService] Body: {}", body);
        // TODO: implement thực tế với JavaMailSender / SendGrid / AWS SES
    }

    /**
     * Gửi mật khẩu mới cho user quên mật khẩu.
     *
     * @param to              email người nhận
     * @param newPassword     mật khẩu mới
     */
    public void sendPasswordReset(String to, String newPassword) {
        sendEmail(
                to,
                "TrendSearchor – Password Reset",
                "Your new password is: " + newPassword + "\nPlease change it after logging in."
        );
    }
}
