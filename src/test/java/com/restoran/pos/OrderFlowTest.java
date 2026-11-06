package com.restoran.pos;

import com.jayway.jsonpath.JsonPath;
import com.restoran.pos.dto.product.ProductResponse;
import com.restoran.pos.service.ProductService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import java.util.UUID;

import static com.restoran.pos.TestUsers.CASHIER;
import static com.restoran.pos.TestUsers.CASHIER_PASSWORD;
import static com.restoran.pos.TestUsers.KITCHEN;
import static com.restoran.pos.TestUsers.KITCHEN_PASSWORD;
import static com.restoran.pos.TestUsers.TABLE_1;
import static com.restoran.pos.TestUsers.TABLE_1_SECRET;
import static com.restoran.pos.TestUsers.TABLE_2;
import static com.restoran.pos.TestUsers.TABLE_2_SECRET;
import static com.restoran.pos.TestUsers.WAITER_1;
import static com.restoran.pos.TestUsers.WAITER_2;
import static com.restoran.pos.TestUsers.WAITER_PASSWORD;
import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.hasItem;
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
 * Masa 1 üzerinden uçtan uca akış: QR müşteri siparişi, idempotency, kayıt sahipliği, mutfak, kasa.
 */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class OrderFlowTest {

    private final MockMvc mockMvc;
    private final ProductService productService;

    @Autowired
    OrderFlowTest(MockMvc mockMvc, ProductService productService) {
        this.mockMvc = mockMvc;
        this.productService = productService;
    }

    @Test
    void customerOrderKitchenAndCashierFlow() throws Exception {
        ProductResponse ayran = findProduct("Ayran");
        String orderKey = UUID.randomUUID().toString();
        String orderBody = "{\"items\": [{\"productId\": %d, \"quantity\": 2, \"note\": \"Soğuk olsun\"}]}"
                .formatted(ayran.id());

        // 1) QR müşteri kendi masasına sipariş verir → 201
        String created = mockMvc.perform(post("/api/orders").with(httpBasic(TABLE_1, TABLE_1_SECRET))
                        .header("Idempotency-Key", orderKey)
                        .contentType(MediaType.APPLICATION_JSON).content(orderBody))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.status").value("CONFIRMED"))
                .andExpect(jsonPath("$.channel").value("QR_MENU"))
                .andExpect(jsonPath("$.tableNumber").value(1))
                .andExpect(jsonPath("$.totalAmount").value(100.00))
                .andReturn().getResponse().getContentAsString();
        long orderId = ((Number) JsonPath.read(created, "$.id")).longValue();
        long billId = ((Number) JsonPath.read(created, "$.billId")).longValue();

        // 2) Aynı Idempotency-Key ile tekrar (çift tıklama) → aynı sipariş, 200, stok tekrar düşmez
        mockMvc.perform(post("/api/orders").with(httpBasic(TABLE_1, TABLE_1_SECRET))
                        .header("Idempotency-Key", orderKey)
                        .contentType(MediaType.APPLICATION_JSON).content(orderBody))
                .andExpect(status().isOk())
                .andExpect(header().string("Idempotent-Replayed", "true"))
                .andExpect(jsonPath("$.id").value(orderId));
        assertThat(findProduct("Ayran").stockQuantity()).isEqualTo(ayran.stockQuantity() - 2);

        // 3) Kayıt sahipliği: başka masanın müşterisi ve başka garson erişemez, masanın garsonu erişir
        mockMvc.perform(get("/api/orders/{id}", orderId).with(httpBasic(TABLE_1, TABLE_1_SECRET)))
                .andExpect(status().isOk());
        mockMvc.perform(get("/api/orders/{id}", orderId).with(httpBasic(TABLE_2, TABLE_2_SECRET)))
                .andExpect(status().isForbidden());
        mockMvc.perform(get("/api/orders/{id}", orderId).with(httpBasic(WAITER_2, WAITER_PASSWORD)))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.message", containsString("size atanmamış")));
        mockMvc.perform(get("/api/orders/{id}", orderId).with(httpBasic(WAITER_1, WAITER_PASSWORD)))
                .andExpect(status().isOk());

        // 4) Müşteri verdiği siparişi düzenleyemez; garson düzenler (PATCH yalnızca notu değiştirir)
        mockMvc.perform(put("/api/orders/{id}", orderId).with(httpBasic(TABLE_1, TABLE_1_SECRET))
                        .contentType(MediaType.APPLICATION_JSON).content(orderBody))
                .andExpect(status().isForbidden());
        mockMvc.perform(patch("/api/orders/{id}", orderId).with(httpBasic(WAITER_1, WAITER_PASSWORD))
                        .contentType(MediaType.APPLICATION_JSON).content("{\"note\": \"Pipetli\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.note").value("Pipetli"))
                .andExpect(jsonPath("$.items[0].quantity").value(2));

        // 5) Mutfak görünümü fiyat içermez; durum Onaylandı → Hazırlanıyor → Hazır
        mockMvc.perform(get("/api/kitchen/orders/{id}", orderId).with(httpBasic(KITCHEN, KITCHEN_PASSWORD)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.tableNumber").value(1))
                .andExpect(jsonPath("$.totalAmount").doesNotExist())
                .andExpect(jsonPath("$.items[0].unitPrice").doesNotExist());
        mockMvc.perform(patch("/api/kitchen/orders/{id}", orderId).with(httpBasic(KITCHEN, KITCHEN_PASSWORD))
                        .contentType(MediaType.APPLICATION_JSON).content("{\"status\": \"PREPARING\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("PREPARING"));

        // Hazırlanan sipariş iptal edilemez
        mockMvc.perform(delete("/api/orders/{id}", orderId).with(httpBasic(WAITER_1, WAITER_PASSWORD)))
                .andExpect(status().isConflict());

        mockMvc.perform(patch("/api/kitchen/orders/{id}", orderId).with(httpBasic(KITCHEN, KITCHEN_PASSWORD))
                        .contentType(MediaType.APPLICATION_JSON).content("{\"status\": \"READY\"}"))
                .andExpect(status().isOk());
        mockMvc.perform(patch("/api/kitchen/orders/{id}", orderId).with(httpBasic(KITCHEN, KITCHEN_PASSWORD))
                        .contentType(MediaType.APPLICATION_JSON).content("{\"status\": \"PREPARING\"}"))
                .andExpect(status().isConflict());

        // 6) Müşteri hesabını görür; kasiyer açık adisyonları görür, ödeme alır (idempotent) ve kapatır
        mockMvc.perform(get("/api/bills/{id}", billId).with(httpBasic(TABLE_1, TABLE_1_SECRET)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.remainingAmount").value(100.00));
        mockMvc.perform(get("/api/bills").param("status", "OPEN").with(httpBasic(CASHIER, CASHIER_PASSWORD)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[*].id", hasItem((int) billId)));

        String paymentKey = UUID.randomUUID().toString();
        String paymentBody = "{\"amount\": 100.00, \"method\": \"CARD\"}";
        mockMvc.perform(post("/api/bills/{id}/payments", billId).with(httpBasic(CASHIER, CASHIER_PASSWORD))
                        .header("Idempotency-Key", paymentKey)
                        .contentType(MediaType.APPLICATION_JSON).content(paymentBody))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.remainingAmount").value(0));
        mockMvc.perform(post("/api/bills/{id}/payments", billId).with(httpBasic(CASHIER, CASHIER_PASSWORD))
                        .header("Idempotency-Key", paymentKey)
                        .contentType(MediaType.APPLICATION_JSON).content(paymentBody))
                .andExpect(status().isOk())
                .andExpect(header().string("Idempotent-Replayed", "true"))
                .andExpect(jsonPath("$.paidAmount").value(100.00));

        mockMvc.perform(patch("/api/bills/{id}", billId).with(httpBasic(CASHIER, CASHIER_PASSWORD))
                        .contentType(MediaType.APPLICATION_JSON).content("{\"status\": \"CLOSED\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("CLOSED"));

        // 7) Kapanan adisyon, masaya yeni oturuşta gelen müşteriye gösterilmez
        mockMvc.perform(get("/api/bills/{id}", billId).with(httpBasic(TABLE_1, TABLE_1_SECRET)))
                .andExpect(status().isForbidden());
    }

    @Test
    void insufficientStockReturns409AndNothingIsSaved() throws Exception {
        ProductResponse soldOut = findProduct("Künefe");
        mockMvc.perform(post("/api/orders").with(httpBasic(WAITER_1, WAITER_PASSWORD))
                        .header("Idempotency-Key", UUID.randomUUID().toString())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"tableId\": 2, \"items\": [{\"productId\": %d, \"quantity\": 1}]}".formatted(soldOut.id())))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.details[0]", containsString("Künefe")));
        assertThat(findProduct("Künefe").stockQuantity()).isZero();
    }

    @Test
    void orderRequestValidationAndOwnershipRules() throws Exception {
        ProductResponse coffee = findProduct("Türk Kahvesi");
        String body = "{\"items\": [{\"productId\": %d, \"quantity\": 1}]}".formatted(coffee.id());

        // Idempotency-Key başlığı zorunlu → 400
        mockMvc.perform(post("/api/orders").with(httpBasic(TABLE_2, TABLE_2_SECRET))
                        .contentType(MediaType.APPLICATION_JSON).content(body))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message", containsString("Idempotency-Key")));

        // Müşteri başka masa adına sipariş veremez → 403
        mockMvc.perform(post("/api/orders").with(httpBasic(TABLE_2, TABLE_2_SECRET))
                        .header("Idempotency-Key", UUID.randomUUID().toString())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"tableId\": 1, \"items\": [{\"productId\": %d, \"quantity\": 1}]}".formatted(coffee.id())))
                .andExpect(status().isForbidden());

        // Garson kendisine atanmamış masaya sipariş açamaz → 403 (masa 4, garson2'nin)
        mockMvc.perform(post("/api/orders").with(httpBasic(WAITER_1, WAITER_PASSWORD))
                        .header("Idempotency-Key", UUID.randomUUID().toString())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"tableId\": 4, \"items\": [{\"productId\": %d, \"quantity\": 1}]}".formatted(coffee.id())))
                .andExpect(status().isForbidden());

        // Mutfak sipariş fiyatlarını, adisyonları ve ürün stoklarını göremez
        mockMvc.perform(get("/api/orders").with(httpBasic(KITCHEN, KITCHEN_PASSWORD))).andExpect(status().isForbidden());
        mockMvc.perform(get("/api/bills").with(httpBasic(KITCHEN, KITCHEN_PASSWORD))).andExpect(status().isForbidden());
    }

    private ProductResponse findProduct(String name) {
        return productService.findAll(null, null).stream()
                .filter(product -> product.name().equals(name))
                .findFirst()
                .orElseThrow();
    }
}
