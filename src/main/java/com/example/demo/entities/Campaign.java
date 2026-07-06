package com.example.demo.entities;

import jakarta.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "campaigns")
public class Campaign {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "campaign_name", nullable = false)
    private String campaignName;

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "template_id", nullable = false)
    private EmailTemplate template;

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "smtp_account_id", nullable = false)
    private SMTPAccount smtpAccount;

    @Column(name = "schedule_time")
    private LocalDateTime scheduleTime;

    @Column(nullable = false)
    private String status = "DRAFT"; // DRAFT, SCHEDULED, SENDING, PAUSED, COMPLETED

    @Column(name = "min_delay_seconds")
    private int minDelaySeconds = 15;

    @Column(name = "max_delay_seconds")
    private int maxDelaySeconds = 45;

    @Column(name = "target_type", nullable = false)
    private String targetType = "ALL"; // ALL, INDUSTRY

    @Column(name = "target_value")
    private String targetValue;

    @Column(name = "created_date", nullable = false)
    private LocalDateTime createdDate;

    public Campaign() {}

    @PrePersist
    protected void onCreate() {
        if (createdDate == null) {
            createdDate = LocalDateTime.now();
        }
    }

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public String getCampaignName() { return campaignName; }
    public void setCampaignName(String campaignName) { this.campaignName = campaignName; }

    public EmailTemplate getTemplate() { return template; }
    public void setTemplate(EmailTemplate template) { this.template = template; }

    public SMTPAccount getSmtpAccount() { return smtpAccount; }
    public void setSmtpAccount(SMTPAccount smtpAccount) { this.smtpAccount = smtpAccount; }

    public LocalDateTime getScheduleTime() { return scheduleTime; }
    public void setScheduleTime(LocalDateTime scheduleTime) { this.scheduleTime = scheduleTime; }

    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }

    public int getMinDelaySeconds() { return minDelaySeconds; }
    public void setMinDelaySeconds(int minDelaySeconds) { this.minDelaySeconds = minDelaySeconds; }

    public int getMaxDelaySeconds() { return maxDelaySeconds; }
    public void setMaxDelaySeconds(int maxDelaySeconds) { this.maxDelaySeconds = maxDelaySeconds; }

    public String getTargetType() { return targetType; }
    public void setTargetType(String targetType) { this.targetType = targetType; }

    public String getTargetValue() { return targetValue; }
    public void setTargetValue(String targetValue) { this.targetValue = targetValue; }

    public LocalDateTime getCreatedDate() { return createdDate; }
    public void setCreatedDate(LocalDateTime createdDate) { this.createdDate = createdDate; }
}
