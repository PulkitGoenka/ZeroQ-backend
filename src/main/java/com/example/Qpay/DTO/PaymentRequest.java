package com.example.Qpay.DTO;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.util.UUID;

public class PaymentRequest {

    @Data
    public static class InitiateOnline {
        // Initiated from active session — no extra fields needed;
        // session context comes from JWT principal
    }

    @Data
    public static class InitiateCash {
        // Same — session derived from JWT
    }

    @Data
    public static class VerifyExitQr {
        @NotBlank(message = "QR token is required")
        private String qrToken;
    }

    @Data
    public static class ConfirmCashPayment {
        @NotBlank(message = "QR token is required")
        private String qrToken;
    }

    // ✅ Razorpay verification payload from Mobile App
    @Data
    public static class VerifyRazorpay {
        @NotNull(message = "Order ID is required")
        private UUID orderId;

        @NotBlank(message = "Razorpay payment ID is required")
        private String razorpayPaymentId;

        @NotBlank(message = "Razorpay order ID is required")
        private String razorpayOrderId;

        @NotBlank(message = "Razorpay signature is required")
        private String razorpaySignature;
    }
}