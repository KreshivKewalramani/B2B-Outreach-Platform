package com.example.demo.repositories;

import com.example.demo.entities.CampaignRecipient;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

public interface CampaignRecipientRepository extends JpaRepository<CampaignRecipient, Long> {
    List<CampaignRecipient> findByCampaignId(Long campaignId);
    List<CampaignRecipient> findByCampaignIdAndStatus(Long campaignId, String status);
    long countByCampaignIdAndStatus(Long campaignId, String status);
    long countByCampaignId(Long campaignId);
    void deleteByCampaignId(Long campaignId);
}
