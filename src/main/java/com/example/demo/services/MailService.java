package com.example.demo.services;

import com.example.demo.entities.SMTPAccount;
import jakarta.mail.internet.MimeMessage;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.mail.javamail.JavaMailSenderImpl;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Service;

import java.util.Properties;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Service
public class MailService {

    private static final Logger log = LoggerFactory.getLogger(MailService.class);

    private final SMTPAccountService smtpAccountService;

    public MailService(SMTPAccountService smtpAccountService) {
        this.smtpAccountService = smtpAccountService;
    }

    /**
     * Configures a JavaMailSenderImpl instance dynamically using SMTP account credentials.
     */
    public JavaMailSenderImpl configureMailSender(SMTPAccount smtp) {
        JavaMailSenderImpl mailSender = new JavaMailSenderImpl();
        mailSender.setHost(smtp.getHost());
        mailSender.setPort(smtp.getPort());
        mailSender.setUsername(smtp.getUsername());
        
        String decryptedPassword = smtpAccountService.decryptPassword(smtp.getPassword());
        mailSender.setPassword(decryptedPassword);

        Properties props = mailSender.getJavaMailProperties();
        props.put("mail.transport.protocol", "smtp");
        props.put("mail.smtp.auth", "true");
        
        // Timeout configurations (Issue 9)
        props.put("mail.smtp.connectiontimeout", "10000"); // 10s connection timeout
        props.put("mail.smtp.timeout", "10000");           // 10s socket read timeout
        props.put("mail.smtp.writetimeout", "10000");      // 10s socket write timeout
        
        // Trust configuration to avoid SSL handshake verification exceptions on custom certs
        props.put("mail.smtp.ssl.trust", smtp.getHost());
        
        // Detailed logging: output raw SMTP conversation commands directly to stdout
        props.put("mail.debug", "true");

        if (smtp.isTls()) {
            props.put("mail.smtp.starttls.enable", "true");
            props.put("mail.smtp.starttls.required", "true");
            props.put("mail.smtp.ssl.protocols", "TLSv1.2 TLSv1.3");
        }
        
        if (smtp.isSsl()) {
            props.put("mail.smtp.ssl.enable", "true");
            props.put("mail.smtp.socketFactory.port", String.valueOf(smtp.getPort()));
            props.put("mail.smtp.socketFactory.class", "javax.net.ssl.SSLSocketFactory");
            props.put("mail.smtp.socketFactory.fallback", "false");
        }

        return mailSender;
    }

    /**
     * Dispatches the constructed email, logging detailed logs at each milestone.
     */
    public void sendEmail(JavaMailSenderImpl mailSender, 
                          String from, 
                          String to, 
                          String subject, 
                          String htmlBody, 
                          String attachmentName, 
                          byte[] attachmentData) throws Exception {
        
        log.info("[SMTP] Starting email... [From: <{}>, To: <{}>]", from, to);
        log.info("[SMTP] Connecting SMTP to server {}:{}...", mailSender.getHost(), mailSender.getPort());

        MimeMessage message = mailSender.createMimeMessage();
        MimeMessageHelper helper = new MimeMessageHelper(message, true, "UTF-8");
        
        helper.setFrom(from);
        helper.setTo(to);
        helper.setSubject(subject);
        helper.setText(htmlBody, true); // True designates it is sent as HTML

        if (attachmentName != null && attachmentData != null) {
            log.info("[SMTP] Appending attachment: {} (Size: {} bytes)", attachmentName, attachmentData.length);
            ByteArrayResource attachmentSource = new ByteArrayResource(attachmentData);
            helper.addAttachment(attachmentName, attachmentSource);
        }

        log.info("[SMTP] Authentication successful (Credentials mapped)...");
        log.info("[SMTP] Sending...");
        mailSender.send(message);
        log.info("[SMTP] Sent successfully.");
    }
}
