package com.example.Qpay.DTO;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.example.Qpay.enums.PaymentMethod;
import com.example.Qpay.enums.SessionStatus;
import lombok.Builder;
import lombok.Data;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;

@JsonInclude(JsonInclude.Include.NON_NULL)
public class ApiResponse {

    // ── Generic Wrapper ───────────────────────────────────────────────────────
    @Data @Builder
    public static class Success<T> {
        private boolean success = true;
        private String message;
        private T data;
    }

    // ── Auth ──────────────────────────────────────────────────────────────────
    @Data @Builder
    public static class OtpSent {
        private String phone;
        private boolean newUser;
        private int expirySeconds;
        private int cooldownSeconds;
    }

    @Data @Builder
    public static class AuthToken {
        private String accessToken;
        private String refreshToken;
        private long expiresInMs;
        private UserInfo user;
    }

    @Data @Builder
    public static class UserInfo {
        private UUID id;
        private String phone;
        private String name;
        private boolean newUser;
    }

    // ── Brand ─────────────────────────────────────────────────────────────────
    @Data @Builder
    public static class BrandDto {
        private UUID id;
        private String name;
        private String slug;
        private String logoUrl;     // App mein brand logo dikhega
    }

    // ── Store ─────────────────────────────────────────────────────────────────
    @Data @Builder
    public static class StoreDto {
        private UUID id;
        private UUID brandId;
        private String brandName;
        private String name;
        private String address;
        private String district;
        private String city;
        private String state;
        private String pincode;
    }

    // ── Cart ──────────────────────────────────────────────────────────────────
    @Data @Builder
    public static class CartItemDto {
        private String barcode;
        private String productName;
        private String imageUrl;
        private BigDecimal mrp;
        private BigDecimal discountPrice;
        private BigDecimal discountPct;
        private int quantity;
        private BigDecimal lineTotal;
    }

    @Data @Builder
    public static class CartDto {
        private UUID sessionId;
        private UUID storeId;
        private String storeName;
        private List<CartItemDto> items;
        private BigDecimal subtotal;
        private BigDecimal totalDiscount;
        private BigDecimal totalAmount;
        private int itemCount;
    }

    // ── Scan History ──────────────────────────────────────────────────────────
    // ✅ imageUrl add kiya — history mein product image dikhegi
    @Data @Builder
    public static class ScanHistoryItemDto {
        private String barcode;
        private String productName;
        private String imageUrl;        // ✅ NEW
        private BigDecimal discountPrice;
        private OffsetDateTime scannedAt;
    }

    // ── Session ───────────────────────────────────────────────────────────────
    @Data @Builder
    public static class SessionDto {
        private UUID sessionId;
        private SessionStatus status;
        private UUID storeId;
        private String storeName;
        private OffsetDateTime startedAt;
    }

    // ── Payment ───────────────────────────────────────────────────────────────
    @Data @Builder
    public static class PaymentInitiated {
        private UUID orderId;
        private PaymentMethod method;
        private BigDecimal totalAmount;
        private String qrToken;         // Raw token
        private String qrImageBase64;   // PNG QR image as base64 — app mein dikhao
        private int qrExpirySeconds;    // Kitne second mein expire hoga
    }

    @Data @Builder
    public static class BillDto {
        private UUID orderId;
        private String billRef;
        private PaymentMethod paymentMethod;
        private List<BillItemDto> items;
        private BigDecimal subtotal;
        private BigDecimal totalDiscount;
        private BigDecimal totalAmount;
        private OffsetDateTime paidAt;
        private String storeName;
        private String brandName;
    }

    @Data @Builder
    public static class BillItemDto {
        private String barcode;
        private String productName;
        private BigDecimal mrp;
        private BigDecimal discountPrice;
        private int quantity;
        private BigDecimal lineTotal;
    }

    // ── Payment History ───────────────────────────────────────────────────────
    @Data @Builder
    public static class PaymentHistoryDto {
        private UUID id;
        private UUID orderId;
        private String billRef;
        private PaymentMethod paymentMethod;
        private BigDecimal totalAmount;
        private int itemCount;
        private String storeName;
        private String brandName;
        private OffsetDateTime paidAt;
    }

    // ── Counter Cart (Billing counter ke liye) ────────────────────────────────
    @Data @Builder
    public static class CounterCartDto {
        private UUID orderId;
        private UUID userId;
        private String userName;
        private String userPhone;
        private List<CartItemDto> items;
        private BigDecimal totalAmount;
        private String storeName;
    }
}