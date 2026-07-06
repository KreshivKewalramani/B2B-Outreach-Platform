package com.example.demo.controllers;

import com.example.demo.entities.Campaign;
import com.example.demo.entities.SMTPAccount;
import com.example.demo.repositories.CampaignRepository;
import com.example.demo.repositories.CompanyRepository;
import com.example.demo.repositories.EmailLogRepository;
import com.example.demo.services.SMTPAccountService;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;

import java.util.List;

@Controller
public class DashboardController {

    private final CompanyRepository companyRepository;
    private final CampaignRepository campaignRepository;
    private final EmailLogRepository emailLogRepository;
    private final SMTPAccountService smtpAccountService;

    public DashboardController(CompanyRepository companyRepository,
                               CampaignRepository campaignRepository,
                               EmailLogRepository emailLogRepository,
                               SMTPAccountService smtpAccountService) {
        this.companyRepository = companyRepository;
        this.campaignRepository = campaignRepository;
        this.emailLogRepository = emailLogRepository;
        this.smtpAccountService = smtpAccountService;
    }

    @GetMapping("/")
    public String dashboard(Model model) {
        long totalLeads = companyRepository.count();
        long totalCampaigns = campaignRepository.count();
        
        long sentCount = emailLogRepository.countByStatus("SENT");
        long failedCount = emailLogRepository.countByStatus("FAILED");
        
        double successRate = 0.0;
        if (sentCount + failedCount > 0) {
            successRate = ((double) sentCount / (sentCount + failedCount)) * 100.0;
        }

        List<SMTPAccount> activeSmtp = smtpAccountService.getActiveAccounts();
        String activeSmtpName = activeSmtp.isEmpty() ? "None Active" : activeSmtp.get(0).getName() + " (" + activeSmtp.get(0).getEmail() + ")";

        model.addAttribute("totalLeads", totalLeads);
        model.addAttribute("totalCampaigns", totalCampaigns);
        model.addAttribute("sentCount", sentCount);
        model.addAttribute("failedCount", failedCount);
        model.addAttribute("successRate", String.format("%.1f", successRate));
        model.addAttribute("activeSmtp", activeSmtpName);

        model.addAttribute("recentLogs", emailLogRepository.findTop10ByOrderByTimestampDesc());

        List<Campaign> campaigns = campaignRepository.findAll();
        model.addAttribute("campaigns", campaigns);

        return "dashboard";
    }
}
