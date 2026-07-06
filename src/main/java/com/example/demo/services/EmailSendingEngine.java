package com.example.demo.services;

import com.example.demo.entities.*;
import com.example.demo.repositories.CampaignRecipientRepository;
import com.example.demo.repositories.CampaignRepository;
import com.example.demo.repositories.EmailLogRepository;
import org.springframework.context.annotation.Lazy;
import org.springframework.mail.javamail.JavaMailSenderImpl;
import org.springframework.scheduling.annotation.Async;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Random;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Service
@EnableAsync
@EnableScheduling
public class EmailSendingEngine {

    private static final Logger log = LoggerFactory.getLogger(EmailSendingEngine.class);

    private final CampaignRepository campaignRepository;
    private final CampaignRecipientRepository campaignRecipientRepository;
    private final EmailLogRepository emailLogRepository;
    private final SMTPAccountService smtpAccountService;
    private final CampaignService campaignService;
    private final MailService mailService;
    private final TemplateProcessorService templateProcessorService;
    private final HtmlRendererService htmlRendererService;

    // In-memory concurrency locks to prevent double-sending on overlapping cron triggers
    private final Set<Long> activeCampaigns = ConcurrentHashMap.newKeySet();
    private final Random random = new Random();

    public EmailSendingEngine(CampaignRepository campaignRepository,
                              CampaignRecipientRepository campaignRecipientRepository,
                              EmailLogRepository emailLogRepository,
                              SMTPAccountService smtpAccountService,
                              @Lazy CampaignService campaignService,
                              MailService mailService,
                              TemplateProcessorService templateProcessorService,
                              HtmlRendererService htmlRendererService) {
        this.campaignRepository = campaignRepository;
        this.campaignRecipientRepository = campaignRecipientRepository;
        this.emailLogRepository = emailLogRepository;
        this.smtpAccountService = smtpAccountService;
        this.campaignService = campaignService;
        this.mailService = mailService;
        this.templateProcessorService = templateProcessorService;
        this.htmlRendererService = htmlRendererService;
    }

    /**
     * Periodic scheduled check for pending/scheduled campaigns.
     * Runs every 10 seconds.
     */
    @Scheduled(fixedDelay = 10000)
    public void checkScheduledCampaigns() {
        List<Campaign> scheduled = campaignRepository.findByStatus("SCHEDULED");
        LocalDateTime now = LocalDateTime.now();
        for (Campaign campaign : scheduled) {
            if (campaign.getScheduleTime() == null || campaign.getScheduleTime().isBefore(now) || campaign.getScheduleTime().isEqual(now)) {
                log.info("Scheduled campaign '{}' is ready to start.", campaign.getCampaignName());
                campaignService.startCampaign(campaign.getId());
            }
        }

        List<Campaign> sending = campaignRepository.findByStatus("SENDING");
        for (Campaign campaign : sending) {
            runCampaignAsync(campaign.getId());
        }
    }

