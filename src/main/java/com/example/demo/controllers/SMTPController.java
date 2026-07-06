package com.example.demo.controllers;

import com.example.demo.entities.SMTPAccount;
import com.example.demo.services.SMTPAccountService;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.util.HashMap;
import java.util.Map;

@Controller
@RequestMapping("/smtp")
public class SMTPController {

    private final SMTPAccountService smtpAccountService;

    public SMTPController(SMTPAccountService smtpAccountService) {
        this.smtpAccountService = smtpAccountService;
    }

    @GetMapping
    public String listAccounts(Model model) {
        model.addAttribute("accounts", smtpAccountService.getAllAccounts());
        return "smtp/list";
    }

    @GetMapping("/new")
    public String showCreateForm(Model model) {
        model.addAttribute("account", new SMTPAccount());
        return "smtp/form";
    }

    @PostMapping("/save")
    public String saveAccount(@ModelAttribute SMTPAccount account, RedirectAttributes redirectAttributes) {
        try {
            smtpAccountService.saveAccount(account);
            redirectAttributes.addFlashAttribute("success", "SMTP Account saved successfully.");
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("error", "Error saving SMTP Account: " + e.getMessage());
        }
        return "redirect:/smtp";
    }

    @GetMapping("/edit/{id}")
    public String showEditForm(@PathVariable Long id, Model model, RedirectAttributes redirectAttributes) {
        return smtpAccountService.getAccountById(id)
                .map(account -> {
                    SMTPAccount formAcc = new SMTPAccount();
                    formAcc.setId(account.getId());
                    formAcc.setName(account.getName());
                    formAcc.setEmail(account.getEmail());
                    formAcc.setHost(account.getHost());
                    formAcc.setPort(account.getPort());
                    formAcc.setUsername(account.getUsername());
                    formAcc.setTls(account.isTls());
                    formAcc.setSsl(account.isSsl());
                    formAcc.setActive(account.isActive());
                    formAcc.setPassword(""); // Leave blank in form
                    
                    model.addAttribute("account", formAcc);
                    return "smtp/form";
                })
                .orElseGet(() -> {
                    redirectAttributes.addFlashAttribute("error", "SMTP Account not found.");
                    return "redirect:/smtp";
                });
    }

    @GetMapping("/delete/{id}")
    public String deleteAccount(@PathVariable Long id, RedirectAttributes redirectAttributes) {
        try {
            smtpAccountService.deleteAccount(id);
            redirectAttributes.addFlashAttribute("success", "SMTP Account deleted successfully.");
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("error", "Error deleting SMTP Account: " + e.getMessage());
        }
        return "redirect:/smtp";
    }

    @PostMapping("/test")
    @ResponseBody
    public ResponseEntity<Map<String, Object>> testConnection(@RequestBody Map<String, String> payload) {
        Map<String, Object> response = new HashMap<>();
        try {
            String host = payload.get("host");
            String portStr = payload.get("port");
            String username = payload.get("username");
            String password = payload.get("password");
            String tlsStr = payload.get("tls");
            String sslStr = payload.get("ssl");
            String accountIdStr = payload.get("id");

            SMTPAccount account = new SMTPAccount();
            account.setHost(host);
            account.setPort(Integer.parseInt(portStr));
            account.setUsername(username);
            account.setTls(Boolean.parseBoolean(tlsStr));
            account.setSsl(Boolean.parseBoolean(sslStr));

            String rawPassword = password;
            if ((rawPassword == null || rawPassword.isEmpty()) && accountIdStr != null && !accountIdStr.isEmpty()) {
                Long id = Long.parseLong(accountIdStr);
                SMTPAccount existing = smtpAccountService.getAccountById(id).orElse(null);
                if (existing != null) {
                    rawPassword = smtpAccountService.decryptPassword(existing.getPassword());
                }
            }

            boolean connected = smtpAccountService.testConnection(account, rawPassword);
            response.put("success", connected);
            response.put("message", connected ? "Connection Successful!" : "Failed to connect. Please verify settings.");
        } catch (Exception e) {
            response.put("success", false);
            response.put("message", "Error testing connection: " + e.getMessage());
        }
        return ResponseEntity.ok(response);
    }
}
