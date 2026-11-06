package com.restoran.pos.config;

import com.restoran.pos.exception.ApiError;
import io.swagger.v3.core.converter.ModelConverters;
import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.PathItem;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.media.Content;
import io.swagger.v3.oas.models.media.MediaType;
import io.swagger.v3.oas.models.media.Schema;
import io.swagger.v3.oas.models.responses.ApiResponse;
import io.swagger.v3.oas.models.responses.ApiResponses;
import io.swagger.v3.oas.models.security.SecurityRequirement;
import io.swagger.v3.oas.models.security.SecurityScheme;
import org.springdoc.core.customizers.OpenApiCustomizer;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.Set;

/**
 * OpenAPI (Swagger) belgesinin genel bilgileri, HTTP Basic güvenlik şeması ve ortak hata cevapları.
 */
@Configuration
public class OpenApiConfig {

    public static final String BASIC_AUTH_SCHEME = "basicAuth";
    private static final String API_ERROR_REF = "#/components/schemas/ApiError";
    private static final Set<PathItem.HttpMethod> MODIFYING_METHODS = Set.of(
            PathItem.HttpMethod.POST, PathItem.HttpMethod.PUT, PathItem.HttpMethod.PATCH, PathItem.HttpMethod.DELETE);

    @Bean
    public OpenAPI restoranPosOpenApi(@Value("${info.app.version}") String version,
                                      @Value("${info.app.description}") String description) {
        return new OpenAPI()
                .info(new Info()
                        .title("Restoran POS API")
                        .version(version)
                        .description(description + ". Eşzamanlı sipariş güvenliği (satır kilidi + idempotency), "
                                + "rol ve kayıt sahipliği tabanlı erişim, mutfağa anlık bildirim (SSE) ve "
                                + "yalnızca yüklenen belgelere dayanan (RAG) personel asistanı içerir. "
                                + "Sağ üstteki 'Authorize' düğmesiyle örnek kullanıcılardan biriyle giriş yapabilirsiniz."))
                .components(new Components().addSecuritySchemes(BASIC_AUTH_SCHEME, new SecurityScheme()
                        .type(SecurityScheme.Type.HTTP)
                        .scheme("basic")
                        .description("Veritabanındaki kullanıcı adı ve parola ile HTTP Basic kimlik doğrulaması")))
                .addSecurityItem(new SecurityRequirement().addList(BASIC_AUTH_SCHEME));
    }

    /**
     * Her işleme ortak hata modeliyle (ApiError) 400/401/403 ve uygun olduğunda 404/409 cevaplarını ekler.
     */
    @Bean
    public OpenApiCustomizer standardErrorResponsesCustomizer() {
        return openApi -> {
            ModelConverters.getInstance().read(ApiError.class)
                    .forEach((name, schema) -> openApi.getComponents().addSchemas(name, schema));
            if (openApi.getPaths() == null) {
                return;
            }
            openApi.getPaths().forEach((path, pathItem) -> pathItem.readOperationsMap().forEach((method, operation) -> {
                ApiResponses responses = operation.getResponses();
                addIfAbsent(responses, "400", "Geçersiz istek (doğrulama hatası, hatalı JSON veya parametre)");
                addIfAbsent(responses, "401", "Kimlik doğrulaması yok veya başarısız (HTTP Basic)");
                addIfAbsent(responses, "403", "Gerekli yetki yok veya kayıt sahipliği kuralı ihlal edildi");
                if (path.contains("{")) {
                    addIfAbsent(responses, "404", "Kayıt bulunamadı");
                }
                if (MODIFYING_METHODS.contains(method)) {
                    addIfAbsent(responses, "409",
                            "Kaydın mevcut durumuyla çakışma (stok, eşzamanlı güncelleme, benzersiz alan, durum kuralı)");
                }
            }));
        };
    }

    private static void addIfAbsent(ApiResponses responses, String code, String description) {
        if (responses != null && !responses.containsKey(code)) {
            responses.addApiResponse(code, new ApiResponse()
                    .description(description)
                    .content(new Content().addMediaType("application/json",
                            new MediaType().schema(new Schema<>().$ref(API_ERROR_REF)))));
        }
    }
}
