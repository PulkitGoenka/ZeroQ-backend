package com.example.Qpay.Entity;

import com.example.Qpay.enums.SessionStatus;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;

import java.time.OffsetDateTime;
import java.util.UUID;

@Entity
@Table(name = "shopping_sessions")
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class ShoppingSession {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "store_id", nullable = false)
    private Stores store;

    // ✅ columnDefinition bug fix — VARCHAR use karo
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    @Builder.Default
    private SessionStatus status = SessionStatus.ACTIVE;

    // ✅ Auto set — @PrePersist ki zaroorat nahi
    @CreationTimestamp
    @Column(name = "started_at", updatable = false)
    private OffsetDateTime startedAt;

    @Column(name = "ended_at")
    private OffsetDateTime endedAt;

    // ✅ Redis cart link ke liye zaroori
    @Column(name = "redis_key", length = 200)
    private String redisKey;
}