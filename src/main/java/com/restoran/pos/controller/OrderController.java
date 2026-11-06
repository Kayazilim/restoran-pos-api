package com.restoran.pos.controller;

import com.restoran.pos.dto.order.OrderCreateRequest;
import com.restoran.pos.dto.order.OrderPatchRequest;
import com.restoran.pos.dto.order.OrderPlacementResult;
import com.restoran.pos.dto.order.OrderResponse;
import com.restoran.pos.dto.order.OrderUpdateRequest;
import com.restoran.pos.entity.OrderStatus;
import com.restoran.pos.security.Permissions;
import com.restoran.pos.security.PosUserDetails;
import com.restoran.pos.service.OrderService;
import com.restoran.pos.util.IdempotencyKeys;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

import java.net.URI;
import java.util.List;

@RestController
@RequestMapping("/api/orders")
@Tag(name = "Siparişler", description = "MÜŞTERİ kendi masası için verir ve izler; GARSON kendi masalarının siparişlerini "
        + "açar, düzenler ve iptal eder; KASİYER ve YÖNETİCİ tüm siparişleri görür")
public class OrderController {

    private final OrderService orderService;

    public OrderController(OrderService orderService) {
        this.orderService = orderService;
    }

    @GetMapping
    @PreAuthorize("hasAuthority('" + Permissions.ORDER_READ + "')")
    @Operation(summary = "Siparişleri listele", description = "Sonuçlar kayıt sahipliğine göre süzülür.")
    public List<OrderResponse> findAll(
            @Parameter(description = "Durum filtresi", example = "CONFIRMED") @RequestParam(required = false) OrderStatus status,
            @Parameter(description = "Masa filtresi", example = "1") @RequestParam(required = false) Long tableId,
            @Parameter(hidden = true) @AuthenticationPrincipal PosUserDetails principal) {
        return orderService.findAll(status, tableId, principal.currentUser());
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasAuthority('" + Permissions.ORDER_READ + "')")
    @Operation(summary = "Kimliğe göre sipariş getir (sipariş takibi)")
    public OrderResponse findById(@PathVariable Long id,
                                  @Parameter(hidden = true) @AuthenticationPrincipal PosUserDetails principal) {
        return orderService.findById(id, principal.currentUser());
    }

    @PostMapping
    @PreAuthorize("hasAuthority('" + Permissions.ORDER_WRITE + "')")
    @Operation(summary = "Sipariş ver",
            description = "Stok, ürün satırı kilitlenerek düşülür; yetersizse 409 döner. 'Siparişi Ver' düğmesine arka arkaya "
                    + "basılsa bile aynı Idempotency-Key ile gelen tekrarlar yeni sipariş açmaz: önceki sipariş 200 ve "
                    + "'Idempotent-Replayed: true' başlığıyla döner.")
    @ApiResponse(responseCode = "201", description = "Sipariş oluşturuldu ve onaylandı",
            content = @Content(schema = @Schema(implementation = OrderResponse.class)))
    @ApiResponse(responseCode = "200", description = "Aynı Idempotency-Key ile daha önce oluşturulmuş sipariş (tekrar işlenmedi)",
            content = @Content(schema = @Schema(implementation = OrderResponse.class)))
    public ResponseEntity<OrderResponse> create(
            @Parameter(description = "İstemcinin her sipariş denemesi için ürettiği benzersiz anahtar (ör. UUID)", required = true)
            @RequestHeader(IdempotencyKeys.HEADER) String idempotencyKey,
            @Valid @RequestBody OrderCreateRequest request,
            @Parameter(hidden = true) @AuthenticationPrincipal PosUserDetails principal) {
        OrderPlacementResult result = orderService.placeOrder(request, idempotencyKey, principal.currentUser());
        if (result.replayed()) {
            return ResponseEntity.ok().header(IdempotencyKeys.REPLAYED_HEADER, "true").body(result.order());
        }
        URI location = ServletUriComponentsBuilder.fromCurrentRequest()
                .path("/{id}").buildAndExpand(result.order().id()).toUri();
        return ResponseEntity.created(location).body(result.order());
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasAuthority('" + Permissions.ORDER_WRITE + "')")
    @Operation(summary = "Siparişi tam güncelle (PUT)", description = "Garson, kendi masasındaki 'Onaylandı' durumundaki "
            + "siparişin kalemlerini değiştirir; stok farkı otomatik düzeltilir.")
    public OrderResponse update(@PathVariable Long id, @Valid @RequestBody OrderUpdateRequest request,
                                @Parameter(hidden = true) @AuthenticationPrincipal PosUserDetails principal) {
        return orderService.update(id, request, principal.currentUser());
    }

    @PatchMapping("/{id}")
    @PreAuthorize("hasAuthority('" + Permissions.ORDER_WRITE + "')")
    @Operation(summary = "Siparişi kısmi güncelle (PATCH)", description = "Yalnızca gönderilen alanlar (not ve/veya kalemler) değişir.")
    public OrderResponse patch(@PathVariable Long id, @Valid @RequestBody OrderPatchRequest request,
                               @Parameter(hidden = true) @AuthenticationPrincipal PosUserDetails principal) {
        return orderService.patch(id, request, principal.currentUser());
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasAuthority('" + Permissions.ORDER_DELETE + "')")
    @Operation(summary = "Siparişi sil",
            description = "Mutfak hazırlamaya başlamadıysa sipariş ve kalemleri veritabanından silinir, ürünler stoğa iade "
                    + "edilir ve tutar adisyondan düşülür. Adisyonda başka sipariş ve ödeme kalmazsa boş adisyon da silinir.")
    @ApiResponse(responseCode = "204", description = "Sipariş silindi")
    public ResponseEntity<Void> delete(@PathVariable Long id,
                                       @Parameter(hidden = true) @AuthenticationPrincipal PosUserDetails principal) {
        orderService.delete(id, principal.currentUser());
        return ResponseEntity.noContent().build();
    }
}
