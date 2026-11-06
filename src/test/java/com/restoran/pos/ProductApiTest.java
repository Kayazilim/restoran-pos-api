package com.restoran.pos;

import com.jayway.jsonpath.JsonPath;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import java.util.UUID;

import static com.restoran.pos.TestUsers.ADMIN;
import static com.restoran.pos.TestUsers.ADMIN_PASSWORD;
import static com.restoran.pos.TestUsers.DISABLED_WAITER;
import static com.restoran.pos.TestUsers.KITCHEN;
import static com.restoran.pos.TestUsers.KITCHEN_PASSWORD;
import static com.restoran.pos.TestUsers.WAITER_1;
import static com.restoran.pos.TestUsers.WAITER_PASSWORD;
import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.everyItem;
import static org.hamcrest.Matchers.hasItem;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.not;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.httpBasic;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Ürün kaynağı üzerinden CRUD, HTTP durum kodları (200/201/204/400/401/403/404) ve PATCH kuralları.
 */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class ProductApiTest {

    private final MockMvc mockMvc;

    @Autowired
    ProductApiTest(MockMvc mockMvc) {
        this.mockMvc = mockMvc;
    }

    @Test
    void crudLifecycleWithFullAndPartialUpdate() throws Exception {
        String name = "Test Tatlısı " + UUID.randomUUID();

        // POST: gövdedeki id yok sayılır, yeni kimlik üretilir
        String created = mockMvc.perform(post("/api/products").with(httpBasic(ADMIN, ADMIN_PASSWORD))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"id": 1, "name": "%s", "description": "Deneme", "category": "Tatlılar",
                                 "price": 199.90, "stockQuantity": 5, "allergens": ["Süt", "gluten"]}
                                """.formatted(name)))
                .andExpect(status().isCreated())
                .andExpect(header().exists(HttpHeaders.LOCATION))
                .andExpect(jsonPath("$.name").value(name))
                .andExpect(jsonPath("$.allergens[0]").value("süt"))
                .andExpect(jsonPath("$.active").value(true))
                .andReturn().getResponse().getContentAsString();
        long id = ((Number) JsonPath.read(created, "$.id")).longValue();
        assertThat(id).isNotEqualTo(1L);

        // Mevcut 1 numaralı ürün POST'taki id yüzünden değişmemiş olmalı
        mockMvc.perform(get("/api/products/1").with(httpBasic(ADMIN, ADMIN_PASSWORD)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.name").value(not(name)));

        // GET (tek kayıt)
        mockMvc.perform(get("/api/products/{id}", id).with(httpBasic(ADMIN, ADMIN_PASSWORD)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(id))
                .andExpect(jsonPath("$.stockQuantity").value(5));

        // PUT: tam güncelleme, gönderilmeyen açıklama boşaltılır
        mockMvc.perform(put("/api/products/{id}", id).with(httpBasic(ADMIN, ADMIN_PASSWORD))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"name": "%s", "category": "Tatlılar", "price": 210.00, "stockQuantity": 8,
                                 "allergens": [], "active": true}
                                """.formatted(name)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.price").value(210.00))
                .andExpect(jsonPath("$.stockQuantity").value(8))
                .andExpect(jsonPath("$.description").doesNotExist())
                .andExpect(jsonPath("$.allergens").isEmpty());

        // PATCH: yalnızca fiyat değişir, diğer alanlar korunur
        mockMvc.perform(patch("/api/products/{id}", id).with(httpBasic(ADMIN, ADMIN_PASSWORD))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"price\": 99.90}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.price").value(99.90))
                .andExpect(jsonPath("$.name").value(name))
                .andExpect(jsonPath("$.stockQuantity").value(8))
                .andExpect(jsonPath("$.category").value("Tatlılar"));

        // PATCH ile birincil anahtar değiştirilemez
        mockMvc.perform(patch("/api/products/{id}", id).with(httpBasic(ADMIN, ADMIN_PASSWORD))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"id\": 424242, \"price\": 5.00}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value(400))
                .andExpect(jsonPath("$.message", containsString("Birincil anahtar")));

        // DELETE ve sonrasında 404
        mockMvc.perform(delete("/api/products/{id}", id).with(httpBasic(ADMIN, ADMIN_PASSWORD)))
                .andExpect(status().isNoContent());
        mockMvc.perform(get("/api/products/{id}", id).with(httpBasic(ADMIN, ADMIN_PASSWORD)))
                .andExpect(status().isNotFound());
    }

    @Test
    void listCanBeFilteredByCategoryWithJpqlParameter() throws Exception {
        mockMvc.perform(get("/api/products").param("category", "Tatlılar").with(httpBasic(ADMIN, ADMIN_PASSWORD)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[*].category", everyItem(is("Tatlılar"))))
                .andExpect(jsonPath("$[*].name", hasItem("Fıstıklı Baklava")));
    }

    @Test
    void invalidBodyReturns400WithFieldDetails() throws Exception {
        mockMvc.perform(post("/api/products").with(httpBasic(ADMIN, ADMIN_PASSWORD))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"name\": \"\", \"category\": \"Tatlılar\", \"price\": -5, \"stockQuantity\": 1}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value(400))
                .andExpect(jsonPath("$.timestamp").exists())
                .andExpect(jsonPath("$.details[*]", hasItem(containsString("price"))))
                .andExpect(jsonPath("$.details[*]", hasItem(containsString("name"))));

        mockMvc.perform(post("/api/products").with(httpBasic(ADMIN, ADMIN_PASSWORD))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{bozuk json"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message", containsString("JSON")));
    }

    @Test
    void unknownProductReturns404WithCustomMessage() throws Exception {
        mockMvc.perform(get("/api/products/999999").with(httpBasic(ADMIN, ADMIN_PASSWORD)))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.status").value(404))
                .andExpect(jsonPath("$.error").value("Not Found"))
                .andExpect(jsonPath("$.message").value("Ürün bulunamadı: id=999999"))
                .andExpect(jsonPath("$.path").value("/api/products/999999"));
    }

    @Test
    void missingOrWrongCredentialsReturn401() throws Exception {
        mockMvc.perform(get("/api/products"))
                .andExpect(status().isUnauthorized())
                .andExpect(header().exists(HttpHeaders.WWW_AUTHENTICATE))
                .andExpect(jsonPath("$.status").value(401));

        mockMvc.perform(get("/api/products").with(httpBasic(ADMIN, "yanlis-parola")))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.message").value("Kullanıcı adı veya parola hatalı."));

        // Aktiflik bilgisi veritabanından okunur: pasif hesap doğru parolayla da giremez
        mockMvc.perform(get("/api/menu").with(httpBasic(DISABLED_WAITER, WAITER_PASSWORD)))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.message", containsString("pasif")));
    }

    @Test
    void authenticatedUserWithoutPermissionGets403() throws Exception {
        // Mutfak stok bilgisine erişemez
        mockMvc.perform(get("/api/products").with(httpBasic(KITCHEN, KITCHEN_PASSWORD)))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.status").value(403));

        // Garson ürün ekleyemez / güncelleyemez (PATCH de WRITE yetkisi ister) / silemez
        mockMvc.perform(post("/api/products").with(httpBasic(WAITER_1, WAITER_PASSWORD))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"name\": \"X\", \"category\": \"Y\", \"price\": 1, \"stockQuantity\": 1}"))
                .andExpect(status().isForbidden());
        mockMvc.perform(patch("/api/products/1").with(httpBasic(WAITER_1, WAITER_PASSWORD))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"price\": 1}"))
                .andExpect(status().isForbidden());
        mockMvc.perform(delete("/api/products/1").with(httpBasic(WAITER_1, WAITER_PASSWORD)))
                .andExpect(status().isForbidden());

        // Menü ise tüm rollere açık
        mockMvc.perform(get("/api/menu").with(httpBasic(KITCHEN, KITCHEN_PASSWORD)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].stockQuantity").doesNotExist());
    }
}
