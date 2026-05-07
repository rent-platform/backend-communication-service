package ru.rentplatform.communicationservice.client.catalog.dto;

import lombok.Builder;
import lombok.Data;
import java.util.UUID;

@Data
@Builder
public class CatalogItemInfo {

    private UUID itemId;

    private UUID ownerId;

    private String title;

    private String imageUrl;
}