    /**
     * Executes campaign sending asynchronously in a background worker thread.
     */
    @Async
    public void runCampaignAsync(Long campaignId) {
        // Prevent concurrent thread runs for the same campaign
        if (!activeCampaigns.add(campaignId)) {
            return;
        }

        try {
            Campaign campaign = campaignRepository.findById(campaignId).orElse(null);
            if (campaign == null || !"SENDING".equals(campaign.getStatus())) {
                return;
            }

            SMTPAccount smtp = campaign.getSmtpAccount();
            if (smtp == null) {
                logCampaignFailure(campaign, "SMTP Account is missing.");
                return;
            }

            // Initialize JavaMailSender dynamically via MailService
            JavaMailSenderImpl mailSender;
            try {
                mailSender = mailService.configureMailSender(smtp);
            } catch (Exception e) {
                log.error("Failed to configure SMTP mail sender dynamically. Reason: " + e.getMessage(), e);
                logCampaignFailure(campaign, "Failed to configure SMTP: " + e.getMessage());
                return;
            }

            List<CampaignRecipient> pendingRecipients = campaignRecipientRepository.findByCampaignIdAndStatus(campaignId, "PENDING");
            log.info("Campaign '{}' is processing. Pending recipients: {}", campaign.getCampaignName(), pendingRecipients.size());

            for (CampaignRecipient recipient : pendingRecipients) {
                // Recheck campaign status to handle runtime pause/stop requests
                Campaign currentCampaign = campaignRepository.findById(campaignId).orElse(null);
                if (currentCampaign == null || !"SENDING".equals(currentCampaign.getStatus())) {
                    log.info("Campaign '{}' has been paused or deleted. Stopping email loop.", campaign.getCampaignName());
                    break;
                }

                Company company = recipient.getCompany();
                String senderName = smtp.getName();

                // 1. Process placeholders for both subject and body (TemplateProcessor Layer)
                String processedSubject = templateProcessorService.process(campaign.getTemplate().getSubject(), company, senderName);
                String processedBody = templateProcessorService.process(campaign.getTemplate().getBody(), company, senderName);

                // 2. Render formatting to clean, secure HTML (HTML Renderer Layer - Markdown + XSS Check)
                String finalBodyHtml = htmlRendererService.render(processedBody);

                try {
                    // Send Email (MailService Layer)
                    mailService.sendEmail(
                            mailSender,
                            smtp.getEmail(),
                            company.getEmail(),
                            processedSubject,
                            finalBodyHtml,
                            campaign.getTemplate().getAttachmentName(),
                            campaign.getTemplate().getAttachmentData()
                    );

                    recipient.setStatus("SENT");
                    recipient.setSentTimestamp(LocalDateTime.now());
                    recipient.setErrorMessage(null);
                    campaignRecipientRepository.save(recipient);

                    EmailLog logRecord = new EmailLog(company.getEmail(), processedSubject, LocalDateTime.now(), "SENT", null, campaign, smtp);
                    emailLogRepository.save(logRecord);

                } catch (Exception e) {
                    log.error("SMTP sending failure to <" + company.getEmail() + ">. Complete Exception stacktrace:", e);

                    int retries = recipient.getRetryCount() + 1;
                    recipient.setRetryCount(retries);

                    String errorMsg = e.getMessage() != null ? e.getMessage() : e.getClass().getSimpleName();

                    if (retries >= 3) {
                        recipient.setStatus("FAILED");
                        recipient.setErrorMessage("Max retries exceeded. Error: " + errorMsg);
                        campaignRecipientRepository.save(recipient);

                        EmailLog logRecord = new EmailLog(company.getEmail(), processedSubject, LocalDateTime.now(), "FAILED", "Max retries exceeded. Error: " + errorMsg, campaign, smtp);
                        emailLogRepository.save(logRecord);
                    } else {
                        recipient.setErrorMessage("Attempt " + retries + " failed: " + errorMsg);
                        campaignRecipientRepository.save(recipient);

                        EmailLog logRecord = new EmailLog(company.getEmail(), processedSubject, LocalDateTime.now(), "FAILED", "Attempt " + retries + " failed: " + errorMsg, campaign, smtp);
                        emailLogRepository.save(logRecord);
                    }
                }

                // Anti-spam Delay logic
                int minDelay = campaign.getMinDelaySeconds();
                int maxDelay = campaign.getMaxDelaySeconds();
                int delay = minDelay;
                if (maxDelay > minDelay) {
                    delay = minDelay + random.nextInt(maxDelay - minDelay + 1);
                }

                log.info("Sleeping for {} seconds to avoid anti-spam triggers...", delay);
                try {
                    Thread.sleep(delay * 1000L);
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    log.warn("Campaign email loop interrupted.");
                    break;
                }
            }

            // Post-campaign state evaluation
            Campaign finalCampaignState = campaignRepository.findById(campaignId).orElse(null);
            if (finalCampaignState != null && "SENDING".equals(finalCampaignState.getStatus())) {
                long remainingPending = campaignRecipientRepository.countByCampaignIdAndStatus(campaignId, "PENDING");
                if (remainingPending == 0) {
                    finalCampaignState.setStatus("COMPLETED");
                    campaignRepository.save(finalCampaignState);
                    log.info("Campaign '{}' has finished sending to all recipients.", campaign.getCampaignName());
                }
            }

        } finally {
            activeCampaigns.remove(campaignId);
        }
    }

    private void logCampaignFailure(Campaign campaign, String error) {
        campaign.setStatus("FAILED");
        campaignRepository.save(campaign);

        EmailLog logRecord = new EmailLog("SYSTEM", "Campaign " + campaign.getCampaignName() + " Failed", LocalDateTime.now(), "FAILED", error, campaign, campaign.getSmtpAccount());
        emailLogRepository.save(logRecord);
    }
}
