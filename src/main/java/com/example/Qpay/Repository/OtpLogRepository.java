package com.example.Qpay.Repository;
import com.example.Qpay.Entity.OtpLog;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface OtpLogRepository extends JpaRepository<OtpLog, UUID> {



    @Query("SELECT o FROM OtpLog o WHERE o.phone = :phone AND o.isUsed = false AND o.expiresAt > :now ORDER BY o.createdAt DESC LIMIT 1")
    Optional<OtpLog> findLatestActiveByPhone(@Param("phone") String phone, @Param("now") OffsetDateTime now);

    @Query("SELECT COUNT(o) FROM OtpLog o WHERE o.phone = :phone AND o.createdAt > :since")
    long countRecentByPhone(@Param("phone") String phone, @Param("since") OffsetDateTime since);

    List<OtpLog> findByPhoneAndIsUsedFalseOrderByCreatedAtDesc(String phone);
}
