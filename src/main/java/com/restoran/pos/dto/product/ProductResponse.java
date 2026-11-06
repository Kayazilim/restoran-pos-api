package com.restoran.pos.dto.product;

import com.restoran.pos.entity.Product;
import com.restoran.pos.util.AllergenFormatter;
import io.swagger.v3.oas.annotations.media.Schema;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;

@Schema(description = "Ürün (yönetici görünümü: stok ve sürüm bilgisi dahil)")
public record ProductResponse(
        @Schema(example = "1") Long id,
        @Schema(example = "Fıstıklı Baklava") String name,
        String description,
        @Schema(example = "Tatlılar") String category,
        @Schema(example = "220.00") BigDecimal price,
        @Schema(example = "1") Integer stockQuantity,
        List<String> allergens,
        boolean active,
        @Schema(description = "Aktif ve stokta varsa true") boolean available,
        @Schema(description = "İyimser kilit sürümü") Long version,
        Instant createdAt,
        Instant updatedAt) {

    public static ProductResponse from(Product product) {
        return new ProductResponse(product.getId(), product.getName(), product.getDescription(),
                product.getCategory(), product.getPrice(), product.getStockQuantity(),
                AllergenFormatter.split(product.getAllergens()), product.isActive(), product.isAvailable(),
                product.getVersion(), product.getCreatedAt(), product.getUpdatedAt());
    }
}
