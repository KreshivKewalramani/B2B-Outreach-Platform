package com.example.demo.services;

import com.example.demo.entities.Company;
import org.springframework.stereotype.Service;

@Service
public class TemplateProcessorService {

    public String process(String template, Company company, String senderName) {
        if (template == null) return "";
        String result = template;
        
        // Standard Placeholders
        result = result.replace("{{company_name}}", getOrEmpty(company.getCompanyName()));
        result = result.replace("{{contact_person}}", getOrEmpty(company.getContactPerson()));
        result = result.replace("{{email}}", getOrEmpty(company.getEmail()));
        result = result.replace("{{designation}}", getOrEmpty(company.getDesignation()));
        result = result.replace("{{industry}}", getOrEmpty(company.getIndustry()));
        
        // Additional requested placeholders & aliases (Issue 4)
        result = result.replace("{{name}}", getOrEmpty(company.getContactPerson()));
        result = result.replace("{{team}}", getOrEmpty(company.getCompanyName()));
        result = result.replace("{{sender}}", senderName != null ? senderName : "Outreach Team");
        
        return result;
    }

    private String getOrEmpty(String val) {
        return val != null ? val.trim() : "";
    }
}
