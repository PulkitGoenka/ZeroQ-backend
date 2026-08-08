package com.example.Qpay.Entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.OffsetDateTime;
import java.util.UUID;

@Entity
@Table(name = "scan_history")
@Data
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class ScanHistory {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "session_id", nullable = false)
    private ShoppingSession session;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @Column(name = "product_mongo_id", nullable = false, length = 100)
    private String productMongoId;

    @Column(nullable = false, length = 50)
    private String barcode;

    @Column(name = "product_name", nullable = false, length = 300)
    private String productName;

    @Column(name = "scanned_price", precision = 10, scale = 2)
    private BigDecimal scannedPrice;

    private String imageUrl;

    @Enumerated(EnumType.STRING)
    @Builder.Default
    private ScanStatus scanStatus = ScanStatus.IN_CART;

    @CreationTimestamp
    @Column(name = "scanned_at", updatable = false)
    private OffsetDateTime scannedAt;

    public enum ScanStatus {
        IN_CART,
        REMOVED
    }
}