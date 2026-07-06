package com.example.demo.dto;

import java.util.ArrayList;
import java.util.List;

public class CompanyImportResult {
    private int imported = 0;
    private int skipped = 0;
    private int duplicate = 0;
    private int invalid = 0;
    private List<String> logs = new ArrayList<>();

    public int getImported() { return imported; }
    public void setImported(int imported) { this.imported = imported; }
    public void incrementImported() { this.imported++; }

    public int getSkipped() { return skipped; }
    public void setSkipped(int skipped) { this.skipped = skipped; }
    public void incrementSkipped() { this.skipped++; }

    public int getDuplicate() { return duplicate; }
    public void setDuplicate(int duplicate) { this.duplicate = duplicate; }
    public void incrementDuplicate() { this.duplicate++; }

    public int getInvalid() { return invalid; }
    public void setInvalid(int invalid) { this.invalid = invalid; }
    public void incrementInvalid() { this.invalid++; }

    public List<String> getLogs() { return logs; }
    public void addLog(String log) { this.logs.add(log); }
}
