package com.example.demo.services;

import com.example.demo.entities.EmailLog;
import com.example.demo.repositories.EmailLogRepository;
import com.lowagie.text.Document;
import com.lowagie.text.Element;
import com.lowagie.text.Font;
import com.lowagie.text.FontFactory;
import com.lowagie.text.Paragraph;
import com.lowagie.text.Phrase;
import com.lowagie.text.PageSize;
import com.lowagie.text.pdf.PdfPCell;
import com.lowagie.text.pdf.PdfPTable;
import com.lowagie.text.pdf.PdfWriter;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.CellStyle;
import org.apache.poi.ss.usermodel.IndexedColors;
import org.apache.poi.ss.usermodel.FillPatternType;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.stereotype.Service;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.time.format.DateTimeFormatter;
import java.util.List;

@Service
public class ReportService {

    private final EmailLogRepository emailLogRepository;

    public ReportService(EmailLogRepository emailLogRepository) {
        this.emailLogRepository = emailLogRepository;
    }

    public byte[] exportToExcel() throws IOException {
        List<EmailLog> logs = emailLogRepository.findAll();
        try (Workbook workbook = new XSSFWorkbook();
             ByteArrayOutputStream out = new ByteArrayOutputStream()) {

            Sheet sheet = workbook.createSheet("Email Outreach Logs");

            org.apache.poi.ss.usermodel.Font headerFont = workbook.createFont();
            headerFont.setBold(true);
            headerFont.setColor(IndexedColors.WHITE.getIndex());

            CellStyle headerCellStyle = workbook.createCellStyle();
            headerCellStyle.setFont(headerFont);
            headerCellStyle.setFillForegroundColor(IndexedColors.DARK_BLUE.getIndex());
            headerCellStyle.setFillPattern(FillPatternType.SOLID_FOREGROUND);

            Row headerRow = sheet.createRow(0);
            String[] headers = {"ID", "Recipient", "Subject", "Timestamp", "Status", "Error Message", "Campaign", "SMTP Account"};
            for (int i = 0; i < headers.length; i++) {
                Cell cell = headerRow.createCell(i);
                cell.setCellValue(headers[i]);
                cell.setCellStyle(headerCellStyle);
            }

            DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

            int rowIdx = 1;
            for (EmailLog log : logs) {
                Row row = sheet.createRow(rowIdx++);
                row.createCell(0).setCellValue(log.getId() != null ? log.getId() : 0);
                row.createCell(1).setCellValue(log.getRecipient() != null ? log.getRecipient() : "");
                row.createCell(2).setCellValue(log.getSubject() != null ? log.getSubject() : "");
                row.createCell(3).setCellValue(log.getTimestamp() != null ? log.getTimestamp().format(formatter) : "");
                row.createCell(4).setCellValue(log.getStatus() != null ? log.getStatus() : "");
                row.createCell(5).setCellValue(log.getErrorMessage() != null ? log.getErrorMessage() : "");
                row.createCell(6).setCellValue(log.getCampaign() != null ? log.getCampaign().getCampaignName() : "N/A");
                row.createCell(7).setCellValue(log.getSmtpAccount() != null ? log.getSmtpAccount().getName() : "N/A");
            }

            for (int i = 0; i < headers.length; i++) {
                sheet.autoSizeColumn(i);
            }

            workbook.write(out);
            return out.toByteArray();
        }
    }

    public byte[] exportToCsv() {
        List<EmailLog> logs = emailLogRepository.findAll();
        StringBuilder sb = new StringBuilder();
        sb.append("ID,Recipient,Subject,Timestamp,Status,Error Message,Campaign,SMTP Account\n");

        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

        for (EmailLog log : logs) {
            sb.append(log.getId()).append(",")
              .append(escapeCsvField(log.getRecipient())).append(",")
              .append(escapeCsvField(log.getSubject())).append(",")
              .append(log.getTimestamp() != null ? log.getTimestamp().format(formatter) : "").append(",")
              .append(escapeCsvField(log.getStatus())).append(",")
              .append(escapeCsvField(log.getErrorMessage())).append(",")
              .append(log.getCampaign() != null ? escapeCsvField(log.getCampaign().getCampaignName()) : "N/A").append(",")
              .append(log.getSmtpAccount() != null ? escapeCsvField(log.getSmtpAccount().getName()) : "N/A")
              .append("\n");
        }

        return sb.toString().getBytes(java.nio.charset.StandardCharsets.UTF_8);
    }

    private String escapeCsvField(String field) {
        if (field == null) {
            return "";
        }
        if (field.contains(",") || field.contains("\"") || field.contains("\n")) {
            return "\"" + field.replace("\"", "\"\"") + "\"";
        }
        return field;
    }

    public byte[] exportToPdf() {
        List<EmailLog> logs = emailLogRepository.findAll();
        ByteArrayOutputStream out = new ByteArrayOutputStream();

        Document document = new Document(PageSize.A4.rotate());
        try {
            PdfWriter.getInstance(document, out);
            document.open();

            Font titleFont = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 18);
            Paragraph title = new Paragraph("Email Outreach Platform - Performance Logs Report", titleFont);
            title.setAlignment(Element.ALIGN_CENTER);
            title.setSpacingAfter(20);
            document.add(title);

            PdfPTable table = new PdfPTable(7);
            table.setWidthPercentage(100);
            table.setWidths(new float[]{2.0f, 2.5f, 1.8f, 1.0f, 2.2f, 1.5f, 1.5f});

            Font headerFont = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 10, Font.BOLD, java.awt.Color.WHITE);
            PdfPCell cell = new PdfPCell();
            cell.setBackgroundColor(java.awt.Color.DARK_GRAY);
            cell.setPadding(5);

            String[] headers = {"Recipient", "Subject", "Timestamp", "Status", "Error Message", "Campaign", "SMTP"};
            for (String header : headers) {
                cell.setPhrase(new Phrase(header, headerFont));
                table.addCell(cell);
            }

            Font cellFont = FontFactory.getFont(FontFactory.HELVETICA, 8);
            DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm");

            for (EmailLog log : logs) {
                table.addCell(new Phrase(log.getRecipient() != null ? log.getRecipient() : "", cellFont));
                table.addCell(new Phrase(log.getSubject() != null ? log.getSubject() : "", cellFont));
                table.addCell(new Phrase(log.getTimestamp() != null ? log.getTimestamp().format(formatter) : "", cellFont));
                table.addCell(new Phrase(log.getStatus() != null ? log.getStatus() : "", cellFont));
                table.addCell(new Phrase(log.getErrorMessage() != null ? log.getErrorMessage() : "", cellFont));
                table.addCell(new Phrase(log.getCampaign() != null ? log.getCampaign().getCampaignName() : "N/A", cellFont));
                table.addCell(new Phrase(log.getSmtpAccount() != null ? log.getSmtpAccount().getName() : "N/A", cellFont));
            }

            document.add(table);
            document.close();
        } catch (Exception e) {
            e.printStackTrace();
        }

        return out.toByteArray();
    }
}
