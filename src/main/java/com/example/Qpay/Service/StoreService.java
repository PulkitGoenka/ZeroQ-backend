package com.example.Qpay.Service;

import com.example.Qpay.DTO.ApiResponse;
import com.example.Qpay.DTO.StoreRequest;

import java.util.List;

public interface StoreService {
    List<ApiResponse.StoreDto> findByPincode(StoreRequest.ByPincode request);
    List<ApiResponse.StoreDto> findByState(StoreRequest.ByState request);
    List<ApiResponse.StoreDto> findByDistrict(StoreRequest.ByDistrict request); // NAYA
    ApiResponse.StoreDto findByQrCode(String qrCode);
}