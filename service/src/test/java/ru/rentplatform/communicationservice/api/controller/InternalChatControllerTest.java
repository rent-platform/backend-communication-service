package ru.rentplatform.communicationservice.api.controller;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import ru.rentplatform.communicationservice.config.JwtBeansConfig;
import ru.rentplatform.communicationservice.config.RsaKeyConfig;
import ru.rentplatform.communicationservice.config.SecurityConfig;
import ru.rentplatform.communicationservice.core.dao.entity.Chat;
import ru.rentplatform.communicationservice.core.service.ChatService;

import java.util.List;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(InternalChatController.class)
@Import({SecurityConfig.class, JwtBeansConfig.class, RsaKeyConfig.class})
@AutoConfigureMockMvc(addFilters = false)
class InternalChatControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private ChatService chatService;

    @Test
    void sendDealStatus_shouldSendSystemMessage() throws Exception {

        UUID itemId = UUID.randomUUID();
        UUID chatId = UUID.randomUUID();
        Chat chat = Chat.builder()
                .id(chatId)
                .itemId(itemId)
                .build();

        when(chatService.findAllByItemId(itemId))
                .thenReturn(List.of(chat));

        mockMvc.perform(post("/api/internal/chats/deal-status")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                        {
                            "itemId": "%s",
                            "dealId": "%s",
                            "status": "CONFIRMED"
                        }
                        """.formatted(itemId, UUID.randomUUID())))
                .andExpect(status().isOk());

        verify(chatService).sendSystemMessage(eq(chatId), any(), eq("DEAL_STATUS"), any());
    }
}
