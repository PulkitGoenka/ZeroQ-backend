package com.example.Qpay.DTO;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

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
}
