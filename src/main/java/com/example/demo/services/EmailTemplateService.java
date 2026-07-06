package com.example.demo.services;

import com.example.demo.entities.EmailTemplate;
import com.example.demo.repositories.EmailTemplateRepository;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;

@Service
public class EmailTemplateService {

    private final EmailTemplateRepository emailTemplateRepository;

    public EmailTemplateService(EmailTemplateRepository emailTemplateRepository) {
        this.emailTemplateRepository = emailTemplateRepository;
    }

    public List<EmailTemplate> getAllTemplates() {
        return emailTemplateRepository.findAll();
    }

    public Optional<EmailTemplate> getTemplateById(Long id) {
        return emailTemplateRepository.findById(id);
    }

    public EmailTemplate saveTemplate(EmailTemplate template) {
        return emailTemplateRepository.save(template);
    }

    public void deleteTemplate(Long id) {
        emailTemplateRepository.deleteById(id);
    }

    public EmailTemplate duplicateTemplate(Long id) {
        EmailTemplate original = emailTemplateRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Template not found: " + id));
        EmailTemplate duplicate = new EmailTemplate();
        duplicate.setTemplateName("Copy of " + original.getTemplateName());
        duplicate.setSubject(original.getSubject());
        duplicate.setBody(original.getBody());
        duplicate.setAttachmentName(original.getAttachmentName());
        duplicate.setAttachmentData(original.getAttachmentData());
        return emailTemplateRepository.save(duplicate);
    }
}
