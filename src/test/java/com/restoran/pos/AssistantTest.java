package com.restoran.pos;

import org.junit.jupiter.api.MethodOrderer;
import org.junit.jupiter.api.Order;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestMethodOrder;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import static com.restoran.pos.TestUsers.ADMIN;
import static com.restoran.pos.TestUsers.ADMIN_PASSWORD;
import static com.restoran.pos.TestUsers.KITCHEN;
import static com.restoran.pos.TestUsers.KITCHEN_PASSWORD;
import static com.restoran.pos.TestUsers.TABLE_1;
import static com.restoran.pos.TestUsers.TABLE_1_SECRET;
import static com.restoran.pos.TestUsers.WAITER_1;
import static com.restoran.pos.TestUsers.WAITER_PASSWORD;
import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.hasItem;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.httpBasic;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Asistan yalnızca yüklenen belgelere dayanır. Testlerde Gemini anahtarı tanımlı olmadığından
 * güvenli anahtar kelime modu (yapay zekasız birebir alıntı) kullanılır.
 */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
class AssistantTest {

    private final MockMvc mockMvc;
    private final ConfigurableApplicationContext context;

    @Autowired
    AssistantTest(MockMvc mockMvc, ConfigurableApplicationContext context) {
        this.mockMvc = mockMvc;
        this.context = context;
    }

    @Test
    @Order(1)
    void ragAssistantBeanIsCreatedLazilyOnFirstQuestion() throws Exception {
        assertThat(context.getBeanFactory().containsSingleton("ragMenuAssistant"))
                .as("@Lazy bean açılışta oluşturulmamalı").isFalse();

        mockMvc.perform(post("/api/assistant/questions").with(httpBasic(WAITER_1, WAITER_PASSWORD))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"question\": \"Fıstıklı baklavada hangi alerjenler var?\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.grounded").value(true))
                .andExpect(jsonPath("$.mode").value("KEYWORD_SEARCH"))
                .andExpect(jsonPath("$.answer", containsString("Antep fıstığı")))
                .andExpect(jsonPath("$.sources", hasItem("Alerjen Bilgileri")));

        assertThat(context.getBeanFactory().containsSingleton("ragMenuAssistant")).isTrue();
    }

    @Test
    @Order(2)
    void questionOutsideDocumentsIsNotAnsweredWithGuesses() throws Exception {
        mockMvc.perform(post("/api/assistant/questions").with(httpBasic(KITCHEN, KITCHEN_PASSWORD))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"question\": \"Yarın İstanbul'da hava nasıl olacak?\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.grounded").value(false))
                .andExpect(jsonPath("$.sources").isEmpty());
    }

    @Test
    @Order(3)
    void newlyUploadedDocumentIsUsedAsReference() throws Exception {
        mockMvc.perform(post("/api/knowledge-documents").with(httpBasic(ADMIN, ADMIN_PASSWORD))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"title\": \"Haftanın Menüsü\", \"documentType\": \"MENU\", "
                                + "\"content\": \"Kayseri Mantısı: hamur, dana kıyma ve sarımsaklı yoğurt. "
                                + "Alerjenler: gluten, süt, yumurta.\"}"))
                .andExpect(status().isCreated());

        mockMvc.perform(post("/api/assistant/questions").with(httpBasic(WAITER_1, WAITER_PASSWORD))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"question\": \"Mantının alerjenleri neler?\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.grounded").value(true))
                .andExpect(jsonPath("$.sources", hasItem("Haftanın Menüsü")));
    }

    @Test
    @Order(4)
    void customersCannotUseStaffAssistantAndEmptyQuestionIsRejected() throws Exception {
        mockMvc.perform(post("/api/assistant/questions").with(httpBasic(TABLE_1, TABLE_1_SECRET))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"question\": \"Sütlaçta süt var mı?\"}"))
                .andExpect(status().isForbidden());
        mockMvc.perform(post("/api/assistant/questions").with(httpBasic(WAITER_1, WAITER_PASSWORD))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"question\": \"  \"}"))
                .andExpect(status().isBadRequest());
    }
}
