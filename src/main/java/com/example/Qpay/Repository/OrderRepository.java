package com.example.Qpay.Repository;
import com.example.Qpay.Entity.Order;
import com.example.Qpay.enums.OrderStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

@Repository
public interface OrderRepository extends JpaRepository<Order, UUID> {

    Optional<Order> findByExitQrToken(String token);

    Optional<Order> findByCounterQrToken(String token);

    @Query("SELECT o FROM Order o JOIN FETCH o.items WHERE o.id = :id")
    Optional<Order> findByIdWithItems(@Param("id") UUID id);

    Optional<Order> findBySessionIdAndOrderStatus(UUID sessionId, OrderStatus status);
}
