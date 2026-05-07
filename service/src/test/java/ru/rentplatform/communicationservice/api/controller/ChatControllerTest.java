package ru.rentplatform.communicationservice.api.controller;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import ru.rentplatform.communicationservice.api.dto.response.ChatListItemResponse;
import ru.rentplatform.communicationservice.api.dto.response.MessageResponse;
import ru.rentplatform.communicationservice.core.service.ChatService;

import java.util.List;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.MOCK)
@AutoConfigureMockMvc
class ChatControllerTest {

    @Autowired private MockMvc mockMvc;

    @MockitoBean
    private ChatService chatService;

    @Test
    void getChats_shouldReturnList() throws Exception {
        ChatListItemResponse chat = ChatListItemResponse.builder()
                .id(UUID.randomUUID()).itemId(UUID.randomUUID())
                .itemTitle("Test").role("RENTER").unreadCount(0).build();

        when(chatService.getChats(any(), eq("RENTER"))).thenReturn(List.of(chat));

        mockMvc.perform(get("/api/chats?role=RENTER")
                        .with(jwt().jwt(j -> j.claim("sub", "3227ee7b-775f-4743-8781-5563f352f9a7"))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].itemTitle").value("Test"));
    }

    @Test
    void sendMessage_shouldReturnMessage() throws Exception {
        UUID chatId = UUID.randomUUID();
        MessageResponse response = MessageResponse.builder()
                .id(UUID.randomUUID()).chatId(chatId).text("Hello!")
                .messageType("USER").build();

        when(chatService.sendMessage(eq(chatId), any(), eq("Hello!"))).thenReturn(response);

        mockMvc.perform(post("/api/chats/" + chatId + "/messages")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"text\":\"Hello!\"}")
                        .with(jwt().jwt(j -> j.claim("sub", "3227ee7b-775f-4743-8781-5563f352f9a7"))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.text").value("Hello!"));
    }
}
