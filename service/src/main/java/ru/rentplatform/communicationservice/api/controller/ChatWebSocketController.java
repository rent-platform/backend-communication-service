package ru.rentplatform.communicationservice.api.controller;

import jakarta.validation.Valid;
import org.springframework.messaging.handler.annotation.DestinationVariable;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.handler.annotation.SendTo;
import org.springframework.stereotype.Controller;
import ru.rentplatform.communicationservice.api.dto.request.SendMessageRequest;
import ru.rentplatform.communicationservice.api.dto.response.MessageResponse;
import ru.rentplatform.communicationservice.core.service.ChatService;

import java.security.Principal;
import java.util.UUID;

@Controller
public class ChatWebSocketController {

    private final ChatService chatService;

    public ChatWebSocketController(ChatService chatService) {
        this.chatService = chatService;
    }

    @MessageMapping("/chat/{chatId}/send")
    @SendTo("/topic/chat/{chatId}")
    public MessageResponse sendMessage(@DestinationVariable UUID chatId,
                                       @Valid SendMessageRequest request,
                                       Principal principal) {
        UUID senderId = UUID.fromString(principal.getName());
        return chatService.sendMessage(chatId, senderId, request.getText());
    }
}
