package com.example.demo.controllers;

import com.example.demo.entities.Campaign;
import com.example.demo.entities.CampaignRecipient;
import com.example.demo.repositories.CampaignRecipientRepository;
import com.example.demo.repositories.EmailTemplateRepository;
import com.example.demo.repositories.SMTPAccountRepository;
import com.example.demo.services.CampaignService;
import com.example.demo.services.CompanyService;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.util.List;

@Controller
@RequestMapping("/campaigns")
public class CampaignController {

    private final CampaignService campaignService;
    private final EmailTemplateRepository emailTemplateRepository;
    private final SMTPAccountRepository smtpAccountRepository;
    private final CompanyService companyService;
    private final CampaignRecipientRepository campaignRecipientRepository;

    public CampaignController(CampaignService campaignService,
                              EmailTemplateRepository emailTemplateRepository,
                              SMTPAccountRepository smtpAccountRepository,
                              CompanyService companyService,
                              CampaignRecipientRepository campaignRecipientRepository) {
        this.campaignService = campaignService;
        this.emailTemplateRepository = emailTemplateRepository;
        this.smtpAccountRepository = smtpAccountRepository;
        this.companyService = companyService;
        this.campaignRecipientRepository = campaignRecipientRepository;
    }

    @GetMapping
    public String listCampaigns(Model model) {
        model.addAttribute("campaigns", campaignService.getAllCampaigns());
        return "campaigns/list";
    }

    @GetMapping("/new")
    public String showCreateForm(Model model) {
        model.addAttribute("campaign", new Campaign());
        model.addAttribute("templates", emailTemplateRepository.findAll());
        model.addAttribute("smtpAccounts", smtpAccountRepository.findAll());
        model.addAttribute("industries", companyService.getDistinctIndustries());
        return "campaigns/form";
    }

    @PostMapping("/save")
    public String saveCampaign(@ModelAttribute Campaign campaign, RedirectAttributes redirectAttributes) {
        try {
            campaignService.saveCampaign(campaign);
            redirectAttributes.addFlashAttribute("success", "Campaign saved successfully.");
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("error", "Error saving campaign: " + e.getMessage());
        }
        return "redirect:/campaigns";
    }

    @GetMapping("/edit/{id}")
    public String showEditForm(@PathVariable Long id, Model model, RedirectAttributes redirectAttributes) {
        return campaignService.getCampaignById(id)
                .map(campaign -> {
                    model.addAttribute("campaign", campaign);
                    model.addAttribute("templates", emailTemplateRepository.findAll());
                    model.addAttribute("smtpAccounts", smtpAccountRepository.findAll());
                    model.addAttribute("industries", companyService.getDistinctIndustries());
                    return "campaigns/form";
                })
                .orElseGet(() -> {
                    redirectAttributes.addFlashAttribute("error", "Campaign not found.");
                    return "redirect:/campaigns";
                });
    }

    @GetMapping("/delete/{id}")
    public String deleteCampaign(@PathVariable Long id, RedirectAttributes redirectAttributes) {
        try {
            campaignService.deleteCampaign(id);
            redirectAttributes.addFlashAttribute("success", "Campaign deleted successfully.");
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("error", "Error deleting campaign: " + e.getMessage());
        }
        return "redirect:/campaigns";
    }

    @GetMapping("/clone/{id}")
    public String cloneCampaign(@PathVariable Long id, RedirectAttributes redirectAttributes) {
        try {
            campaignService.cloneCampaign(id);
            redirectAttributes.addFlashAttribute("success", "Campaign cloned successfully.");
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("error", "Error cloning campaign: " + e.getMessage());
        }
        return "redirect:/campaigns";
    }

    @GetMapping("/start/{id}")
    public String startCampaign(@PathVariable Long id, RedirectAttributes redirectAttributes) {
        try {
            campaignService.startCampaign(id);
            redirectAttributes.addFlashAttribute("success", "Campaign started / scheduled successfully.");
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("error", "Error starting campaign: " + e.getMessage());
        }
        return "redirect:/campaigns";
    }

    @GetMapping("/pause/{id}")
    public String pauseCampaign(@PathVariable Long id, RedirectAttributes redirectAttributes) {
        try {
            campaignService.pauseCampaign(id);
            redirectAttributes.addFlashAttribute("success", "Campaign paused.");
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("error", "Error pausing campaign: " + e.getMessage());
        }
        return "redirect:/campaigns";
    }

    @GetMapping("/resume/{id}")
    public String resumeCampaign(@PathVariable Long id, RedirectAttributes redirectAttributes) {
        try {
            campaignService.resumeCampaign(id);
            redirectAttributes.addFlashAttribute("success", "Campaign resumed.");
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("error", "Error resuming campaign: " + e.getMessage());
        }
        return "redirect:/campaigns";
    }

    @GetMapping("/view/{id}")
    public String viewCampaignProgress(@PathVariable Long id, Model model, RedirectAttributes redirectAttributes) {
        return campaignService.getCampaignById(id)
                .map(campaign -> {
                    List<CampaignRecipient> recipients = campaignRecipientRepository.findByCampaignId(id);
                    long sent = campaignRecipientRepository.countByCampaignIdAndStatus(id, "SENT");
                    long failed = campaignRecipientRepository.countByCampaignIdAndStatus(id, "FAILED");
                    long pending = campaignRecipientRepository.countByCampaignIdAndStatus(id, "PENDING");
                    long total = campaignRecipientRepository.countByCampaignId(id);

                    model.addAttribute("campaign", campaign);
                    model.addAttribute("recipients", recipients);
                    model.addAttribute("sentCount", sent);
                    model.addAttribute("failedCount", failed);
                    model.addAttribute("pendingCount", pending);
                    model.addAttribute("totalCount", total);

                    double progress = 0;
                    if (total > 0) {
                        progress = ((double)(sent + failed) / total) * 100.0;
                    }
                    model.addAttribute("progressPercent", String.format("%.1f", progress));

                    return "campaigns/view";
                })
                .orElseGet(() -> {
                    redirectAttributes.addFlashAttribute("error", "Campaign not found.");
                    return "redirect:/campaigns";
                });
    }
}
