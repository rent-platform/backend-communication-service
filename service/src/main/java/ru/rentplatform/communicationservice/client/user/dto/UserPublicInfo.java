package ru.rentplatform.communicationservice.client.user.dto;

import lombok.Builder;
import lombok.Data;
import java.util.UUID;

@Data
@Builder
public class UserPublicInfo {

    private UUID id;

    private String nickname;

    private String avatarUrl;
}
