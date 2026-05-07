package ru.rentplatform.communicationservice.core.dao.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import ru.rentplatform.communicationservice.core.dao.entity.MessageRead;
import ru.rentplatform.communicationservice.core.dao.entity.MessageReadId;

import java.util.UUID;

public interface MessageReadRepository extends JpaRepository<MessageRead, MessageReadId> {

    @Query("""
        SELECT COUNT(mr) > 0 FROM MessageRead mr
        WHERE mr.id.messageId = :messageId
          AND mr.id.userId = :userId
        """)
    boolean existsByMessageIdAndUserId(
            @Param("messageId") UUID messageId,
            @Param("userId") UUID userId
    );
}
