package com.example.demo.services;

import com.example.demo.entities.Campaign;
import com.example.demo.entities.CampaignRecipient;
import com.example.demo.entities.Company;
import com.example.demo.repositories.CampaignRecipientRepository;
import com.example.demo.repositories.CampaignRepository;
import com.example.demo.repositories.CompanyRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Service
public class CampaignService {

    private final CampaignRepository campaignRepository;
    private final CampaignRecipientRepository campaignRecipientRepository;
    private final CompanyRepository companyRepository;
    private final EmailSendingEngine emailSendingEngine;

    // Use constructor injection. Lazy loading the engine to avoid circular reference if any.
    public CampaignService(CampaignRepository campaignRepository,
                           CampaignRecipientRepository campaignRecipientRepository,
                           CompanyRepository companyRepository,
                           @Lazy EmailSendingEngine emailSendingEngine) {
        this.campaignRepository = campaignRepository;
        this.campaignRecipientRepository = campaignRecipientRepository;
        this.companyRepository = companyRepository;
        this.emailSendingEngine = emailSendingEngine;
    }

    public List<Campaign> getAllCampaigns() {
        return campaignRepository.findAll();
    }

    public Optional<Campaign> getCampaignById(Long id) {
        return campaignRepository.findById(id);
    }

    public Campaign saveCampaign(Campaign campaign) {
        return campaignRepository.save(campaign);
    }

    @Transactional
    public void deleteCampaign(Long id) {
        campaignRecipientRepository.deleteByCampaignId(id);
        campaignRepository.deleteById(id);
    }

    @Transactional
    public Campaign cloneCampaign(Long id) {
        Campaign original = campaignRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Campaign not found: " + id));

        Campaign clone = new Campaign();
        clone.setCampaignName("Copy of " + original.getCampaignName());
        clone.setTemplate(original.getTemplate());
        clone.setSmtpAccount(original.getSmtpAccount());
        clone.setMinDelaySeconds(original.getMinDelaySeconds());
        clone.setMaxDelaySeconds(original.getMaxDelaySeconds());
        clone.setTargetType(original.getTargetType());
        clone.setTargetValue(original.getTargetValue());
        clone.setStatus("DRAFT");
        clone.setCreatedDate(LocalDateTime.now());

        return campaignRepository.save(clone);
    }

    @Transactional
    public void startCampaign(Long id) {
        Campaign campaign = campaignRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Campaign not found: " + id));

        // Validation checks (Issue 8)
        if (campaign.getSmtpAccount() == null) {
            throw new IllegalStateException("SMTP settings are not configured for this campaign.");
        }
        if (!campaign.getSmtpAccount().isActive()) {
            throw new IllegalStateException("The SMTP Account selected is currently inactive. Please activate it in settings.");
        }
        if (campaign.getTemplate() == null) {
            throw new IllegalStateException("Email template is not configured for this campaign.");
        }
        if (campaign.getTemplate().getSubject() == null || campaign.getTemplate().getSubject().trim().isEmpty()) {
            throw new IllegalStateException("Email template subject cannot be empty.");
        }
        if (campaign.getTemplate().getBody() == null || campaign.getTemplate().getBody().trim().isEmpty()) {
            throw new IllegalStateException("Email template body cannot be empty.");
        }

        // Check if there are recipients
        List<Company> targets;
        if ("INDUSTRY".equalsIgnoreCase(campaign.getTargetType()) && campaign.getTargetValue() != null) {
            targets = companyRepository.searchAndFilter(null, campaign.getTargetValue());
        } else {
            targets = companyRepository.findAll();
        }

        if (targets.isEmpty()) {
            throw new IllegalStateException("No target recipients (companies) exist in the system for this campaign's target criteria.");
        }

        // Validate recipient emails
        long validEmailCount = targets.stream()
                .filter(company -> company.getEmail() != null && isValidEmail(company.getEmail()))
                .count();

        if (validEmailCount == 0) {
            throw new IllegalStateException("No recipients with valid email formats exist in the targeted list.");
        }

        // If the schedule time is in the future, set status to SCHEDULED
        if (campaign.getScheduleTime() != null && campaign.getScheduleTime().isAfter(LocalDateTime.now())) {
            campaign.setStatus("SCHEDULED");
            campaignRepository.save(campaign);
            return;
        }

        campaign.setStatus("SENDING");
        campaignRepository.save(campaign);

        // Populate recipients if empty
        long recipientCount = campaignRecipientRepository.countByCampaignId(campaign.getId());
        if (recipientCount == 0) {
            for (Company company : targets) {
                if (company.getEmail() != null && isValidEmail(company.getEmail())) {
                    CampaignRecipient cr = new CampaignRecipient(campaign, company);
                    campaignRecipientRepository.save(cr);
                }
            }
        }

        // Trigger engine
        emailSendingEngine.runCampaignAsync(campaign.getId());
    }

    private boolean isValidEmail(String email) {
        if (email == null) return false;
        return email.trim().matches("^[a-zA-Z0-9_+&*-]+(?:\\.[a-zA-Z0-9_+&*-]+)*@(?:[a-zA-Z0-9-]+\\.)+[a-zA-Z]{2,7}$");
    }

    public void pauseCampaign(Long id) {
        Campaign campaign = campaignRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Campaign not found: " + id));
        campaign.setStatus("PAUSED");
        campaignRepository.save(campaign);
    }

    @Transactional
    public void resumeCampaign(Long id) {
        Campaign campaign = campaignRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Campaign not found: " + id));
        campaign.setStatus("SENDING");
        campaignRepository.save(campaign);

        // Trigger engine to run
        emailSendingEngine.runCampaignAsync(campaign.getId());
    }
}
