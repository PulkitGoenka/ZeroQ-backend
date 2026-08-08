package com.example.Qpay.DTO;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import lombok.Data;

public class StoreRequest {

    @Data
    public static class ByPincode {
        // String rakha — UUID object se crash hota tha (leastSigBits/mostSigBits bug)
        // Service layer me UUID.fromString() se convert karenge
        private String brandId;

        @NotBlank(message = "Pincode is required")
        @Pattern(regexp = "\\d{6}", message = "Enter a valid 6-digit pincode")
        private String pincode;
    }

    @Data
    public static class ByState {
        private String brandId; // optional — null hoga to sab brands ke stores aayenge

        @NotBlank(message = "State is required")
        private String state;
    }

    // ── NAYA — District search ────────────────────────────────
    @Data
    public static class ByDistrict {
        private String brandId;

        @NotBlank(message = "District is required")
        private String district;
    }
}