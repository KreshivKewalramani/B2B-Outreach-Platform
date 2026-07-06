package com.example.demo.controllers;

import com.example.demo.dto.CompanyImportResult;
import com.example.demo.entities.Company;
import com.example.demo.services.CompanyService;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.util.List;

@Controller
@RequestMapping("/companies")
public class CompanyController {

    private final CompanyService companyService;

    public CompanyController(CompanyService companyService) {
        this.companyService = companyService;
    }

    @GetMapping
    public String listCompanies(@RequestParam(required = false) String search,
                                @RequestParam(required = false) String industry,
                                Model model) {
        List<Company> companies = companyService.searchAndFilter(search, industry);
        List<String> industries = companyService.getDistinctIndustries();

        model.addAttribute("companies", companies);
        model.addAttribute("industries", industries);
        model.addAttribute("currentSearch", search);
        model.addAttribute("currentIndustry", industry);

        return "companies/list";
    }

    @GetMapping("/new")
    public String showCreateForm(Model model) {
        model.addAttribute("company", new Company());
        return "companies/form";
    }

    @PostMapping("/save")
    public String saveCompany(@ModelAttribute Company company, RedirectAttributes redirectAttributes) {
        try {
            companyService.saveCompany(company);
            redirectAttributes.addFlashAttribute("success", "Company saved successfully.");
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("error", "Error saving company: " + e.getMessage());
        }
        return "redirect:/companies";
    }

    @GetMapping("/edit/{id}")
    public String showEditForm(@PathVariable Long id, Model model, RedirectAttributes redirectAttributes) {
        return companyService.getCompanyById(id)
                .map(company -> {
                    model.addAttribute("company", company);
                    return "companies/form";
                })
                .orElseGet(() -> {
                    redirectAttributes.addFlashAttribute("error", "Company not found.");
                    return "redirect:/companies";
                });
    }

    @GetMapping("/delete/{id}")
    public String deleteCompany(@PathVariable Long id, RedirectAttributes redirectAttributes) {
        try {
            companyService.deleteCompany(id);
            redirectAttributes.addFlashAttribute("success", "Company deleted successfully.");
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("error", "Error deleting company: " + e.getMessage());
        }
        return "redirect:/companies";
    }

    @PostMapping("/bulk-delete")
    public String bulkDelete(@RequestParam(value = "ids", required = false) List<Long> ids, RedirectAttributes redirectAttributes) {
        if (ids == null || ids.isEmpty()) {
            redirectAttributes.addFlashAttribute("error", "No companies selected for deletion.");
            return "redirect:/companies";
        }
        try {
            companyService.bulkDelete(ids);
            redirectAttributes.addFlashAttribute("success", "Selected companies deleted successfully.");
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("error", "Error performing bulk delete: " + e.getMessage());
        }
        return "redirect:/companies";
    }

    @GetMapping("/import")
    public String showImportForm() {
        return "companies/import";
    }

    @PostMapping("/import")
    public String importExcel(@RequestParam("file") MultipartFile file, RedirectAttributes redirectAttributes) {
        if (file.isEmpty()) {
            redirectAttributes.addFlashAttribute("error", "Please select a file to import.");
            return "redirect:/companies/import";
        }

        String filename = file.getOriginalFilename();
        if (filename == null || (!filename.endsWith(".xlsx") && !filename.endsWith(".xls"))) {
            redirectAttributes.addFlashAttribute("error", "Unsupported format. Only .xlsx and .xls are supported.");
            return "redirect:/companies/import";
        }

        try {
            CompanyImportResult result = companyService.importFromExcel(file);
            redirectAttributes.addFlashAttribute("importResult", result);
            redirectAttributes.addFlashAttribute("success", "Excel file processed.");
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("error", "Error processing Excel file: " + e.getMessage());
        }

        return "redirect:/companies/import";
    }
}
