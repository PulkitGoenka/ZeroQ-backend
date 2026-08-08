package com.example.Qpay.Repository;
import com.example.Qpay.Entity.ScanHistory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface ScanHistoryRepository extends JpaRepository<ScanHistory, UUID> {

    List<ScanHistory> findBySessionId(UUID sessionId);

    Page<ScanHistory> findByUserIdOrderByScannedAtDesc(UUID userId, Pageable pageable);

    @Query("SELECT sh FROM ScanHistory sh JOIN FETCH sh.productName WHERE sh.session.id = :sessionId ORDER BY sh.scannedAt DESC")
    List<ScanHistory> findBySessionWithProduct(@Param("sessionId") UUID sessionId);
}
