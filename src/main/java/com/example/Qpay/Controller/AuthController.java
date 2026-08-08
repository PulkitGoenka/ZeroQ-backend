package com.example.Qpay.Controller;

import com.example.Qpay.DTO.AuthRequest;
import com.example.Qpay.DTO.ApiResponse;
import com.example.Qpay.Security.UserPrincipal;
import com.example.Qpay.Service.AuthService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/auth")
@RequiredArgsConstructor
public class AuthController {

    private final AuthService authService;

    /**
     * POST /api/v1/auth/send-otp
     * Works for both new users (with name) and existing users (phone only).
     */
    @PostMapping("/send-otp")
    public ResponseEntity<ApiResponse.Success<ApiResponse.OtpSent>> sendOtp(
            @Valid @RequestBody AuthRequest.SendOtp request) {
        ApiResponse.OtpSent result = authService.sendOtp(request);
        return ResponseEntity.ok(ApiResponse.Success.<ApiResponse.OtpSent>builder()
                        .success(true)
                .message("OTP sent successfully")
                .data(result)
                .build());
    }

    /**
     * POST /api/v1/auth/resend-otp
     * Resend OTP with cooldown and hourly rate limiting.
     */
    @PostMapping("/resend-otp")
    public ResponseEntity<ApiResponse.Success<ApiResponse.OtpSent>> resendOtp(
            @Valid @RequestBody AuthRequest.ResendOtp request) {
        ApiResponse.OtpSent result = authService.resendOtp(request);
        return ResponseEntity.ok(ApiResponse.Success.<ApiResponse.OtpSent>builder()
                        .success(true)
                .message("OTP resent successfully")
                .data(result)
                .build());
    }
    /**
     * POST /api/v1/auth/verify-otp
     * Verify OTP and receive JWT access + refresh tokens.
     */
    @PostMapping("/verify-otp")
    public ResponseEntity<ApiResponse.Success<ApiResponse.AuthToken>> verifyOtp(
            @Valid @RequestBody AuthRequest.VerifyOtp request) {
        ApiResponse.AuthToken result = authService.verifyOtp(request);
        return ResponseEntity.ok(ApiResponse.Success.<ApiResponse.AuthToken>builder()
                        .success(true)
                .message("Authenticated successfully")
                .data(result)
                .build());
    }

    /**
     * POST /api/v1/auth/refresh-token
     * Exchange a valid refresh token for a new access token pair.
     */
    @PostMapping("/refresh-token")
    public ResponseEntity<ApiResponse.Success<ApiResponse.AuthToken>> refresh(
            @Valid @RequestBody AuthRequest.RefreshToken request) {
        ApiResponse.AuthToken result = authService.refreshToken(request);
        return ResponseEntity.ok(ApiResponse.Success.<ApiResponse.AuthToken>builder()
                        .success(true)
                .message("Token refreshed successfully")
                .data(result)
                .build());
    }

    /**
     * POST /api/v1/auth/logout
     * Revoke the refresh token.
     */
    @PostMapping("/logout")
    public ResponseEntity<ApiResponse.Success<Void>> logout(
            @Valid @RequestBody AuthRequest.RefreshToken request,
            @AuthenticationPrincipal UserPrincipal principal) {
        authService.logout(request.getRefreshToken());
        return ResponseEntity.ok(ApiResponse.Success.<Void>builder()
                        .success(true)
                .message("Logged out successfully")
                .build());
    }
}
