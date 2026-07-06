package com.example.demo.services;

import com.example.demo.entities.SMTPAccount;
import com.example.demo.repositories.SMTPAccountRepository;
import jakarta.mail.Session;
import jakarta.mail.Transport;
import org.springframework.mail.javamail.JavaMailSenderImpl;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;
import java.util.Properties;

@Service
public class SMTPAccountService {

    private final SMTPAccountRepository smtpAccountRepository;
    private final EncryptionService encryptionService;

    public SMTPAccountService(SMTPAccountRepository smtpAccountRepository, EncryptionService encryptionService) {
        this.smtpAccountRepository = smtpAccountRepository;
        this.encryptionService = encryptionService;
    }

    public List<SMTPAccount> getAllAccounts() {
        return smtpAccountRepository.findAll();
    }

    public Optional<SMTPAccount> getAccountById(Long id) {
        return smtpAccountRepository.findById(id);
    }

    public List<SMTPAccount> getActiveAccounts() {
        return smtpAccountRepository.findByActive(true);
    }

    public SMTPAccount saveAccount(SMTPAccount account) {
        if (account.getId() != null) {
            SMTPAccount existing = smtpAccountRepository.findById(account.getId()).orElse(null);
            if (existing != null) {
                if (account.getPassword() == null || account.getPassword().isEmpty()) {
                    account.setPassword(existing.getPassword());
                } else {
                    account.setPassword(encryptionService.encrypt(account.getPassword()));
                }
            } else {
                if (account.getPassword() != null && !account.getPassword().isEmpty()) {
                    account.setPassword(encryptionService.encrypt(account.getPassword()));
                }
            }
        } else {
            if (account.getPassword() != null && !account.getPassword().isEmpty()) {
                account.setPassword(encryptionService.encrypt(account.getPassword()));
            }
        }
        return smtpAccountRepository.save(account);
    }

    public void deleteAccount(Long id) {
        smtpAccountRepository.deleteById(id);
    }

    public String decryptPassword(String encryptedPassword) {
        return encryptionService.decrypt(encryptedPassword);
    }

    public boolean testConnection(SMTPAccount account, String rawPassword) {
        JavaMailSenderImpl mailSender = new JavaMailSenderImpl();
        mailSender.setHost(account.getHost());
        mailSender.setPort(account.getPort());
        mailSender.setUsername(account.getUsername());
        mailSender.setPassword(rawPassword);

        Properties props = mailSender.getJavaMailProperties();
        props.put("mail.transport.protocol", "smtp");
        props.put("mail.smtp.auth", "true");
        props.put("mail.smtp.timeout", "5000");
        props.put("mail.smtp.connectiontimeout", "5000");

        if (account.isTls()) {
            props.put("mail.smtp.starttls.enable", "true");
            props.put("mail.smtp.starttls.required", "true");
        }
        if (account.isSsl()) {
            props.put("mail.smtp.socketFactory.port", String.valueOf(account.getPort()));
            props.put("mail.smtp.socketFactory.class", "javax.net.ssl.SSLSocketFactory");
            props.put("mail.smtp.socketFactory.fallback", "false");
        }

        try {
            Session session = Session.getInstance(props);
            Transport transport = session.getTransport("smtp");
            transport.connect(account.getHost(), account.getPort(), account.getUsername(), rawPassword);
            transport.close();
            return true;
        } catch (Exception e) {
            e.printStackTrace();
            return false;
        }
    }
}
