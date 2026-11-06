package com.restoran.pos.controller;

import com.restoran.pos.dto.product.ProductCreateRequest;
import com.restoran.pos.dto.product.ProductPatchRequest;
import com.restoran.pos.dto.product.ProductResponse;
import com.restoran.pos.dto.product.ProductUpdateRequest;
import com.restoran.pos.security.Permissions;
import com.restoran.pos.service.ProductService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

import java.net.URI;
import java.util.List;

@RestController
@RequestMapping("/api/products")
@Tag(name = "Ürünler", description = "Ürün, fiyat ve stok yönetimi (yalnızca YÖNETİCİ). "
        + "Okuma: PRODUCT_READ, ekleme/güncelleme (PUT, PATCH): PRODUCT_WRITE, silme: PRODUCT_DELETE")
public class ProductController {

    private final ProductService productService;

    public ProductController(ProductService productService) {
        this.productService = productService;
    }

    @GetMapping
    @PreAuthorize("hasAuthority('" + Permissions.PRODUCT_READ + "')")
    @Operation(summary = "Ürünleri listele",
            description = "Kategori ve/veya aktiflik alanına göre parametreli JPQL sorgusuyla filtrelenebilir.")
    public List<ProductResponse> findAll(
            @Parameter(description = "Kategori filtresi", example = "Tatlılar")
            @RequestParam(required = false) String category,
            @Parameter(description = "Aktiflik filtresi", example = "true")
            @RequestParam(required = false) Boolean active) {
        return productService.findAll(category, active);
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasAuthority('" + Permissions.PRODUCT_READ + "')")
    @Operation(summary = "Kimliğe göre ürün getir")
    public ProductResponse findById(@PathVariable Long id) {
        return productService.findById(id);
    }

    @PostMapping
    @PreAuthorize("hasAuthority('" + Permissions.PRODUCT_WRITE + "')")
    @Operation(summary = "Yeni ürün oluştur",
            description = "Kimlik veritabanı tarafından üretilir; gövdede gönderilen 'id' yok sayılır ve mevcut bir kaydı güncelleyemez.")
    @ApiResponse(responseCode = "201", description = "Ürün oluşturuldu",
            content = @Content(schema = @Schema(implementation = ProductResponse.class)))
    public ResponseEntity<ProductResponse> create(@Valid @RequestBody ProductCreateRequest request) {
        ProductResponse created = productService.create(request);
        URI location = ServletUriComponentsBuilder.fromCurrentRequest()
                .path("/{id}").buildAndExpand(created.id()).toUri();
        return ResponseEntity.created(location).body(created);
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasAuthority('" + Permissions.PRODUCT_WRITE + "')")
    @Operation(summary = "Ürünü tam güncelle (PUT)", description = "Tüm alanlar gönderilir ve kaydın tamamı değiştirilir.")
    public ProductResponse update(@PathVariable Long id, @Valid @RequestBody ProductUpdateRequest request) {
        return productService.update(id, request);
    }

    @PatchMapping("/{id}")
    @PreAuthorize("hasAuthority('" + Permissions.PRODUCT_WRITE + "')")
    @Operation(summary = "Ürünü kısmi güncelle (PATCH)",
            description = "Yalnızca gönderilen alanlar değişir, diğerleri korunur. Gövdeyle birincil anahtar değiştirilemez (400).")
    public ProductResponse patch(@PathVariable Long id, @Valid @RequestBody ProductPatchRequest request) {
        return productService.patch(id, request);
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasAuthority('" + Permissions.PRODUCT_DELETE + "')")
    @Operation(summary = "Ürünü sil", description = "Geçmiş siparişlerde kullanılmış ürünler silinemez (409); pasif yapılabilir.")
    @ApiResponse(responseCode = "204", description = "Ürün silindi")
    public ResponseEntity<Void> delete(@PathVariable Long id) {
        productService.delete(id);
        return ResponseEntity.noContent().build();
    }
}
