package ru.rentplatform.communicationservice.core.service;

import ru.rentplatform.communicationservice.api.dto.response.ChatListItemResponse;
import ru.rentplatform.communicationservice.api.dto.response.MessageResponse;
import ru.rentplatform.communicationservice.core.dao.entity.Chat;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.Map;
import java.util.UUID;

public interface ChatService {

    ChatListItemResponse createOrGetChat(UUID itemId, UUID userId);

    List<ChatListItemResponse> getChats(UUID userId, String role);

    List<MessageResponse> getMessages(UUID chatId, UUID userId, OffsetDateTime before, int limit);

    MessageResponse sendMessage(UUID chatId, UUID senderId, String text);

    Chat findByItemIdAndOwnerIdAndRenterId(UUID itemId, UUID ownerId, UUID renterId);

    void markRead(UUID chatId, UUID userId);

    void sendSystemMessage(UUID chatId, String text, String messageType, Map<String, Object> payload);

    List<Chat> findAllByItemId(UUID itemId);

    void hideChat(UUID chatId, UUID userId);
}
