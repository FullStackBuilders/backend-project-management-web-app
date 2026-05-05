package com.project.projectmanagementapplication.service;

import jakarta.mail.MessagingException;
import jakarta.mail.internet.MimeMessage;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Service;

@Service
@Slf4j
public class EmailServiceImpl implements EmailService {

    private final JavaMailSender javaMailSender;

    @Value("${spring.mail.username}")
    private String fromEmail;

    @Autowired
    public EmailServiceImpl(JavaMailSender javaMailSender) {
        this.javaMailSender = javaMailSender;
    }

    @Override
    public void sendEmailWithToken(String userEmail, String link) throws MessagingException {
        MimeMessage mimeMessage = javaMailSender.createMimeMessage();
        MimeMessageHelper helper = new MimeMessageHelper(mimeMessage, true, "UTF-8");

        String subject = "🚀Project Team Invitation";
        String htmlContent = buildEmailTemplate(link);

        helper.setFrom(fromEmail);
        helper.setTo(userEmail);
        helper.setSubject(subject);
        helper.setText(htmlContent, true);

        try {
            javaMailSender.send(mimeMessage);
            log.info("Email sent successfully to: {}", userEmail);
        } catch (Exception e) {
            log.error("Failed to send email to: {}", userEmail, e);
            throw new MessagingException("Failed to send email to " + userEmail, e);
        }
    }

    @Override
    public void sendRemovalNotification(String userEmail, String projectName, String removedByName)
            throws MessagingException {
        MimeMessage mimeMessage = javaMailSender.createMimeMessage();
        MimeMessageHelper helper = new MimeMessageHelper(mimeMessage, true, "UTF-8");

        helper.setFrom(fromEmail);
        helper.setTo(userEmail);
        helper.setSubject("Project Team Update");
        helper.setText(buildRemovalEmailTemplate(projectName, removedByName), true);

        try {
            javaMailSender.send(mimeMessage);
            log.info("Removal notification sent to: {}", userEmail);
        } catch (Exception e) {
            log.error("Failed to send removal notification to: {}", userEmail, e);
            throw new MessagingException("Failed to send removal notification to " + userEmail, e);
        }
    }

    private String buildEmailTemplate(String invitationLink) {
        return """
            <html>
            <body style="font-family: Arial, sans-serif; line-height: 1.6; color: #333;">
                <div style="max-width: 600px; margin: 0 auto; padding: 20px; border: 1px solid #ddd; border-radius: 10px;">
                    <h2 style="color: #2c3e50; text-align: center;">Project Team Invitation</h2>
                    <p>👩‍💻Hello!👨‍💻</p>
                    <p>You have been invited to join an exciting project team. We'd love to have you collaborate with us!</p>
                    
                    <div style="text-align: center; margin: 30px 0;">
                        <a href="%s" 
                  `         style="background-color: #3498db; color: white; padding: 12px 25px; 
                                  text-decoration: none; border-radius: 5px; display: inline-block; 
                                  font-weight: bold;">
                            Accept Invitation
                        </a>
                    </div>
                    
                    <p>Or copy and paste this link in your browser:</p>
                    <p style="word-break: break-all; background-color: #f8f9fa; padding: 10px; border-radius: 5px;">
                        <a href="%s">%s</a>
                    </p>
                    
                    <hr style="margin: 30px 0; border: none; border-top: 1px solid #eee;">
                    <p style="font-size: 12px; color: #666; text-align: center;">
                        This invitation was sent from TeamBoard.
                        If you didn't expect this invitation, please ignore this email.
                    </p>
                </div>
            </body>
            </html>
            """.formatted(invitationLink, invitationLink, invitationLink);
    }

    private String buildRemovalEmailTemplate(String projectName, String removedByName) {
        return """
            <html>
            <body style="font-family: Arial, sans-serif; line-height: 1.6; color: #333;">
                <div style="max-width: 600px; margin: 0 auto; padding: 20px; border: 1px solid #ddd; border-radius: 10px;">
                    <h2 style="color: #2c3e50; text-align: center;">Project Team Update</h2>
                    <p>Hello,</p>
                    <p>This is to let you know that you have been removed from the project <strong>%s</strong> by <strong>%s</strong>.</p>
                    <p>You no longer have access to this project.</p>
                    <hr style="margin: 30px 0; border: none; border-top: 1px solid #eee;">
                    <p style="font-size: 12px; color: #666; text-align: center;">
                        This notification was sent from TeamBoard.
                        If you have any questions, please contact your project administrator.
                    </p>
                </div>
            </body>
            </html>
            """.formatted(projectName, removedByName);
    }
}