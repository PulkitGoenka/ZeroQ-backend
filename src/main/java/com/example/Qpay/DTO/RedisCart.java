package com.example.Qpay.DTO;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;
import java.math.BigDecimal;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.UUID;

/**
 * Cart stored in Redis as a JSON string.
 * Key: "cart:{sessionId}"
 * TTL: configured via session.cart-ttl-hours
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class RedisCart implements Serializable {

    private UUID sessionId;
    private UUID userId;
    private UUID storeId;
    private UUID brandId;

    // barcode → CartItem
    @Builder.Default
    private Map<String, CartItem> items = new LinkedHashMap<>();

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class CartItem implements Serializable {
        private String barcode;
        private String productMongoId;
        private String productName;
        private String imageUrl;
        private BigDecimal mrp;
        private BigDecimal discountPrice;
        private BigDecimal discountPct;
        private int quantity;

        public BigDecimal getLineTotal() {
            return discountPrice.multiply(BigDecimal.valueOf(quantity));
        }
    }

    public BigDecimal getSubtotal() {
        return items.values().stream()
                .map(i -> i.getMrp().multiply(BigDecimal.valueOf(i.getQuantity())))
                .reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    public BigDecimal getTotalDiscount() {
        return items.values().stream()
                .map(i -> i.getMrp().subtract(i.getDiscountPrice())
                        .multiply(BigDecimal.valueOf(i.getQuantity())))
                .reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    public BigDecimal getTotalAmount() {
        return items.values().stream()
                .map(RedisCart.CartItem::getLineTotal)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    public int getItemCount() {
        return items.values().stream().mapToInt(CartItem::getQuantity).sum();
    }
}
