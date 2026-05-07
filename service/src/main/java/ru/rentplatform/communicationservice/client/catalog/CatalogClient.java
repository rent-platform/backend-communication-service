package ru.rentplatform.communicationservice.client.catalog;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import ru.rentplatform.communicationservice.client.catalog.dto.CatalogItemInfo;

import java.util.Map;
import java.util.UUID;

@Component
@RequiredArgsConstructor
@Slf4j
public class CatalogClient {

    private final RestClient catalogServiceRestClient;

    public CatalogItemInfo getItemDealInfo(UUID itemId) {
        try {
            Map<String, Object> response = catalogServiceRestClient.get()
                    .uri("/api/catalog/items/{itemId}/deal-info", itemId)
                    .retrieve()
                    .body(Map.class);

            if (response != null) {
                return CatalogItemInfo.builder()
                        .itemId(itemId)
                        .ownerId(UUID.fromString((String) response.get("ownerId")))
                        .title((String) response.get("title"))
                        .imageUrl(extractImageUrl(response))
                        .build();
            }
        } catch (Exception e) {
            log.warn("Failed to get item info: {}", e.getMessage());
        }
        return null;
    }

    private String extractImageUrl(Map<String, Object> response) {
        try {
            var photos = (java.util.List<Map<String, Object>>) response.get("photos");
            if (photos != null && !photos.isEmpty()) {
                return (String) photos.get(0).get("photoUrl");
            }
        } catch (Exception e) {
        }
        return null;
    }
}
