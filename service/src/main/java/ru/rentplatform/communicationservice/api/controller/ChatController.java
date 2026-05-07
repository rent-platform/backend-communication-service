package ru.rentplatform.communicationservice.api.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.*;
import ru.rentplatform.communicationservice.api.dto.request.SendMessageRequest;
import ru.rentplatform.communicationservice.api.dto.response.ChatListItemResponse;
import ru.rentplatform.communicationservice.api.dto.response.MessageResponse;
import ru.rentplatform.communicationservice.core.service.ChatService;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;

import static ru.rentplatform.communicationservice.api.ApiPaths.CHATS;

@RestController
@RequestMapping(CHATS)
@RequiredArgsConstructor
@SecurityRequirement(name = "bearerAuth")
@Tag(name = "Чаты", description = "Управление чатами")
public class ChatController {

    private final ChatService chatService;

    @PostMapping
    @Operation(summary = "Создать или получить чат",
            description = "Создаёт чат по объявлению. Если чат уже существует — возвращает его. Только для арендатора")
    public ChatListItemResponse createChat(@AuthenticationPrincipal Jwt jwt,
                                           @RequestParam UUID itemId) {
        UUID userId = UUID.fromString(jwt.getSubject());
        return chatService.createOrGetChat(itemId, userId);
    }

    @GetMapping
    @Operation(summary = "Список чатов",
            description = "Возвращает чаты текущего пользователя. role=OWNER — я сдаю, role=RENTER — я арендую")
    public List<ChatListItemResponse> getChats(@AuthenticationPrincipal Jwt jwt,
                                               @RequestParam(defaultValue = "RENTER") String role) {
        UUID userId = UUID.fromString(jwt.getSubject());
        return chatService.getChats(userId, role);
    }

    @GetMapping("/{chatId}/messages")
    @Operation(summary = "История сообщений",
            description = "Возвращает сообщения чата с пагинацией (курсорная). limit — количество, before — дата, до которой грузить")
    public List<MessageResponse> getMessages(@AuthenticationPrincipal Jwt jwt,
                                             @PathVariable UUID chatId,
                                             @RequestParam(required = false) OffsetDateTime before,
                                             @RequestParam(defaultValue = "50") int limit) {
        UUID userId = UUID.fromString(jwt.getSubject());
        return chatService.getMessages(chatId, userId, before, limit);
    }

    @PostMapping("/{chatId}/read")
    @Operation(summary = "Отметить прочитанным",
            description = "Помечает все сообщения в чате как прочитанные текущим пользователем")
    public void markRead(@AuthenticationPrincipal Jwt jwt,
                         @PathVariable UUID chatId) {
        UUID userId = UUID.fromString(jwt.getSubject());
        chatService.markRead(chatId, userId);
    }

    @PostMapping("/{chatId}/messages")
    @Operation(summary = "Отправить сообщение",
            description = "Отправляет сообщение в чат и рассылает через WebSocket")
    public MessageResponse sendMessage(@AuthenticationPrincipal Jwt jwt,
                                       @PathVariable UUID chatId,
                                       @Valid @RequestBody SendMessageRequest request) {
        UUID senderId = UUID.fromString(jwt.getSubject());
        return chatService.sendMessage(chatId, senderId, request.getText());
    }
}
