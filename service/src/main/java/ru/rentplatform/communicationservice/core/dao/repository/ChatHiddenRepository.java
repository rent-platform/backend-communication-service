package ru.rentplatform.communicationservice.core.dao.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import ru.rentplatform.communicationservice.core.dao.entity.ChatHidden;
import ru.rentplatform.communicationservice.core.dao.entity.ChatHiddenId;

import java.time.OffsetDateTime;
import java.util.Optional;
import java.util.UUID;

public interface ChatHiddenRepository extends JpaRepository<ChatHidden, ChatHiddenId> {

    @Modifying
    @Query("DELETE FROM ChatHidden ch WHERE ch.id.chatId = :chatId AND ch.id.userId = :userId")
    void unhide(@Param("chatId") UUID chatId, @Param("userId") UUID userId);

    @Query("""
        SELECT COUNT(ch) > 0 FROM ChatHidden ch
        WHERE ch.id.chatId = :chatId AND ch.id.userId = :userId
        """)
    boolean existsByChatIdAndUserId(@Param("chatId") UUID chatId, @Param("userId") UUID userId);

    @Query("""
        SELECT ch.hiddenAt FROM ChatHidden ch
        WHERE ch.id.chatId = :chatId AND ch.id.userId = :userId
        """)
    Optional<OffsetDateTime> getHiddenAt(@Param("chatId") UUID chatId, @Param("userId") UUID userId);
}