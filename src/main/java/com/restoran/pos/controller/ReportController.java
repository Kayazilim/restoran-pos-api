package com.restoran.pos.controller;

import com.restoran.pos.dto.report.SalesReportResponse;
import com.restoran.pos.security.Permissions;
import com.restoran.pos.service.ReportService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDate;

@RestController
@RequestMapping("/api/reports")
@Tag(name = "Raporlar", description = "YÖNETİCİ satış raporları (REPORT_READ)")
public class ReportController {

    private final ReportService reportService;

    public ReportController(ReportService reportService) {
        this.reportService = reportService;
    }

    @GetMapping("/sales")
    @PreAuthorize("hasAuthority('" + Permissions.REPORT_READ + "')")
    @Operation(summary = "Satış raporu", description = "Tarih verilmezse bugünün raporu döner. Tarih biçimi: YYYY-AA-GG.")
    public SalesReportResponse salesReport(
            @Parameter(description = "Başlangıç günü", example = "2026-11-01")
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
            @Parameter(description = "Bitiş günü (dahil)", example = "2026-11-06")
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to) {
        return reportService.getSalesReport(from, to);
    }
}
