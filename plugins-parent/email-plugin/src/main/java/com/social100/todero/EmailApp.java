package com.social100.todero;

import com.social100.todero.handler.ImapProtocolHandler;
import com.social100.todero.handler.MailProtocolHandler;

import java.util.Properties;

public class EmailApp {
    public static void main(String[] args) {
        String protocol = "imap"; // Or "pop3"
        String host = protocol.equals("imap") ? "imap.gmail.com" : "pop.gmail.com";
        int port = protocol.equals("imap") ? 993 : 995;
        boolean useSSL = true;
        String username = "your-email@gmail.com";
        String password = "your-app-password";
        boolean useOAuth2 = true; // Change to true for OAuth2
        String provider = "gmail"; // Options: gmail, outlook, yahoo

        Properties imapProperties = EmailConfig.getProperties(protocol, host, port, useSSL);

        Properties smtpProperties = EmailConfig.getProperties("smtp", "smtp.gmail.com", 587, true);

        MailProtocolHandler protocolHandler = new ImapProtocolHandler(imapProperties, username, password);
        EmailService emailService = new EmailService(protocolHandler, smtpProperties, username, password, useOAuth2, provider);

        // Fetch emails
        emailService.fetchEmails("INBOX");

        // Send an email
        emailService.sendEmail("recipient@example.com", "Test Subject", "This is a test email.");
    }
}