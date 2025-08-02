package com.project.projectmanagementapplication.service;

import jakarta.mail.MessagingException;
import jakarta.mail.internet.MimeMessage;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Service;

@Service
public class EmailServiceImpl implements EmailService{

    private final JavaMailSender javaMailSender;

    @Autowired
    public EmailServiceImpl(JavaMailSender javaMailSender) {
        this.javaMailSender = javaMailSender;
    }

    @Override
    public void sendEmailWithToken(String userEmail, String link) throws MessagingException {
        MimeMessage mimeMessage = javaMailSender.createMimeMessage();
        MimeMessageHelper helper = new MimeMessageHelper(mimeMessage, "utf-8");

        String subject = "Join Project Team Invitation";
        String text = "You have been invited to join a project team. Please click the link below to accept the invitation:\n" + link;

        helper.setSubject(subject);
        helper.setText(text, true);
        helper.setTo(userEmail);

        try
        {
            javaMailSender.send(mimeMessage);
        } catch (Exception e) {
            throw new MessagingException("Failed to send email to " + userEmail, e);
        }

    }
}
