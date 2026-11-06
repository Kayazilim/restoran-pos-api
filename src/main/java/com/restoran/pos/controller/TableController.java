package com.restoran.pos.controller;

import com.restoran.pos.dto.table.QrCredentialsResponse;
import com.restoran.pos.dto.table.TableCreateRequest;
import com.restoran.pos.dto.table.TableCreatedResponse;
import com.restoran.pos.dto.table.TablePatchRequest;
import com.restoran.pos.dto.table.TableResponse;
import com.restoran.pos.dto.table.TableUpdateRequest;
import com.restoran.pos.security.Permissions;
import com.restoran.pos.security.PosUserDetails;
import com.restoran.pos.service.TableService;
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
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

import java.net.URI;
import java.util.List;

@RestController
@RequestMapping("/api/tables")
@Tag(name = "Masalar", description = "YÖNETİCİ masaları tanımlar; GARSON kendi masalarını, MÜŞTERİ yalnızca kendi masasını görür")
public class TableController {

    private final TableService tableService;

    public TableController(TableService tableService) {
        this.tableService = tableService;
    }

    @GetMapping
    @PreAuthorize("hasAuthority('" + Permissions.TABLE_READ + "')")
    @Operation(summary = "Masaları listele", description = "Kayıt sahipliğine göre: garson yalnızca kendisine atanmış masaları, "
            + "müşteri yalnızca kendi masasını, kasiyer ve yönetici tümünü görür.")
    public List<TableResponse> findAll(@Parameter(hidden = true) @AuthenticationPrincipal PosUserDetails principal) {
        return tableService.findAll(principal.currentUser());
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasAuthority('" + Permissions.TABLE_READ + "')")
    @Operation(summary = "Kimliğe göre masa getir")
    public TableResponse findById(@PathVariable Long id,
                                  @Parameter(hidden = true) @AuthenticationPrincipal PosUserDetails principal) {
        return tableService.findById(id, principal.currentUser());
    }

    @PostMapping
    @PreAuthorize("hasAuthority('" + Permissions.TABLE_WRITE + "')")
    @Operation(summary = "Yeni masa oluştur", description = "Masaya özel QR müşteri hesabı da oluşturulur; anahtar yalnızca bu yanıtta gösterilir.")
    @ApiResponse(responseCode = "201", description = "Masa oluşturuldu",
            content = @Content(schema = @Schema(implementation = TableCreatedResponse.class)))
    public ResponseEntity<TableCreatedResponse> create(@Valid @RequestBody TableCreateRequest request) {
        TableCreatedResponse created = tableService.create(request);
        URI location = ServletUriComponentsBuilder.fromCurrentRequest()
                .path("/{id}").buildAndExpand(created.table().id()).toUri();
        return ResponseEntity.created(location).body(created);
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasAuthority('" + Permissions.TABLE_WRITE + "')")
    @Operation(summary = "Masayı tam güncelle (PUT)")
    public TableResponse update(@PathVariable Long id, @Valid @RequestBody TableUpdateRequest request) {
        return tableService.update(id, request);
    }

    @PatchMapping("/{id}")
    @PreAuthorize("hasAuthority('" + Permissions.TABLE_WRITE + "')")
    @Operation(summary = "Masayı kısmi güncelle (PATCH)", description = "Ör. yalnızca garson ataması değiştirilebilir.")
    public TableResponse patch(@PathVariable Long id, @Valid @RequestBody TablePatchRequest request) {
        return tableService.patch(id, request);
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasAuthority('" + Permissions.TABLE_DELETE + "')")
    @Operation(summary = "Masayı sil", description = "Adisyon geçmişi olan masa silinemez (409).")
    @ApiResponse(responseCode = "204", description = "Masa silindi")
    public ResponseEntity<Void> delete(@PathVariable Long id) {
        tableService.delete(id);
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/{id}/qr-code")
    @PreAuthorize("hasAuthority('" + Permissions.TABLE_WRITE + "')")
    @Operation(summary = "Masanın QR kodunu yenile", description = "Yeni anahtar üretilir; eski QR kodu geçersiz olur.")
    public QrCredentialsResponse regenerateQrCode(@PathVariable Long id) {
        return tableService.regenerateQrCode(id);
    }
}
