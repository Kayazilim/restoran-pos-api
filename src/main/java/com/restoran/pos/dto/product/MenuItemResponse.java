package com.restoran.pos.dto.product;

import com.restoran.pos.entity.Product;
import com.restoran.pos.util.AllergenFormatter;
import io.swagger.v3.oas.annotations.media.Schema;

import java.math.BigDecimal;
import java.util.List;

@Schema(description = "Menü kalemi (tüm rollerin gördüğü görünüm; stok adedi içermez)")
public record MenuItemResponse(
        @Schema(example = "8") Long id,
        @Schema(example = "Fıstıklı Baklava") String name,
        String description,
        @Schema(example = "Tatlılar") String category,
        @Schema(example = "220.00") BigDecimal price,
        List<String> allergens,
        @Schema(description = "Şu anda sipariş verilebilir mi (tükendiyse false)") boolean available) {

    public static MenuItemResponse from(Product product) {
        return new MenuItemResponse(product.getId(), product.getName(), product.getDescription(),
                product.getCategory(), product.getPrice(), AllergenFormatter.split(product.getAllergens()),
                product.isAvailable());
    }
}
