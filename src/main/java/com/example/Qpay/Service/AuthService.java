package com.example.Qpay.Service;

import com.example.Qpay.DTO.AuthRequest;
import com.example.Qpay.DTO.ApiResponse;

public interface AuthService {
    ApiResponse.OtpSent sendOtp(AuthRequest.SendOtp request);
    ApiResponse.OtpSent resendOtp(AuthRequest.ResendOtp request);
    ApiResponse.AuthToken verifyOtp(AuthRequest.VerifyOtp request);
    ApiResponse.AuthToken refreshToken(AuthRequest.RefreshToken request);
    void logout(String refreshToken);
}
