package com.restoran.pos.controller;

import com.restoran.pos.dto.kitchen.KitchenOrderResponse;
import com.restoran.pos.dto.kitchen.KitchenStatusPatchRequest;
import com.restoran.pos.entity.OrderStatus;
import com.restoran.pos.security.Permissions;
import com.restoran.pos.security.PosUserDetails;
import com.restoran.pos.service.KitchenService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.MediaType;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.util.List;

@RestController
@RequestMapping("/api/kitchen")
@Tag(name = "Mutfak", description = "MUTFAK yeni siparişleri görür ve hazırlık durumunu günceller; ödeme ve stok bilgisine erişemez")
public class KitchenController {

    private final KitchenService kitchenService;

    public KitchenController(KitchenService kitchenService) {
        this.kitchenService = kitchenService;
    }

    @GetMapping("/orders")
    @PreAuthorize("hasAuthority('" + Permissions.KITCHEN_READ + "')")
    @Operation(summary = "Mutfak siparişlerini listele",
            description = "Varsayılan olarak hazırlanmayı bekleyen (CONFIRMED) ve hazırlanan (PREPARING) siparişler, en eskisi önce.")
    public List<KitchenOrderResponse> findOrders(
            @Parameter(description = "Durum filtresi", example = "CONFIRMED") @RequestParam(required = false) OrderStatus status) {
        return kitchenService.findOrders(status);
    }

    @GetMapping("/orders/{id}")
    @PreAuthorize("hasAuthority('" + Permissions.KITCHEN_READ + "')")
    @Operation(summary = "Mutfak görünümünde sipariş getir")
    public KitchenOrderResponse findById(@PathVariable Long id) {
        return kitchenService.findById(id);
    }

    @PatchMapping("/orders/{id}")
    @PreAuthorize("hasAuthority('" + Permissions.KITCHEN_WRITE + "')")
    @Operation(summary = "Hazırlık durumunu güncelle (PATCH)", description = "Onaylandı → Hazırlanıyor → Hazır.")
    public KitchenOrderResponse updateStatus(@PathVariable Long id, @Valid @RequestBody KitchenStatusPatchRequest request) {
        return kitchenService.updateStatus(id, request);
    }

    @GetMapping(path = "/stream", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    @PreAuthorize("hasAuthority('" + Permissions.KITCHEN_READ + "')")
    @Operation(summary = "Anlık sipariş bildirimleri (SSE)",
            description = "Mutfak ekranı bu bağlantıyı bir kez açar; sunucu yalnızca sipariş geldiğinde veya değiştiğinde "
                    + "'order-created', 'order-updated', 'order-status-changed', 'order-deleted' olaylarını gönderir. "
                    + "Sürekli sorgulama (polling) gerekmez.")
    public SseEmitter stream(@Parameter(hidden = true) @AuthenticationPrincipal PosUserDetails principal) {
        return kitchenService.subscribe(principal.currentUser());
    }
}
