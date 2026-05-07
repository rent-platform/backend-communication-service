package ru.rentplatform.communicationservice.api.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.OffsetDateTime;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ChatListItemResponse {

    private UUID id;

    private UUID itemId;

    private String itemTitle;

    private String imageUrl;

    private UUID otherUserId;

    private String otherUserNickname;

    private String otherUserAvatarUrl;

    private String lastMessage;

    private OffsetDateTime lastMessageTime;

    private long unreadCount;

    private String dealStatus;

    private String role;
}
