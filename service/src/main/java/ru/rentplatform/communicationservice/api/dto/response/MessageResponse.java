package ru.rentplatform.communicationservice.api.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.OffsetDateTime;
import java.util.Map;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class MessageResponse {

    private UUID id;

    private UUID chatId;

    private UUID senderId;

    private String text;

    private String messageType;

    private Map<String, Object> systemPayload;

    private OffsetDateTime createdAt;
}
