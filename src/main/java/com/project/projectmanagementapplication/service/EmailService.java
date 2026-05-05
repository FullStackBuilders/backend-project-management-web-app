package com.project.projectmanagementapplication.service;


import jakarta.mail.MessagingException;

public interface EmailService {
    void sendEmailWithToken(String userEmail, String link) throws MessagingException;

    void sendRemovalNotification(String userEmail, String projectName, String removedByName)
            throws MessagingException;
}
