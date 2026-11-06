package com.restoran.pos;

import com.jayway.jsonpath.JsonPath;
import com.restoran.pos.repository.UserAccountRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import static com.restoran.pos.TestUsers.ADMIN;
import static com.restoran.pos.TestUsers.ADMIN_PASSWORD;
import static com.restoran.pos.TestUsers.KITCHEN;
import static com.restoran.pos.TestUsers.KITCHEN_PASSWORD;
import static com.restoran.pos.TestUsers.WAITER_1;
import static com.restoran.pos.TestUsers.WAITER_PASSWORD;
import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.hasItem;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.httpBasic;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Argon2id, Actuator erişim kuralları, OpenAPI/Swagger, masa QR hesabı, genişletilebilir rol yapısı ve
 * elle yazılan isteklerdeki (ör. Postman) yaygın hatalara verilen açıklayıcı mesajlar.
 */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class SecurityAndPlatformTest {

    private final MockMvc mockMvc;
    private final UserAccountRepository userAccountRepository;

    @Autowired
    SecurityAndPlatformTest(MockMvc mockMvc, UserAccountRepository userAccountRepository) {
        this.mockMvc = mockMvc;
        this.userAccountRepository = userAccountRepository;
    }

    @Test
    void passwordsAreStoredAsArgon2idHashes() {
        String hash = userAccountRepository.findByUsername(WAITER_1).orElseThrow().getPasswordHash();
        assertThat(hash).startsWith("$argon2id$v=19$m=16384,t=2,p=1$").doesNotContain(WAITER_PASSWORD);
    }

    @Test
    void healthAndInfoArePublicButManagementEndpointsAreProtected() throws Exception {
        mockMvc.perform(get("/actuator/health"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("UP"));
        mockMvc.perform(get("/actuator/info"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.app.name").value("Restoran POS API"))
                .andExpect(jsonPath("$.app.version").value("1.0.0"))
                .andExpect(jsonPath("$.app.description", containsString("Sipariş")));

        mockMvc.perform(get("/actuator/metrics")).andExpect(status().isUnauthorized());
        mockMvc.perform(get("/actuator/metrics").with(httpBasic(WAITER_1, WAITER_PASSWORD))).andExpect(status().isForbidden());
        mockMvc.perform(get("/actuator/metrics").with(httpBasic(ADMIN, ADMIN_PASSWORD))).andExpect(status().isOk());
    }

    @Test
    void openApiDocumentAndSwaggerUiAreAvailable() throws Exception {
        mockMvc.perform(get("/v3/api-docs"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.paths['/api/products/{id}'].patch").exists())
                .andExpect(jsonPath("$.paths['/api/products/{id}'].put").exists())
                .andExpect(jsonPath("$.paths['/api/products/{id}'].delete").exists())
                .andExpect(jsonPath("$.components.schemas.ApiError").exists())
                .andExpect(jsonPath("$.components.securitySchemes.basicAuth.scheme").value("basic"));
        mockMvc.perform(get("/swagger-ui/index.html"))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("Swagger UI")));
    }

    @Test
    void newTableGetsItsOwnQrCustomerAccount() throws Exception {
        String response = mockMvc.perform(post("/api/tables").with(httpBasic(ADMIN, ADMIN_PASSWORD))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"tableNumber\": 2001, \"capacity\": 2, \"location\": \"Teras\"}"))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.table.tableNumber").value(2001))
                .andExpect(jsonPath("$.qrCredentials.username").value("masa-2001"))
                .andReturn().getResponse().getContentAsString();
        String secret = JsonPath.read(response, "$.qrCredentials.secret");

        // QR anahtarıyla giriş yapan müşteri yalnızca kendi masasını görür
        mockMvc.perform(get("/api/tables").with(httpBasic("masa-2001", secret)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(1))
                .andExpect(jsonPath("$[0].tableNumber").value(2001));
    }

    @Test
    void newRolesCanBeDefinedWithoutCodeChanges() throws Exception {
        mockMvc.perform(post("/api/roles").with(httpBasic(ADMIN, ADMIN_PASSWORD))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"name\": \"PAKET_SERVIS\", \"description\": \"Paket servis platformu\", "
                                + "\"permissions\": [\"MENU_READ\"]}"))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.systemRole").value(false))
                .andExpect(jsonPath("$.permissions[0]").value("MENU_READ"));

        mockMvc.perform(get("/api/roles").with(httpBasic(ADMIN, ADMIN_PASSWORD)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[*].name", hasItem("PAKET_SERVIS")));

        // Sistem rolleri değiştirilemez; tanımsız yetki 400 döner; yetkisiz rol 403 alır
        mockMvc.perform(post("/api/roles").with(httpBasic(ADMIN, ADMIN_PASSWORD))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"name\": \"HATALI_ROL\", \"description\": \"x\", \"permissions\": [\"OLMAYAN_YETKI\"]}"))
                .andExpect(status().isBadRequest());
        mockMvc.perform(get("/api/roles").with(httpBasic(KITCHEN, KITCHEN_PASSWORD)))
                .andExpect(status().isForbidden());
    }

    @Test
    void commonManualRequestMistakesGetClearTurkishMessages() throws Exception {
        // Postman'de elle yazılan isteklerde sık görülen hatalar; her biri ortak hata modeli ve Türkçe açıklamayla döner.
        mockMvc.perform(get("/api/products/").with(httpBasic(ADMIN, ADMIN_PASSWORD)))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.message").value(
                        "İstenen adres bulunamadı: /api/products/ (adresin sonundaki '/' işaretini kaldırıp tekrar deneyin)"));

        mockMvc.perform(post("/api/products").with(httpBasic(ADMIN, ADMIN_PASSWORD))
                        .contentType(MediaType.TEXT_PLAIN)
                        .content("{\"name\": \"X\"}"))
                .andExpect(status().isUnsupportedMediaType())
                .andExpect(jsonPath("$.message", containsString("application/json")));

        mockMvc.perform(post("/api/products").with(httpBasic(ADMIN, ADMIN_PASSWORD))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"name\": \"X\", \"category\": \"Y\", \"price\": \"abc\", \"stockQuantity\": 1}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("'price' alanı için geçersiz değer: 'abc'. Sayısal bir değer bekleniyor."));

        mockMvc.perform(patch("/api/kitchen/orders/1").with(httpBasic(KITCHEN, KITCHEN_PASSWORD))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"status\": \"HAZIR\"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message", containsString("'status' alanı için geçersiz değer: 'HAZIR'.")))
                .andExpect(jsonPath("$.message", containsString("PREPARING")))
                .andExpect(jsonPath("$.details").doesNotExist());

        mockMvc.perform(post("/api/orders").with(httpBasic(WAITER_1, WAITER_PASSWORD))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"tableId\": 1, \"items\": [{\"productId\": 10, \"quantity\": 1}]}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("'Idempotency-Key' başlığı zorunludur."));
    }
}
