package com.aicode.review.service;

import com.aicode.review.dto.ReviewResponseDTO;
import com.aicode.review.entity.User;
import jakarta.mail.internet.MimeMessage;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Service;

@Slf4j
@Service
public class EmailService {

    private final JavaMailSender mailSender;
    private final PdfReportService pdfReportService;

    @Value("${app.email.enabled:false}")
    private boolean emailEnabled;

    @Value("${app.email.from:no-reply@aicodereview.local}")
    private String fromAddress;

    public EmailService(JavaMailSender mailSender, PdfReportService pdfReportService) {
        this.mailSender = mailSender;
        this.pdfReportService = pdfReportService;
    }

    public void sendReviewCompleteEmail(User user, ReviewResponseDTO review) {
        if (!emailEnabled) {
            log.debug("Email notifications disabled (app.email.enabled=false); skipping notification to {}", user.getEmail());
            return;
        }
        if (!user.isEmailNotifications()) {
            log.debug("User {} has opted out of email notifications; skipping", user.getEmail());
            return;
        }

        try {
            MimeMessage message = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message, true);

            helper.setFrom(fromAddress);
            helper.setTo(user.getEmail());
            helper.setSubject("Your code review for \"" + review.getProjectName() + "\" is ready (" + review.getReviewScore() + "/100)");

            String html = pdfReportService.generateHtml(review);
            helper.setText(html, true);

            byte[] pdfBytes = pdfReportService.generatePdf(review);
            helper.addAttachment("review-" + review.getReviewId() + ".pdf", new ByteArrayResource(pdfBytes));

            mailSender.send(message);
            log.info("Sent full review-complete email (with PDF attachment) to {}", user.getEmail());
        } catch (Exception e) {
            log.warn("Failed to send review-complete email to {}: {}", user.getEmail(), e.getMessage());
        }
    }

    public void sendTeamInviteEmail(String toEmail, String teamName, String inviterName) {
        if (!emailEnabled) {
            log.debug("Email notifications disabled; skipping team invite email to {}", toEmail);
            return;
        }
        try {
            SimpleMailMessage message = new SimpleMailMessage();
            message.setFrom(fromAddress);
            message.setTo(toEmail);
            message.setSubject(inviterName + " added you to the \"" + teamName + "\" team");
            message.setText(String.format(
                    "Hi,%n%n%s added you to the \"%s\" team workspace on AI Code Review Assistant.%n%nLog in to view shared project reviews.%n%n— AI Code Review Assistant",
                    inviterName, teamName));
            mailSender.send(message);
        } catch (Exception e) {
            log.warn("Failed to send team invite email to {}: {}", toEmail, e.getMessage());
        }
    }
}