package ru.rentplatform.communicationservice.client.user;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import ru.rentplatform.communicationservice.client.user.dto.UserPublicInfo;

import java.util.Map;
import java.util.UUID;

@Component
@RequiredArgsConstructor
@Slf4j
public class UserClient {

    private final RestClient userServiceRestClient;

    public UserPublicInfo getPublicUser(UUID userId) {
        try {
            Map<String, Object> response = userServiceRestClient.get()
                    .uri("/api/users/{userId}/public", userId)
                    .retrieve()
                    .body(Map.class);

            if (response != null) {
                return UserPublicInfo.builder()
                        .id(UUID.fromString((String) response.get("id")))
                        .nickname((String) response.get("nickname"))
                        .avatarUrl((String) response.get("avatarUrl"))
                        .build();
            }
        } catch (Exception e) {
            log.warn("Failed to get user info: {}", e.getMessage());
        }
        return null;
    }
}