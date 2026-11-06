package com.restoran.pos.service;

import com.restoran.pos.dto.report.SalesReportResponse;

import java.time.LocalDate;

public interface ReportService {

    SalesReportResponse getSalesReport(LocalDate from, LocalDate to);
}
