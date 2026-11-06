package com.restoran.pos.service.impl;

import com.restoran.pos.dto.report.SalesReportResponse;
import com.restoran.pos.dto.report.TopProductResponse;
import com.restoran.pos.entity.PaymentMethod;
import com.restoran.pos.exception.BadRequestException;
import com.restoran.pos.repository.ReportRepository;
import com.restoran.pos.service.ReportService;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Map;

@Service
public class ReportServiceImpl implements ReportService {

    private static final int TOP_PRODUCT_LIMIT = 5;
    private static final long MAX_RANGE_DAYS = 366;

    private final ReportRepository reportRepository;
    private final Clock clock;
    private final ZoneId zoneId;

    public ReportServiceImpl(ReportRepository reportRepository,
                             Clock clock,
                             @Value("${pos.report.zone-id}") String zoneId) {
        this.reportRepository = reportRepository;
        this.clock = clock;
        this.zoneId = ZoneId.of(zoneId);
    }

    @Override
    @Transactional(readOnly = true)
    public SalesReportResponse getSalesReport(LocalDate from, LocalDate to) {
        LocalDate start = from != null ? from : LocalDate.now(clock.withZone(zoneId));
        LocalDate end = to != null ? to : start;
        if (end.isBefore(start)) {
            throw new BadRequestException("'to' tarihi 'from' tarihinden önce olamaz.");
        }
        if (ChronoUnit.DAYS.between(start, end) >= MAX_RANGE_DAYS) {
            throw new BadRequestException("Rapor aralığı en fazla " + MAX_RANGE_DAYS + " gün olabilir.");
        }

        // Gün sınırları restoranın saat diliminde hesaplanır: [from 00:00, to+1 00:00)
        Instant fromInstant = start.atStartOfDay(zoneId).toInstant();
        Instant toInstant = end.plusDays(1).atStartOfDay(zoneId).toInstant();

        Map<PaymentMethod, BigDecimal> paymentsByMethod = reportRepository.sumPaymentsByMethod(fromInstant, toInstant);
        BigDecimal collectedPayments = paymentsByMethod.values().stream().reduce(BigDecimal.ZERO, BigDecimal::add);
        List<TopProductResponse> topProducts = reportRepository.findTopProducts(fromInstant, toInstant, TOP_PRODUCT_LIMIT)
                .stream()
                .map(row -> new TopProductResponse(row.productName(), row.quantity(), row.revenue()))
                .toList();

        return new SalesReportResponse(start, end, zoneId.getId(),
                reportRepository.countOrders(fromInstant, toInstant, false),
                reportRepository.countOrders(fromInstant, toInstant, true),
                reportRepository.sumOrderTotals(fromInstant, toInstant),
                collectedPayments,
                paymentsByMethod,
                reportRepository.countClosedBills(fromInstant, toInstant),
                topProducts);
    }
}
