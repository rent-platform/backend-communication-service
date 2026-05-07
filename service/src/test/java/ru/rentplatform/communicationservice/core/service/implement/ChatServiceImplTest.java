package ru.rentplatform.communicationservice.core.service.implement;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import ru.rentplatform.communicationservice.api.dto.response.ChatListItemResponse;
import ru.rentplatform.communicationservice.api.dto.response.MessageResponse;
import ru.rentplatform.communicationservice.api.exception.ChatAccessDeniedException;
import ru.rentplatform.communicationservice.api.exception.ChatNotFoundException;
import ru.rentplatform.communicationservice.client.catalog.CatalogClient;
import ru.rentplatform.communicationservice.client.catalog.dto.CatalogItemInfo;
import ru.rentplatform.communicationservice.client.user.UserClient;
import ru.rentplatform.communicationservice.core.dao.entity.Chat;
import ru.rentplatform.communicationservice.core.dao.entity.Message;
import ru.rentplatform.communicationservice.core.dao.entity.MessageType;
import ru.rentplatform.communicationservice.core.dao.repository.ChatRepository;
import ru.rentplatform.communicationservice.core.dao.repository.MessageReadRepository;
import ru.rentplatform.communicationservice.core.dao.repository.MessageRepository;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ChatServiceImplTest {

    @Mock private ChatRepository chatRepository;
    @Mock private MessageRepository messageRepository;
    @Mock private MessageReadRepository messageReadRepository;
    @Mock private CatalogClient catalogClient;
    @Mock private UserClient userClient;
    @Mock private SimpMessagingTemplate messagingTemplate;

    @InjectMocks
    private ChatServiceImpl chatService;

    private UUID itemId;
    private UUID ownerId;
    private UUID renterId;
    private UUID chatId;
    private Chat chat;

    @BeforeEach
    void setUp() {
        itemId = UUID.randomUUID();
        ownerId = UUID.randomUUID();
        renterId = UUID.randomUUID();
        chatId = UUID.randomUUID();

        chat = Chat.builder()
                .id(chatId)
                .itemId(itemId)
                .ownerId(ownerId)
                .renterId(renterId)
                .createdAt(OffsetDateTime.now())
                .updatedAt(OffsetDateTime.now())
                .build();
    }

    @Test
    void createOrGetChat_shouldCreateNewChat_whenNotExists() {
        CatalogItemInfo itemInfo = CatalogItemInfo.builder()
                .itemId(itemId).ownerId(ownerId).title("Test item").build();

        when(catalogClient.getItemDealInfo(itemId)).thenReturn(itemInfo);
        when(chatRepository.findByItemIdAndOwnerIdAndRenterId(itemId, ownerId, renterId))
                .thenReturn(Optional.empty());
        when(chatRepository.save(any(Chat.class))).thenReturn(chat);
        when(messageRepository.findAllByChatId(chatId)).thenReturn(List.of());
        when(userClient.getPublicUser(ownerId)).thenReturn(null);

        ChatListItemResponse result = chatService.createOrGetChat(itemId, renterId);

        assertNotNull(result);
        assertEquals(itemId, result.getItemId());
        // Роль RENTER потому что renterId — это арендатор, который создаёт чат
        assertEquals("RENTER", result.getRole());
        verify(chatRepository).save(any(Chat.class));
    }

    @Test
    void createOrGetChat_shouldThrow_whenChattingWithSelf() {
        // Мокаем catalogClient чтобы вернуть itemInfo с ownerId = renterId (сам с собой)
        CatalogItemInfo itemInfo = CatalogItemInfo.builder()
                .itemId(itemId).ownerId(renterId).title("Test").build();

        when(catalogClient.getItemDealInfo(itemId)).thenReturn(itemInfo);

        assertThrows(IllegalArgumentException.class, () ->
                chatService.createOrGetChat(itemId, renterId));
    }

    @Test
    void sendMessage_shouldSaveAndSendViaWebSocket() {

        String text = "Hello!";
        Message message = Message.builder()
                .id(UUID.randomUUID())
                .chat(chat)
                .senderId(renterId)
                .text(text)
                .messageType(MessageType.USER)
                .createdAt(OffsetDateTime.now())
                .build();

        when(chatRepository.findById(chatId)).thenReturn(Optional.of(chat));
        when(messageRepository.save(any(Message.class))).thenReturn(message);

        MessageResponse result = chatService.sendMessage(chatId, renterId, text);

        assertNotNull(result);
        assertEquals(text, result.getText());
        assertEquals("USER", result.getMessageType());
        verify(messagingTemplate).convertAndSend(eq("/topic/chat/" + chatId), any(MessageResponse.class));
    }

    @Test
    void sendMessage_shouldThrow_whenNotParticipant() {

        UUID strangerId = UUID.randomUUID();
        when(chatRepository.findById(chatId)).thenReturn(Optional.of(chat));

        assertThrows(ChatAccessDeniedException.class, () ->
                chatService.sendMessage(chatId, strangerId, "Hi"));
    }

    @Test
    void sendMessage_shouldThrow_whenChatNotFound() {

        when(chatRepository.findById(chatId)).thenReturn(Optional.empty());

        assertThrows(ChatNotFoundException.class, () ->
                chatService.sendMessage(chatId, renterId, "Hi"));
    }

    @Test
    void getMessages_shouldReturnMessages_whenParticipant() {

        Message msg = Message.builder()
                .id(UUID.randomUUID()).chat(chat).senderId(renterId)
                .text("Hi").messageType(MessageType.USER).createdAt(OffsetDateTime.now()).build();

        when(chatRepository
                .findById(chatId))
                .thenReturn(Optional.of(chat));
        when(messageRepository
                .findAllByChatId(chatId))
                .thenReturn(List.of(msg));

        List<MessageResponse> result = chatService.getMessages(chatId, renterId, null, 50);

        assertEquals(1, result.size());
        assertEquals("Hi", result.get(0).getText());
    }

    @Test
    void getMessages_shouldThrow_whenNotParticipant() {

        when(chatRepository.findById(chatId)).thenReturn(Optional.of(chat));

        assertThrows(ChatAccessDeniedException.class, () ->
                chatService.getMessages(chatId, UUID.randomUUID(), null, 50));
    }

    @Test
    void getChats_shouldReturnOwnerChats() {

        when(chatRepository
                .findAllByOwnerId(ownerId))
                .thenReturn(List.of(chat));
        when(messageRepository
                .findAllByChatId(chatId))
                .thenReturn(List.of());
        when(userClient
                .getPublicUser(renterId))
                .thenReturn(null);
        when(catalogClient
                .getItemDealInfo(itemId))
                .thenReturn(
                CatalogItemInfo.builder()
                        .itemId(itemId)
                        .ownerId(ownerId)
                        .title("Test")
                        .build());

        List<ChatListItemResponse> result = chatService.getChats(ownerId, "OWNER");

        assertEquals(1, result.size());
        assertEquals("OWNER", result.get(0).getRole());
    }

    @Test
    void sendSystemMessage_shouldSaveAndNotify() {

        when(chatRepository
                .findById(chatId))
                .thenReturn(Optional.of(chat));
        when(messageRepository
                .save(any(Message.class)))
                .thenAnswer(i ->
                        i.getArgument(0));

        chatService.sendSystemMessage(chatId, "Status changed", "DEAL_STATUS",
                java.util.Map.of("status", "ACTIVE"));

        verify(messageRepository)
                .save(any(Message.class));
        verify(messagingTemplate)
                .convertAndSend(eq("/topic/chat/" + chatId), any(MessageResponse.class));
    }
}
