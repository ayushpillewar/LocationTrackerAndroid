package com.majboormajdoor.locationtracker.services;
import java.util.*;
import javax.mail.*;
import javax.mail.internet.*;
import javax.activation.*;
import javax.mail.Session;
import javax.mail.Transport;
public class SendMailService {
    private String host;
    private String port;
    private String user;
    private String password;

    private String subject;
    private String message;
    private Session session;

    public SendMailService(String host, String port, String user, String password, String subject, String message) throws Exception{
        this.host = host;
        this.port = port;
        this.user = user;
        this.password = password;
        this.subject = subject;
        this.message = message;
        configureServer();
    }

    private void configureServer() throws Exception{

        // Configuration logic for the mail server

        // Getting system properties
        Properties properties = System.getProperties();

        // Setting up mail server
        properties.setProperty("mail.smtp.host", host);

        // creating session object to get properties
        session = Session.getDefaultInstance(properties);
    }

    public void sendEmail(String fromAddress,String toAddress) throws MessagingException {
        // MimeMessage object.
        MimeMessage message = new MimeMessage(session);

        // Set From Field: adding senders email to from field.
        message.setFrom(new InternetAddress(fromAddress));

        // Set To Field: adding recipient's email to from field.
        message.addRecipient(Message.RecipientType.TO, new InternetAddress(toAddress));

        // Set Subject: subject of the email
        message.setSubject(this.subject);

        // set body of the email.
        message.setText(this.message);
        // Send email.
        Transport.send(message);
        System.out.println("Mail successfully sent");

    }
}
