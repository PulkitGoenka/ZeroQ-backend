package com.example.Qpay.Repository;

import com.example.Qpay.Entity.PaymentHistory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

@Repository
public interface PaymentHistoryRepository extends JpaRepository<PaymentHistory, UUID> {
    Page<PaymentHistory> findByUserIdOrderByPaidAtDesc(UUID userId, Pageable pageable);

    // Order ID ke through check karne ke liye
    Optional<PaymentHistory> findByOrderId(UUID orderId);
}