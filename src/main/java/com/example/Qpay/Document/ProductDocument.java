package com.example.Qpay.Document;

import lombok.*;
import org.springframework.data.annotation.Id;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Document(collection = "products")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ProductDocument {

    @Id
    private String id;

    // Barcode — global unique identifier
    @Indexed(unique = true)
    private String barcode;

    // Brand slug — "dmart", "reliance-smart"
    @Indexed
    private String brandSlug;

    // Brand ka PostgreSQL UUID — join ke liye
    private String brandId;

    private String name;
    private String description;

    // Original MRP — sabka same
    private BigDecimal mrp;

    private String imageUrl;
    private String unit;            // "piece", "kg", "litre"

    private boolean active = true;

    // Har store ka apna price + stock + history
    @Builder.Default
    private List<StorePrice> storePrices = new ArrayList<>();

    @Builder.Default
    private Map<String, Object> customFields = new HashMap<>();

    private OffsetDateTime createdAt;

    @LastModifiedDate
    private OffsetDateTime updatedAt;

    // ─────────────────────────────────────────────────────────────────────────

    /**
     * Har store ka price + stock ek jagah.
     * Nested document — alag collection nahi.
     */
    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class StorePrice {

        // Store ka PostgreSQL UUID
        private String storeId;

        private String storeName;

        // Current discount price — is store mein abhi kya hai
        private BigDecimal currentPrice;

        // Kitna stock bacha hai
        @Builder.Default
        private int stock = 0;

        // Stock 0 hone pe ya manually band karne pe
        @Builder.Default
        private boolean available = true;

        // Price change history — automatically add hoti hai
        @Builder.Default
        private List<PriceHistory> priceHistory = new ArrayList<>();

        // ─────────────────────────────────────────────────────────────────

        /**
         * Ek price change ka record.
         * Admin price change kare → purani entry close, nai entry add.
         */
        @Data
        @NoArgsConstructor
        @AllArgsConstructor
        @Builder
        public static class PriceHistory {
            private BigDecimal price;       // Kya tha
            private String reason;          // "Diwali Sale", "Weekend Offer"
            private OffsetDateTime from;    // Kab se laga
            private OffsetDateTime to;      // Kab khatam hua (null = abhi bhi chal raha)
        }
    }

    // ── Helper Methods ────────────────────────────────────────────────────────

    /**
     * Kisi store ka StorePrice nikalo.
     */
    public StorePrice getStorePriceByStoreId(String storeId) {
        return storePrices.stream()
                .filter(sp -> sp.getStoreId().equals(storeId))
                .findFirst()
                .orElse(null);
    }

    /**
     * Price change karne pe — history update karo.
     */
    public void updatePrice(String storeId, BigDecimal newPrice, String reason) {
        StorePrice storePrice = getStorePriceByStoreId(storeId);
        if (storePrice == null) return;

        // Purani history entry close karo
        storePrice.getPriceHistory().stream()
                .filter(h -> h.getTo() == null)
                .forEach(h -> h.setTo(OffsetDateTime.now()));

        // Nai history entry add karo
        storePrice.getPriceHistory().add(
                StorePrice.PriceHistory.builder()
                        .price(newPrice)
                        .reason(reason)
                        .from(OffsetDateTime.now())
                        .to(null) // abhi chal raha hai
                        .build()
        );

        // Current price update karo
        storePrice.setCurrentPrice(newPrice);
    }

    /**
     * Stock decrement — payment hone pe.
     */
    public void decrementStock(String storeId, int qty) {
        StorePrice storePrice = getStorePriceByStoreId(storeId);
        if (storePrice == null) return;

        int newStock = Math.max(0, storePrice.getStock() - qty);
        storePrice.setStock(newStock);

        // Stock 0 hone pe available = false
        if (newStock == 0) {
            storePrice.setAvailable(false);
        }
    }
}
