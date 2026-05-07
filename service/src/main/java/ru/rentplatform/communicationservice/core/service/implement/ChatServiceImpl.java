package ru.rentplatform.communicationservice.core.service.implement;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import ru.rentplatform.communicationservice.api.dto.response.ChatListItemResponse;
import ru.rentplatform.communicationservice.api.dto.response.MessageResponse;
import ru.rentplatform.communicationservice.api.exception.ChatAccessDeniedException;
import ru.rentplatform.communicationservice.api.exception.ChatNotFoundException;
import ru.rentplatform.communicationservice.client.catalog.CatalogClient;
import ru.rentplatform.communicationservice.client.user.UserClient;
import ru.rentplatform.communicationservice.core.dao.entity.*;
import ru.rentplatform.communicationservice.core.dao.repository.ChatRepository;
import ru.rentplatform.communicationservice.core.dao.repository.MessageReadRepository;
import ru.rentplatform.communicationservice.core.dao.repository.MessageRepository;
import ru.rentplatform.communicationservice.core.service.ChatService;

import java.time.OffsetDateTime;
import java.util.*;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class ChatServiceImpl implements ChatService {

    private final ChatRepository chatRepository;
    private final MessageRepository messageRepository;
    private final MessageReadRepository messageReadRepository;
    private final CatalogClient catalogClient;
    private final UserClient userClient;
    private final SimpMessagingTemplate messagingTemplate;

    @Override
    @Transactional
    public ChatListItemResponse createOrGetChat(UUID itemId, UUID userId) {
        var itemInfo = catalogClient.getItemDealInfo(itemId);
        UUID ownerId = itemInfo.getOwnerId();

        if (ownerId.equals(userId)) {
            throw new IllegalArgumentException("Cannot create chat with yourself");
        }

        Chat chat = chatRepository.findByItemIdAndOwnerIdAndRenterId(itemId, ownerId, userId)
                .orElseGet(() -> {
                    Chat newChat = Chat.builder()
                            .itemId(itemId)
                            .ownerId(ownerId)
                            .renterId(userId)
                            .createdAt(OffsetDateTime.now())
                            .updatedAt(OffsetDateTime.now())
                            .build();
                    return chatRepository.save(newChat);
                });

        return buildChatListItemResponse(chat, userId);
    }

    @Override
    @Transactional(readOnly = true)
    public List<ChatListItemResponse> getChats(UUID userId, String role) {
        List<Chat> chats = "OWNER".equals(role)
                ? chatRepository.findAllByOwnerId(userId)
                : chatRepository.findAllByRenterId(userId);

        return chats.stream()
                .map(chat -> buildChatListItemResponse(chat, userId))
                .sorted((a, b) -> {
                    if (a.getLastMessageTime() == null && b.getLastMessageTime() == null) return 0;
                    if (a.getLastMessageTime() == null) return 1;
                    if (b.getLastMessageTime() == null) return -1;
                    return b.getLastMessageTime().compareTo(a.getLastMessageTime());
                })
                .collect(Collectors.toList());
    }

    @Override
    @Transactional(readOnly = true)
    public List<MessageResponse> getMessages(UUID chatId, UUID userId, OffsetDateTime before, int limit) {
        Chat chat = chatRepository.findById(chatId)
                .orElseThrow(() -> new ChatNotFoundException("Chat not found"));

        if (!chat.getOwnerId().equals(userId) && !chat.getRenterId().equals(userId)) {
            throw new ChatAccessDeniedException("Access denied");
        }

        List<Message> messages;
        if (before != null) {
            messages = messageRepository.findMessagesBefore(chatId, before, limit);
        } else {
            messages = messageRepository.findAllByChatId(chatId);
        }

        return messages.stream()
                .map(this::toMessageResponse)
                .collect(Collectors.toList());
    }

    @Override
    @Transactional
    public MessageResponse sendMessage(UUID chatId, UUID senderId, String text) {
        Chat chat = chatRepository.findById(chatId)
                .orElseThrow(() -> new ChatNotFoundException("Chat not found"));

        if (!chat.getOwnerId().equals(senderId) && !chat.getRenterId().equals(senderId)) {
            throw new ChatAccessDeniedException("Access denied");
        }

        Message message = Message.builder()
                .chat(chat)
                .senderId(senderId)
                .text(text)
                .messageType(MessageType.USER)
                .createdAt(OffsetDateTime.now())
                .build();

        messageRepository.save(message);

        chat.setUpdatedAt(OffsetDateTime.now());
        chatRepository.save(chat);

        MessageResponse response = toMessageResponse(message);

        messagingTemplate.convertAndSend("/topic/chat/" + chatId, response);

        return response;
    }

    @Override
    @Transactional(readOnly = true)
    public Chat findByItemIdAndOwnerIdAndRenterId(UUID itemId, UUID ownerId, UUID renterId) {
        return chatRepository.findByItemIdAndOwnerIdAndRenterId(itemId, ownerId, renterId).orElse(null);
    }

    @Override
    @Transactional
    public void markRead(UUID chatId, UUID userId) {
        List<Message> unreadMessages = messageRepository.findAllByChatId(chatId).stream()
                .filter(m -> !m.getSenderId().equals(userId))
                .toList();

        OffsetDateTime now = OffsetDateTime.now();
        for (Message message : unreadMessages) {
            boolean alreadyRead = messageReadRepository.existsByMessageIdAndUserId(message.getId(), userId);
            if (!alreadyRead) {
                MessageRead read = MessageRead.builder()
                        .id(MessageReadId.builder()
                                .messageId(message.getId())
                                .userId(userId)
                                .build())
                        .message(message)
                        .readAt(now)
                        .build();
                messageReadRepository.save(read);
            }
        }
    }

    @Override
    @Transactional
    public void sendSystemMessage(UUID chatId, String text, String messageType, Map<String, Object> payload) {
        Chat chat = chatRepository.findById(chatId)
                .orElseThrow(() -> new ChatNotFoundException("Chat not found"));

        Message message = Message.builder()
                .chat(chat)
                .senderId(UUID.randomUUID()) // system
                .text(text)
                .messageType(MessageType.valueOf(messageType))
                .systemPayload(payload)
                .createdAt(OffsetDateTime.now())
                .build();

        messageRepository.save(message);

        chat.setUpdatedAt(OffsetDateTime.now());
        chatRepository.save(chat);

        MessageResponse response = toMessageResponse(message);
        messagingTemplate.convertAndSend("/topic/chat/" + chatId, response);
    }

    @Override
    @Transactional(readOnly = true)
    public List<Chat> findAllByItemId(UUID itemId) {
        return chatRepository.findAllByItemId(itemId);
    }

    private ChatListItemResponse buildChatListItemResponse(Chat chat, UUID userId) {
        boolean isOwner = chat.getOwnerId().equals(userId);
        UUID otherUserId = isOwner ? chat.getRenterId() : chat.getOwnerId();

        var otherUser = userClient.getPublicUser(otherUserId);
        var itemInfo = catalogClient.getItemDealInfo(chat.getItemId());

        List<Message> messages = messageRepository.findAllByChatId(chat.getId());
        Message lastMessage = messages.isEmpty() ? null : messages.get(messages.size() - 1);

        long unreadCount = messages.stream()
                .filter(m -> !m.getSenderId().equals(userId))
                .filter(m -> !messageReadRepository.existsByMessageIdAndUserId(m.getId(), userId))
                .count();

        return ChatListItemResponse.builder()
                .id(chat.getId())
                .itemId(chat.getItemId())
                .itemTitle(itemInfo.getTitle())
                .imageUrl(itemInfo.getImageUrl())
                .otherUserId(otherUserId)
                .otherUserNickname(otherUser != null ? otherUser.getNickname() : null)
                .otherUserAvatarUrl(otherUser != null ? otherUser.getAvatarUrl() : null)
                .lastMessage(lastMessage != null ? lastMessage.getText() : null)
                .lastMessageTime(lastMessage != null ? lastMessage.getCreatedAt() : chat.getCreatedAt())
                .unreadCount(unreadCount)
                .dealStatus(null)
                .role(isOwner ? "OWNER" : "RENTER")
                .build();
    }

    private MessageResponse toMessageResponse(Message message) {
        return MessageResponse.builder()
                .id(message.getId())
                .chatId(message.getChat().getId())
                .senderId(message.getSenderId())
                .text(message.getText())
                .messageType(message.getMessageType().name())
                .systemPayload(message.getSystemPayload())
                .createdAt(message.getCreatedAt())
                .build();
    }
}
