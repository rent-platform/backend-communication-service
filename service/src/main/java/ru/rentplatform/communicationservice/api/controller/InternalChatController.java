package ru.rentplatform.communicationservice.api.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import ru.rentplatform.communicationservice.core.dao.entity.Chat;
import ru.rentplatform.communicationservice.core.service.ChatService;

import java.util.List;
import java.util.Map;
import java.util.UUID;

@RestController
@RequestMapping("/api/internal/chats")
@RequiredArgsConstructor
@Tag(name = "Internal Chat API")
public class InternalChatController {

    private final ChatService chatService;

    @PostMapping("/deal-status")
    @Operation(summary = "[Internal] Системное сообщение о смене статуса сделки")
    public void sendDealStatus(@RequestBody Map<String, Object> payload) {
        UUID itemId = UUID.fromString((String) payload.get("itemId"));
        UUID dealId = UUID.fromString((String) payload.get("dealId"));
        String status = (String) payload.get("status");

        List<Chat> chats = chatService.findAllByItemId(itemId);
        for (Chat chat : chats) {
            chatService.sendSystemMessage(
                    chat.getId(),
                    "Deal status changed to " + status,
                    "DEAL_STATUS",
                    Map.of("dealId", dealId.toString(), "status", status)
            );
        }
    }
}
