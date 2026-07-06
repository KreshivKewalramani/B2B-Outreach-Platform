package com.example.demo.services;

import com.example.demo.dto.CompanyImportResult;
import com.example.demo.entities.Company;
import com.example.demo.repositories.CompanyRepository;
import org.apache.poi.ss.usermodel.*;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.InputStream;
import java.time.LocalDateTime;
import java.util.*;
import java.util.regex.Pattern;

@Service
public class CompanyService {

    private final CompanyRepository companyRepository;
    private static final Pattern EMAIL_PATTERN = Pattern.compile("^[A-Za-z0-9+_.-]+@(.+)$");

    public CompanyService(CompanyRepository companyRepository) {
        this.companyRepository = companyRepository;
    }

    public List<Company> getAllCompanies() {
        return companyRepository.findAll();
    }

    public Optional<Company> getCompanyById(Long id) {
        return companyRepository.findById(id);
    }

    public Company saveCompany(Company company) {
        return companyRepository.save(company);
    }

    public void deleteCompany(Long id) {
        companyRepository.deleteById(id);
    }

    public void bulkDelete(List<Long> ids) {
        if (ids != null && !ids.isEmpty()) {
            companyRepository.deleteAllById(ids);
        }
    }

    public List<String> getDistinctIndustries() {
        return companyRepository.findDistinctIndustries();
    }

    public List<Company> searchAndFilter(String query, String industry) {
        return companyRepository.searchAndFilter(query, industry);
    }

    public CompanyImportResult importFromExcel(MultipartFile file) {
        CompanyImportResult result = new CompanyImportResult();
        Set<String> processedEmailsInSheet = new HashSet<>();

        try (InputStream is = file.getInputStream();
             Workbook workbook = WorkbookFactory.create(is)) {

            Sheet sheet = workbook.getSheetAt(0);
            Iterator<Row> rowIterator = sheet.rowIterator();

            if (!rowIterator.hasNext()) {
                result.addLog("Excel sheet is empty.");
                return result;
            }

            Row headerRow = rowIterator.next();
            Map<String, Integer> colMap = parseHeaders(headerRow);

            if (!colMap.containsKey("company name") || !colMap.containsKey("email")) {
                result.addLog("Required headers 'Company Name' and 'Email' are missing.");
                result.setInvalid(1);
                return result;
            }

            while (rowIterator.hasNext()) {
                Row row = rowIterator.next();
                if (isRowEmpty(row)) {
                    result.incrementSkipped();
                    continue;
                }

                String companyName = getCellValue(row, colMap.get("company name"));
                String email = getCellValue(row, colMap.get("email")).trim();
                String contactPerson = colMap.containsKey("contact person") ? getCellValue(row, colMap.get("contact person")) : "";
                String phone = colMap.containsKey("phone") ? getCellValue(row, colMap.get("phone")) : "";
                String designation = colMap.containsKey("designation") ? getCellValue(row, colMap.get("designation")) : "";
                String industry = colMap.containsKey("industry") ? getCellValue(row, colMap.get("industry")) : "";

                if (companyName.isEmpty()) {
                    result.incrementInvalid();
                    result.addLog("Row " + (row.getRowNum() + 1) + ": Missing Company Name (Skipped).");
                    continue;
                }

                if (email.isEmpty() || !EMAIL_PATTERN.matcher(email).matches()) {
                    result.incrementInvalid();
                    result.addLog("Row " + (row.getRowNum() + 1) + ": Invalid Email '" + email + "' (Skipped).");
                    continue;
                }

                if (processedEmailsInSheet.contains(email.toLowerCase())) {
                    result.incrementDuplicate();
                    result.addLog("Row " + (row.getRowNum() + 1) + ": Duplicate Email '" + email + "' in sheet (Skipped).");
                    continue;
                }
                processedEmailsInSheet.add(email.toLowerCase());

                Optional<Company> existingOpt = companyRepository.findByEmail(email);
                if (existingOpt.isPresent()) {
                    result.incrementDuplicate();
                    result.addLog("Row " + (row.getRowNum() + 1) + ": Email '" + email + "' already exists in Database (Skipped).");
                    continue;
                }

                Company company = new Company();
                company.setCompanyName(companyName);
                company.setEmail(email);
                company.setContactPerson(contactPerson);
                company.setPhoneNumber(phone);
                company.setDesignation(designation);
                company.setIndustry(industry);
                company.setDateAdded(LocalDateTime.now());

                companyRepository.save(company);
                result.incrementImported();
            }

        } catch (Exception e) {
            result.addLog("Error reading file: " + e.getMessage());
        }

        return result;
    }

    private Map<String, Integer> parseHeaders(Row headerRow) {
        Map<String, Integer> colMap = new HashMap<>();
        for (Cell cell : headerRow) {
            if (cell != null) {
                String headerName = cell.getStringCellValue().trim().toLowerCase();
                colMap.put(headerName, cell.getColumnIndex());
            }
        }
        return colMap;
    }

    private boolean isRowEmpty(Row row) {
        if (row == null) return true;
        for (int c = row.getFirstCellNum(); c < row.getLastCellNum(); c++) {
            Cell cell = row.getCell(c);
            if (cell != null && cell.getCellType() != CellType.BLANK && !getCellValue(row, c).trim().isEmpty()) {
                return false;
            }
        }
        return true;
    }

    private String getCellValue(Row row, Integer colIdx) {
        if (colIdx == null || row == null) return "";
        Cell cell = row.getCell(colIdx);
        if (cell == null) return "";
        
        switch (cell.getCellType()) {
            case STRING:
                return cell.getStringCellValue().trim();
            case NUMERIC:
                if (DateUtil.isCellDateFormatted(cell)) {
                    return cell.getDateCellValue().toString();
                }
                double val = cell.getNumericCellValue();
                if (val == (long) val) {
                    return String.format("%d", (long) val);
                } else {
                    return String.format("%s", val);
                }
            case BOOLEAN:
                return String.valueOf(cell.getBooleanCellValue());
            case FORMULA:
                try {
                    return cell.getStringCellValue().trim();
                } catch (Exception e) {
                    return String.valueOf(cell.getNumericCellValue());
                }
            default:
                return "";
        }
    }
}
