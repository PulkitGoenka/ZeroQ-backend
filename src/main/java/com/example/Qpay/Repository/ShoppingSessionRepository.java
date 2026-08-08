package com.example.Qpay.Repository;
import com.example.Qpay.Entity.ShoppingSession;
import com.example.Qpay.enums.SessionStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

@Repository
public interface ShoppingSessionRepository extends JpaRepository<ShoppingSession, UUID> {

    @Query("SELECT s FROM ShoppingSession s WHERE s.user.id = :userId AND s.status = :status ORDER BY s.startedAt DESC")
    Optional<ShoppingSession> findLatestByUserAndStatus(
            @Param("userId") UUID userId,
            @Param("status") SessionStatus status);

    Optional<ShoppingSession> findByIdAndStatus(UUID id, SessionStatus status);

    boolean existsByUserIdAndStatus(UUID userId, SessionStatus status);
}
