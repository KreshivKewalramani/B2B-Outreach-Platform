package com.example.demo.controllers;

import com.example.demo.entities.Company;
import com.example.demo.entities.EmailTemplate;
import com.example.demo.repositories.CompanyRepository;
import com.example.demo.services.EmailTemplateService;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.http.ResponseEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;

import java.util.List;

@Controller
@RequestMapping("/templates")
public class TemplateController {

    private final EmailTemplateService emailTemplateService;
    private final CompanyRepository companyRepository;

    public TemplateController(EmailTemplateService emailTemplateService, CompanyRepository companyRepository) {
        this.emailTemplateService = emailTemplateService;
        this.companyRepository = companyRepository;
    }

    @GetMapping
    public String listTemplates(Model model) {
        model.addAttribute("templates", emailTemplateService.getAllTemplates());
        return "templates/list";
    }

    @GetMapping("/new")
    public String showCreateForm(Model model) {
        model.addAttribute("template", new EmailTemplate());
        return "templates/form";
    }

    @PostMapping("/save")
    public String saveTemplate(@ModelAttribute EmailTemplate template,
                               @RequestParam(value = "attachmentFile", required = false) MultipartFile attachmentFile,
                               @RequestParam(value = "clearAttachment", required = false) Boolean clearAttachment,
                               RedirectAttributes redirectAttributes) {
        try {
            if (template.getId() != null) {
                EmailTemplate existing = emailTemplateService.getTemplateById(template.getId()).orElse(null);
                if (existing != null) {
                    if (clearAttachment != null && clearAttachment) {
                        template.setAttachmentName(null);
                        template.setAttachmentData(null);
                    } else if (attachmentFile != null && !attachmentFile.isEmpty()) {
                        template.setAttachmentName(attachmentFile.getOriginalFilename());
                        template.setAttachmentData(attachmentFile.getBytes());
                    } else {
                        template.setAttachmentName(existing.getAttachmentName());
                        template.setAttachmentData(existing.getAttachmentData());
                    }
                }
            } else {
                if (attachmentFile != null && !attachmentFile.isEmpty()) {
                    template.setAttachmentName(attachmentFile.getOriginalFilename());
                    template.setAttachmentData(attachmentFile.getBytes());
                }
            }
            emailTemplateService.saveTemplate(template);
            redirectAttributes.addFlashAttribute("success", "Template saved successfully.");
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("error", "Error saving template: " + e.getMessage());
        }
        return "redirect:/templates";
    }

    @GetMapping("/download-attachment/{id}")
    @ResponseBody
    public ResponseEntity<byte[]> downloadAttachment(@PathVariable Long id) {
        return emailTemplateService.getTemplateById(id)
                .filter(t -> t.getAttachmentName() != null && t.getAttachmentData() != null)
                .map(t -> ResponseEntity.ok()
                        .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + t.getAttachmentName() + "\"")
                        .contentType(MediaType.APPLICATION_OCTET_STREAM)
                        .body(t.getAttachmentData()))
                .orElseGet(() -> ResponseEntity.notFound().build());
    }

    @GetMapping("/edit/{id}")
    public String showEditForm(@PathVariable Long id, Model model, RedirectAttributes redirectAttributes) {
        return emailTemplateService.getTemplateById(id)
                .map(template -> {
                    model.addAttribute("template", template);
                    return "templates/form";
                })
                .orElseGet(() -> {
                    redirectAttributes.addFlashAttribute("error", "Template not found.");
                    return "redirect:/templates";
                });
    }

    @GetMapping("/delete/{id}")
    public String deleteTemplate(@PathVariable Long id, RedirectAttributes redirectAttributes) {
        try {
            emailTemplateService.deleteTemplate(id);
            redirectAttributes.addFlashAttribute("success", "Template deleted successfully.");
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("error", "Error deleting template: " + e.getMessage());
        }
        return "redirect:/templates";
    }

    @GetMapping("/duplicate/{id}")
    public String duplicateTemplate(@PathVariable Long id, RedirectAttributes redirectAttributes) {
        try {
            emailTemplateService.duplicateTemplate(id);
            redirectAttributes.addFlashAttribute("success", "Template duplicated successfully.");
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("error", "Error duplicating template: " + e.getMessage());
        }
        return "redirect:/templates";
    }

    @GetMapping("/preview/{id}")
    public String previewTemplate(@PathVariable Long id, Model model, RedirectAttributes redirectAttributes) {
        return emailTemplateService.getTemplateById(id)
                .map(template -> {
                    List<Company> companies = companyRepository.findAll();
                    Company previewCompany;
                    if (!companies.isEmpty()) {
                        previewCompany = companies.get(0);
                    } else {
                        previewCompany = new Company();
                        previewCompany.setCompanyName("Acme Corp");
                        previewCompany.setContactPerson("John Doe");
                        previewCompany.setEmail("johndoe@acme.com");
                        previewCompany.setDesignation("Director of Operations");
                        previewCompany.setIndustry("Technology");
                    }

                    String previewSubject = template.getSubject()
                            .replace("{{company_name}}", previewCompany.getCompanyName())
                            .replace("{{contact_person}}", previewCompany.getContactPerson())
                            .replace("{{email}}", previewCompany.getEmail())
                            .replace("{{designation}}", previewCompany.getDesignation())
                            .replace("{{industry}}", previewCompany.getIndustry());

                    String previewBody = template.getBody()
                            .replace("{{company_name}}", previewCompany.getCompanyName())
                            .replace("{{contact_person}}", previewCompany.getContactPerson())
                            .replace("{{email}}", previewCompany.getEmail())
                            .replace("{{designation}}", previewCompany.getDesignation())
                            .replace("{{industry}}", previewCompany.getIndustry());

                    model.addAttribute("template", template);
                    model.addAttribute("previewSubject", previewSubject);
                    model.addAttribute("previewBody", previewBody);
                    model.addAttribute("company", previewCompany);
                    return "templates/preview";
                })
                .orElseGet(() -> {
                    redirectAttributes.addFlashAttribute("error", "Template not found.");
                    return "redirect:/templates";
                });
    }
}
