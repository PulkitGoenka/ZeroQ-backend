package com.example.Qpay.Controller;

import com.example.Qpay.Entity.*;
import com.example.Qpay.Repository.*;
import com.example.Qpay.enums.SessionStatus;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/admin")
@CrossOrigin(origins = "*")
public class AdminOperationsController {

    @Autowired private OrderRepository orderRepository;
    @Autowired private ShoppingSessionRepository shoppingSessionRepository;
    @Autowired private ScanHistoryRepository scanHistoryRepository;
    @Autowired private UserRepository userRepository;
    @Autowired private PaymentHistoryRepository paymentHistoryRepository;

    // ══════════════════════════════════════════════════════════
    //  ORDERS (view + delete only)
    // ══════════════════════════════════════════════════════════

    @GetMapping("/orders")
    public List<Order> getAllOrders() {
        return orderRepository.findAll();
    }

    @DeleteMapping("/orders/{id}")
    public ResponseEntity<?> deleteOrder(@PathVariable UUID id) {
        orderRepository.deleteById(id);
        return ResponseEntity.ok(Map.of("success", true));
    }

    // ══════════════════════════════════════════════════════════
    //  SHOPPING SESSIONS
    //  Returned as flat Maps — avoids LazyInitializationException on
    //  the `user`/`store` relations, and matches what the dashboard
    //  expects (userId, storeId as plain strings, status as string).
    // ══════════════════════════════════════════════════════════

    @GetMapping("/sessions")
    public List<Map<String, Object>> getAllSessions() {
        return shoppingSessionRepository.findAll().stream().map(this::sessionToMap).toList();
    }

    private Map<String, Object> sessionToMap(ShoppingSession s) {
        Map<String, Object> m = new HashMap<>();
        m.put("id", s.getId());
        m.put("userId", s.getUser() != null ? s.getUser().getId() : null);
        m.put("storeId", s.getStore() != null ? s.getStore().getId() : null);
        m.put("status", s.getStatus() != null ? s.getStatus().name() : null);
        m.put("startedAt", s.getStartedAt());
        m.put("endedAt", s.getEndedAt());
        m.put("redisKey", s.getRedisKey());
        return m;
    }

    @PutMapping("/sessions/{id}/status")
    public Map<String, Object> updateSessionStatus(@PathVariable UUID id, @RequestBody Map<String, String> body) {
        ShoppingSession session = shoppingSessionRepository.findById(id)
                .orElseThrow(() -> new NoSuchElementException("Session not found"));
        session.setStatus(SessionStatus.valueOf(body.get("status"))); // e.g. "ENDED"
        if (session.getStatus() != SessionStatus.ACTIVE && session.getEndedAt() == null) {
            session.setEndedAt(java.time.OffsetDateTime.now());
        }
        return sessionToMap(shoppingSessionRepository.save(session));
    }

    @DeleteMapping("/sessions/{id}")
    public ResponseEntity<?> deleteSession(@PathVariable UUID id) {
        shoppingSessionRepository.deleteById(id);
        return ResponseEntity.ok(Map.of("success", true));
    }

    // ══════════════════════════════════════════════════════════
    //  SCAN HISTORY (view + delete)
    // ══════════════════════════════════════════════════════════

    @GetMapping("/scan-history")
    public List<ScanHistory> getAllScanHistory() {
        return scanHistoryRepository.findAll();
    }

    @DeleteMapping("/scan-history/{id}")
    public ResponseEntity<?> deleteScanHistory(@PathVariable UUID id) {
        scanHistoryRepository.deleteById(id);
        return ResponseEntity.ok(Map.of("success", true));
    }

    // ══════════════════════════════════════════════════════════
    //  USERS
    // ══════════════════════════════════════════════════════════

    @GetMapping("/users")
    public List<User> getAllUsers() {
        return userRepository.findAll();
    }

    @PutMapping("/users/{id}/status")
    public User updateUserStatus(@PathVariable UUID id, @RequestBody Map<String, Boolean> body) {
        User user = userRepository.findById(id)
                .orElseThrow(() -> new NoSuchElementException("User not found"));
        user.setIsActive(body.get("isActive"));
        return userRepository.save(user);
    }

    @DeleteMapping("/users/{id}")
    public ResponseEntity<?> deleteUser(@PathVariable UUID id) {
        userRepository.deleteById(id);
        return ResponseEntity.ok(Map.of("success", true));
    }

    // ══════════════════════════════════════════════════════════
    //  PAYMENT HISTORY (view only)
    // ══════════════════════════════════════════════════════════

    @GetMapping("/payment-history")
    public List<PaymentHistory> getAllPaymentHistory() {
        return paymentHistoryRepository.findAll();
    }
}