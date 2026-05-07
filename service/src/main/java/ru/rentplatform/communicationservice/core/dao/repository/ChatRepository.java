package ru.rentplatform.communicationservice.core.dao.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import ru.rentplatform.communicationservice.core.dao.entity.Chat;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface ChatRepository extends JpaRepository<Chat, UUID> {

    @Query("""
        SELECT c FROM Chat c
        WHERE c.itemId = :itemId
          AND c.ownerId = :ownerId
          AND c.renterId = :renterId
        """)
    Optional<Chat> findByItemIdAndOwnerIdAndRenterId(
            @Param("itemId") UUID itemId,
            @Param("ownerId") UUID ownerId,
            @Param("renterId") UUID renterId
    );

    @Query("""
        SELECT c FROM Chat c
        WHERE c.ownerId = :userId
        ORDER BY c.updatedAt DESC
        """)
    List<Chat> findAllByOwnerId(@Param("userId") UUID userId);

    @Query("""
        SELECT c FROM Chat c
        WHERE c.renterId = :userId
        ORDER BY c.updatedAt DESC
        """)
    List<Chat> findAllByRenterId(@Param("userId") UUID userId);

    @Query("""
        SELECT c FROM Chat c
        WHERE c.itemId = :itemId
        """)
    List<Chat> findAllByItemId(@Param("itemId") UUID itemId);
}
