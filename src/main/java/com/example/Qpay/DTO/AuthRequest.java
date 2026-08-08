package com.example.Qpay.DTO;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.Data;

public class AuthRequest {

    @Data
    public static class SendOtp {
        @NotBlank(message = "Phone number is required")
        @Pattern(regexp = "^[6-9]\\d{9}$", message = "Enter a valid 10-digit Indian mobile number")
        private String phone;

        // Optional: provided only for new-user registration
        private String name;
    }

    @Data
    public static class VerifyOtp {
        @NotBlank(message = "Phone number is required")
        @Pattern(regexp = "^[6-9]\\d{9}$", message = "Enter a valid 10-digit Indian mobile number")
        private String phone;

        @NotBlank(message = "OTP is required")
        @Size(min = 6, max = 6, message = "OTP must be 6 digits")
        @Pattern(regexp = "\\d{6}", message = "OTP must be numeric")
        private String otp;
    }

    @Data
    public static class ResendOtp {
        @NotBlank(message = "Phone number is required")
        @Pattern(regexp = "^[6-9]\\d{9}$", message = "Enter a valid 10-digit Indian mobile number")
        private String phone;
    }

    @Data
    public static class RefreshToken {
        @NotBlank(message = "Refresh token is required")
        private String refreshToken;
    }
}
