package com.restoran.pos.dto.product;

import io.swagger.v3.oas.annotations.media.ArraySchema;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.PositiveOrZero;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;
import java.util.List;

@Schema(description = "PATCH ile kısmi güncelleme: yalnızca gönderilen alanlar değişir, gönderilmeyenler korunur.")
public record ProductPatchRequest(
        @Schema(description = "Birincil anahtar değiştirilemez; URL'dekinden farklı bir id gönderilirse 400 döner.",
                example = "1")
        Long id,

        @Schema(description = "Ürün adı", example = "Fıstıklı Kadayıf")
        @Pattern(regexp = "(?s).*\\S.*", message = "Ürün adı boş olamaz")
        @Size(max = 100, message = "Ürün adı en fazla 100 karakter olabilir")
        String name,

        @Schema(description = "Ürün açıklaması")
        @Size(max = 500, message = "Açıklama en fazla 500 karakter olabilir")
        String description,

        @Schema(description = "Menü kategorisi", example = "Tatlılar")
        @Pattern(regexp = "(?s).*\\S.*", message = "Kategori boş olamaz")
        @Size(max = 50, message = "Kategori en fazla 50 karakter olabilir")
        String category,

        @Schema(description = "Birim fiyat (TL)", example = "250.00")
        @DecimalMin(value = "0.01", message = "Fiyat 0'dan büyük olmalıdır")
        @Digits(integer = 8, fraction = 2, message = "Fiyat en fazla 8 tam ve 2 ondalık basamak içerebilir")
        BigDecimal price,

        @Schema(description = "Stok adedi", example = "30")
        @PositiveOrZero(message = "Stok adedi negatif olamaz")
        @Max(value = 100000, message = "Stok adedi en fazla 100000 olabilir")
        Integer stockQuantity,

        @ArraySchema(arraySchema = @Schema(description = "Alerjenler"), schema = @Schema(example = "süt"))
        @Size(max = 10, message = "En fazla 10 alerjen girilebilir")
        List<@NotBlank(message = "Alerjen adı boş olamaz")
             @Size(max = 40, message = "Alerjen adı en fazla 40 karakter olabilir") String> allergens,

        @Schema(description = "Menüde gösterilsin mi?", example = "false")
        Boolean active) {
}
