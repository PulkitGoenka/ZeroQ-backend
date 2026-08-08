package com.example.Qpay.Service;
import com.example.Qpay.DTO.ApiResponse;
import com.example.Qpay.DTO.CartRequest;
import com.example.Qpay.DTO.RedisCart;
import com.example.Qpay.Security.UserPrincipal;

import java.util.List;
import java.util.UUID;


public interface CartService {
    ApiResponse.SessionDto startSession(CartRequest.StartSession request, UserPrincipal principal);
    ApiResponse.CartDto scanBarcode(CartRequest.ScanBarcode request, UserPrincipal principal);
    ApiResponse.CartDto updateQuantity(CartRequest.UpdateQuantity request, UserPrincipal principal);
    ApiResponse.CartDto removeItem(CartRequest.RemoveItem request, UserPrincipal principal);
    ApiResponse.CartDto getCart(UserPrincipal principal);
    List<ApiResponse.ScanHistoryItemDto> getScanHistory(UserPrincipal principal);
    RedisCart getActiveRedisCart(UUID userId);
    void endSession(UserPrincipal principal);
}
