package com.example.demo.controllers;

import com.example.demo.entities.EmailLog;
import com.example.demo.repositories.EmailLogRepository;
import com.example.demo.services.ReportService;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;

import java.util.List;

@Controller
@RequestMapping("/reports")
public class ReportController {

    private final EmailLogRepository emailLogRepository;
    private final ReportService reportService;

    public ReportController(EmailLogRepository emailLogRepository, ReportService reportService) {
        this.emailLogRepository = emailLogRepository;
        this.reportService = reportService;
    }

    @GetMapping
    public String viewReports(Model model) {
        List<EmailLog> logs = emailLogRepository.findAll();
        long total = logs.size();
        long sent = emailLogRepository.countByStatus("SENT");
        long failed = emailLogRepository.countByStatus("FAILED");
        long queued = emailLogRepository.countByStatus("QUEUED") + emailLogRepository.countByStatus("SCHEDULED");

        double successRate = 0.0;
        if (sent + failed > 0) {
            successRate = ((double) sent / (sent + failed)) * 100.0;
        }

        model.addAttribute("logs", logs);
        model.addAttribute("totalCount", total);
        model.addAttribute("sentCount", sent);
        model.addAttribute("failedCount", failed);
        model.addAttribute("queuedCount", queued);
        model.addAttribute("successRate", String.format("%.1f", successRate));

        return "reports/list";
    }

    @GetMapping("/export/excel")
    public ResponseEntity<byte[]> exportToExcel() {
        try {
            byte[] data = reportService.exportToExcel();
            return ResponseEntity.ok()
                    .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=email_outreach_report.xlsx")
                    .contentType(MediaType.parseMediaType("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"))
                    .body(data);
        } catch (Exception e) {
            e.printStackTrace();
            return ResponseEntity.internalServerError().build();
        }
    }

    @GetMapping("/export/csv")
    public ResponseEntity<byte[]> exportToCsv() {
        try {
            byte[] data = reportService.exportToCsv();
            return ResponseEntity.ok()
                    .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=email_outreach_report.csv")
                    .contentType(MediaType.TEXT_PLAIN)
                    .body(data);
        } catch (Exception e) {
            e.printStackTrace();
            return ResponseEntity.internalServerError().build();
        }
    }

    @GetMapping("/export/pdf")
    public ResponseEntity<byte[]> exportToPdf() {
        try {
            byte[] data = reportService.exportToPdf();
            return ResponseEntity.ok()
                    .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=email_outreach_report.pdf")
                    .contentType(MediaType.APPLICATION_PDF)
                    .body(data);
        } catch (Exception e) {
            e.printStackTrace();
            return ResponseEntity.internalServerError().build();
        }
    }
}
