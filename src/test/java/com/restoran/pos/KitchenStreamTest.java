package com.restoran.pos;

import com.restoran.pos.dto.product.ProductResponse;
import com.restoran.pos.service.ProductService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import java.util.UUID;

import static com.restoran.pos.TestUsers.KITCHEN;
import static com.restoran.pos.TestUsers.KITCHEN_PASSWORD;
import static com.restoran.pos.TestUsers.WAITER_1;
import static com.restoran.pos.TestUsers.WAITER_PASSWORD;
import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.httpBasic;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.request;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Mutfak ekranı sorgulama (polling) yapmaz: tek bir SSE bağlantısı açar ve yeni sipariş
 * geldiğinde olayı sunucudan alır.
 */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class KitchenStreamTest {

    private final MockMvc mockMvc;
    private final ProductService productService;

    @Autowired
    KitchenStreamTest(MockMvc mockMvc, ProductService productService) {
        this.mockMvc = mockMvc;
        this.productService = productService;
    }

    @Test
    void kitchenScreenIsNotifiedWhenANewOrderArrives() throws Exception {
        MvcResult stream = mockMvc.perform(get("/api/kitchen/stream").with(httpBasic(KITCHEN, KITCHEN_PASSWORD)))
                .andExpect(request().asyncStarted())
                .andReturn();

        ProductResponse lemonade = productService.findAll(null, null).stream()
                .filter(product -> product.name().equals("Limonata"))
                .findFirst()
                .orElseThrow();
        mockMvc.perform(post("/api/orders").with(httpBasic(WAITER_1, WAITER_PASSWORD))
                        .header("Idempotency-Key", UUID.randomUUID().toString())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"tableId\": 3, \"items\": [{\"productId\": %d, \"quantity\": 1, \"note\": \"Buzsuz\"}]}"
                                .formatted(lemonade.id())))
                .andExpect(status().isCreated());

        assertThat(stream.getResponse().getContentType()).startsWith(MediaType.TEXT_EVENT_STREAM_VALUE);
        String events = stream.getResponse().getContentAsString();
        assertThat(events)
                .contains("event:connected")
                .contains("event:order-created")
                .contains("\"tableNumber\":3")
                .contains("Buzsuz")
                .as("mutfak olayı fiyat bilgisi içermemeli").doesNotContain("totalAmount");
    }

    @Test
    void streamIsOnlyForKitchenRole() throws Exception {
        mockMvc.perform(get("/api/kitchen/stream").with(httpBasic(WAITER_1, WAITER_PASSWORD)))
                .andExpect(status().isForbidden());
    }
}
