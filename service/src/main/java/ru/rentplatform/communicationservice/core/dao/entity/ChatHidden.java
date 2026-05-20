package ru.rentplatform.communicationservice.core.dao.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.OffsetDateTime;

@Entity
@Table(name = "chat_hidden")
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ChatHidden {

    @EmbeddedId
    private ChatHiddenId id;

    @ManyToOne(fetch = FetchType.LAZY)
    @MapsId("chatId")
    @JoinColumn(name = "chat_id", nullable = false)
    private Chat chat;

    @Column(name = "hidden_at", nullable = false)
    private OffsetDateTime hiddenAt;
}
