package com.example.Qpay.DTO;

import jakarta.validation.constraints.*;
import lombok.Data;

import java.util.UUID;

public class CartRequest {

    @Data
    public static class StartSession {
        @NotNull(message = "Store ID is required")
        private UUID storeId;
    }

    @Data
    public static class ScanBarcode {
        @NotBlank(message = "Barcode is required")
        private String barcode;
    }

    @Data
    public static class UpdateQuantity {
        @NotBlank(message = "Barcode is required")
        private String barcode;

        @NotNull(message = "Quantity is required")
        @Min(value = 0, message = "Quantity cannot be negative")
        @Max(value = 99, message = "Maximum 99 items per product")
        private Integer quantity;
    }

    @Data
    public static class RemoveItem {
        @NotBlank(message = "Barcode is required")
        private String barcode;
    }
}
