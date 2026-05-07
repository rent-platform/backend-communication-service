package ru.rentplatform.communicationservice.core.dao.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import ru.rentplatform.communicationservice.core.dao.entity.Message;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;

public interface MessageRepository extends JpaRepository<Message, UUID> {

    @Query("""
        SELECT m FROM Message m
        WHERE m.chat.id = :chatId
        ORDER BY m.createdAt ASC
        """)
    List<Message> findAllByChatId(@Param("chatId") UUID chatId);

    @Query("""
        SELECT m FROM Message m
        WHERE m.chat.id = :chatId
          AND m.createdAt < :before
        ORDER BY m.createdAt DESC
        LIMIT :limit
        """)
    List<Message> findMessagesBefore(
            @Param("chatId") UUID chatId,
            @Param("before") OffsetDateTime before,
            @Param("limit") int limit
    );
}