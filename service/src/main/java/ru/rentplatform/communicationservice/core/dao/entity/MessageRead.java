package ru.rentplatform.communicationservice.core.dao.entity;

import jakarta.persistence.*;
import lombok.*;
import java.time.OffsetDateTime;
import java.util.UUID;

@Entity
@Table(name = "message_reads")
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class MessageRead {

    @EmbeddedId
    private MessageReadId id;

    @ManyToOne(fetch = FetchType.LAZY)
    @MapsId("messageId")
    @JoinColumn(name = "message_id", nullable = false)
    private Message message;

    @Column(name = "read_at", nullable = false)
    private OffsetDateTime readAt;
}
