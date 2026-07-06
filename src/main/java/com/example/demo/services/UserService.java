package com.example.demo.services;

import com.example.demo.entities.SMTPAccount;
import com.example.demo.entities.User;
import com.example.demo.repositories.SMTPAccountRepository;
import com.example.demo.repositories.UserRepository;
import jakarta.annotation.PostConstruct;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.Optional;
import java.util.UUID;

@Service
public class UserService {

    private final UserRepository userRepository;
    private final SMTPAccountRepository smtpAccountRepository;
    private final PasswordEncoder passwordEncoder;
    private final EncryptionService encryptionService;

    public UserService(UserRepository userRepository,
                       SMTPAccountRepository smtpAccountRepository,
                       PasswordEncoder passwordEncoder,
                       EncryptionService encryptionService) {
        this.userRepository = userRepository;
        this.smtpAccountRepository = smtpAccountRepository;
        this.passwordEncoder = passwordEncoder;
        this.encryptionService = encryptionService;
    }

    /**
     * Seeds default Admin and User credentials, plus the primary Gmail SMTP connection on startup.
     */
    @PostConstruct
    public void seedDatabase() {
        // Seed Users
        if (userRepository.count() == 0) {
            User admin = new User();
            admin.setUsername("admin");
            admin.setPassword(passwordEncoder.encode("admin"));
            admin.setEmail("admin@example.com");
            admin.setRole("ADMIN");
            userRepository.save(admin);

            User user = new User();
            user.setUsername("user");
            user.setPassword(passwordEncoder.encode("user"));
            user.setEmail("user@example.com");
            user.setRole("USER");
            userRepository.save(user);
        }

        // Seed Default SMTP Account
        if (smtpAccountRepository.count() == 0) {
            SMTPAccount primaryGmail = new SMTPAccount();
            primaryGmail.setName("Primary Gmail");
            primaryGmail.setEmail("kreshiv02@gmail.com");
            primaryGmail.setHost("smtp.gmail.com");
            primaryGmail.setPort(587);
            primaryGmail.setUsername("kreshiv02@gmail.com");
            // Encrypt and save App Password
            primaryGmail.setPassword(encryptionService.encrypt("bqky otpp wsgv zayh"));
            primaryGmail.setTls(true);
            primaryGmail.setSsl(false);
            primaryGmail.setActive(true);
            smtpAccountRepository.save(primaryGmail);
        }
    }

    public User registerUser(String username, String password, String email, String role) {
        if (userRepository.findByUsername(username).isPresent()) {
            throw new IllegalArgumentException("Username already exists.");
        }
        if (userRepository.findByEmail(email).isPresent()) {
            throw new IllegalArgumentException("Email already exists.");
        }

        User user = new User();
        user.setUsername(username);
        user.setPassword(passwordEncoder.encode(password));
        user.setEmail(email);
        user.setRole(role);
        return userRepository.save(user);
    }

    public Optional<User> findByUsername(String username) {
        return userRepository.findByUsername(username);
    }

    public Optional<User> findByEmail(String email) {
        return userRepository.findByEmail(email);
    }

    public String generateResetToken(String email) {
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new IllegalArgumentException("User with email " + email + " not found."));

        String token = UUID.randomUUID().toString();
        user.setResetToken(token);
        user.setResetTokenExpiry(LocalDateTime.now().plusHours(1)); // 1 hour expiry
        userRepository.save(user);

        // In a real application, we would email this token. 
        // We will output it to console / system logs for easy retrieval.
        System.out.println("=================================================");
        System.out.println("PASSWORD RESET TOKEN FOR " + email + ": " + token);
        System.out.println("=================================================");

        return token;
    }

    public boolean resetPassword(String token, String newPassword) {
        User user = userRepository.findByResetToken(token)
                .orElseThrow(() -> new IllegalArgumentException("Invalid reset token."));

        if (user.getResetTokenExpiry().isBefore(LocalDateTime.now())) {
            user.setResetToken(null);
            user.setResetTokenExpiry(null);
            userRepository.save(user);
            return false; // Token expired
        }

        user.setPassword(passwordEncoder.encode(newPassword));
        user.setResetToken(null);
        user.setResetTokenExpiry(null);
        userRepository.save(user);
        return true;
    }

    public boolean changePassword(String username, String oldPassword, String newPassword) {
        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new IllegalArgumentException("User not found."));

        if (!passwordEncoder.matches(oldPassword, user.getPassword())) {
            return false;
        }

        user.setPassword(passwordEncoder.encode(newPassword));
        userRepository.save(user);
        return true;
    }
}
