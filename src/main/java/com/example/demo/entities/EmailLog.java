package com.example.demo.entities;

import jakarta.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "email_logs")
public class EmailLog {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String recipient;

    @Column(nullable = false)
    private String subject;

    @Column(nullable = false)
    private LocalDateTime timestamp;

    @Column(nullable = false)
    private String status; // SENT, FAILED, QUEUED, SCHEDULED

    @Column(name = "error_message", columnDefinition = "TEXT")
    private String errorMessage;

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "campaign_id")
    private Campaign campaign;

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "smtp_account_id")
    private SMTPAccount smtpAccount;

    public EmailLog() {}

    public EmailLog(String recipient, String subject, LocalDateTime timestamp, String status, String errorMessage, Campaign campaign, SMTPAccount smtpAccount) {
        this.recipient = recipient;
        this.subject = subject;
        this.timestamp = timestamp;
        this.status = status;
        this.errorMessage = errorMessage;
        this.campaign = campaign;
        this.smtpAccount = smtpAccount;
    }

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public String getRecipient() { return recipient; }
    public void setRecipient(String recipient) { this.recipient = recipient; }

    public String getSubject() { return subject; }
    public void setSubject(String subject) { this.subject = subject; }

    public LocalDateTime getTimestamp() { return timestamp; }
    public void setTimestamp(LocalDateTime timestamp) { this.timestamp = timestamp; }

    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }

    public String getErrorMessage() { return errorMessage; }
    public void setErrorMessage(String errorMessage) { this.errorMessage = errorMessage; }

    public Campaign getCampaign() { return campaign; }
    public void setCampaign(Campaign campaign) { this.campaign = campaign; }

    public SMTPAccount getSmtpAccount() { return smtpAccount; }
    public void setSmtpAccount(SMTPAccount smtpAccount) { this.smtpAccount = smtpAccount; }
}
