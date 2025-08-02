package com.project.projectmanagementapplication.service;


import jakarta.mail.MessagingException;

public interface EmailService {
    public void sendEmailWithToken(String userEmail, String link) throws MessagingException;
}
