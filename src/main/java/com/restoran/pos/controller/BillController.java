package com.restoran.pos.controller;

import com.restoran.pos.dto.bill.BillPatchRequest;
import com.restoran.pos.dto.bill.BillResponse;
import com.restoran.pos.dto.bill.PaymentCreateRequest;
import com.restoran.pos.dto.bill.PaymentReceiptResponse;
import com.restoran.pos.dto.bill.PaymentResponse;
import com.restoran.pos.dto.bill.PaymentResult;
import com.restoran.pos.entity.BillStatus;
import com.restoran.pos.security.Permissions;
import com.restoran.pos.security.PosUserDetails;
import com.restoran.pos.service.BillService;
import com.restoran.pos.util.IdempotencyKeys;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/bills")
@Tag(name = "Adisyon ve Ödeme", description = "KASİYER ödeme alır ve adisyonu kapatır; GARSON ve MÜŞTERİ kendi masalarının "
        + "hesabını görür; YÖNETİCİ tüm adisyonları görür")
public class BillController {

    private final BillService billService;

    public BillController(BillService billService) {
        this.billService = billService;
    }

    @GetMapping
    @PreAuthorize("hasAuthority('" + Permissions.BILL_READ + "')")
    @Operation(summary = "Adisyonları listele", description = "Kasiyer için açık adisyonlar: ?status=OPEN. Sonuçlar kayıt sahipliğine göre süzülür.")
    public List<BillResponse> findAll(
            @Parameter(description = "Durum filtresi", example = "OPEN") @RequestParam(required = false) BillStatus status,
            @Parameter(hidden = true) @AuthenticationPrincipal PosUserDetails principal) {
        return billService.findAll(status, principal.currentUser());
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasAuthority('" + Permissions.BILL_READ + "')")
    @Operation(summary = "Adisyonu (hesabı) getir")
    public BillResponse findById(@PathVariable Long id,
                                 @Parameter(hidden = true) @AuthenticationPrincipal PosUserDetails principal) {
        return billService.findById(id, principal.currentUser());
    }

    @GetMapping("/{id}/payments")
    @PreAuthorize("hasAuthority('" + Permissions.BILL_READ + "')")
    @Operation(summary = "Adisyonun ödemelerini listele")
    public List<PaymentResponse> findPayments(@PathVariable Long id,
                                              @Parameter(hidden = true) @AuthenticationPrincipal PosUserDetails principal) {
        return billService.findPayments(id, principal.currentUser());
    }

    @PostMapping("/{id}/payments")
    @PreAuthorize("hasAuthority('" + Permissions.BILL_WRITE + "')")
    @Operation(summary = "Ödeme al", description = "Aynı Idempotency-Key ile gelen tekrar istekleri yeniden tahsil edilmez (çifte ücret önlenir).")
    @ApiResponse(responseCode = "201", description = "Ödeme alındı",
            content = @Content(schema = @Schema(implementation = PaymentReceiptResponse.class)))
    @ApiResponse(responseCode = "200", description = "Aynı anahtarla daha önce alınmış ödeme (tekrar tahsil edilmedi)",
            content = @Content(schema = @Schema(implementation = PaymentReceiptResponse.class)))
    public ResponseEntity<PaymentReceiptResponse> addPayment(
            @PathVariable Long id,
            @Parameter(description = "Her ödeme denemesi için benzersiz anahtar (ör. UUID)", required = true)
            @RequestHeader(IdempotencyKeys.HEADER) String idempotencyKey,
            @Valid @RequestBody PaymentCreateRequest request,
            @Parameter(hidden = true) @AuthenticationPrincipal PosUserDetails principal) {
        PaymentResult result = billService.addPayment(id, request, idempotencyKey, principal.currentUser());
        if (result.replayed()) {
            return ResponseEntity.ok().header(IdempotencyKeys.REPLAYED_HEADER, "true").body(result.receipt());
        }
        return ResponseEntity.status(HttpStatus.CREATED).body(result.receipt());
    }

    @PatchMapping("/{id}")
    @PreAuthorize("hasAuthority('" + Permissions.BILL_WRITE + "')")
    @Operation(summary = "Adisyonu kapat (PATCH)", description = "Gövde: {\"status\":\"CLOSED\"}. Kalan tutar sıfır değilse 409 döner.")
    public BillResponse patch(@PathVariable Long id, @Valid @RequestBody BillPatchRequest request,
                              @Parameter(hidden = true) @AuthenticationPrincipal PosUserDetails principal) {
        return billService.patch(id, request, principal.currentUser());
    }
}
