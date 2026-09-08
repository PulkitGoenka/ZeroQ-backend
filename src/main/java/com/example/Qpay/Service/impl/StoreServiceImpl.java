package com.example.Qpay.Service.impl;

import com.example.Qpay.DTO.StoreRequest;
import com.example.Qpay.DTO.ApiResponse;
import com.example.Qpay.Entity.Stores;
import com.example.Qpay.ExceptionClass.GlobalExceptionHandler.ResourceNotFoundException;
import com.example.Qpay.Repository.StoreRepository;
import com.example.Qpay.Service.StoreService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class StoreServiceImpl implements StoreService {

    private final StoreRepository storeRepository;

    private UUID parseBrandId(String raw) {
        if (raw == null || raw.isBlank()) return null;
        try {
            return UUID.fromString(raw.trim());
        } catch (IllegalArgumentException ex) {
            return null;
        }
    }

    @Override
    @Transactional(readOnly = true)
    public List<ApiResponse.StoreDto> findByPincode(StoreRequest.ByPincode request) {
        UUID brandId = parseBrandId(request.getBrandId());
        String pin = request.getPincode() != null ? request.getPincode().trim() : "";
        List<Stores> stores = storeRepository.findByBrandAndPincode(brandId, pin);
        return stores.stream().map(this::toDto).toList();
    }

    @Override
    @Transactional(readOnly = true)
    public List<ApiResponse.StoreDto> findByState(StoreRequest.ByState request) {
        UUID brandId = parseBrandId(request.getBrandId());
        String state = request.getState() != null ? request.getState().trim() : "";
        List<Stores> stores = storeRepository.findByBrandAndState(brandId, state);
        return stores.stream().map(this::toDto).toList();
    }

    @Override
    @Transactional(readOnly = true)
    public List<ApiResponse.StoreDto> findByDistrict(StoreRequest.ByDistrict request) {
        UUID brandId = parseBrandId(request.getBrandId());
        String district = request.getDistrict() != null ? request.getDistrict().trim() : "";
        List<Stores> stores = storeRepository.findByBrandAndDistrict(brandId, district);
        return stores.stream().map(this::toDto).toList();
    }

    @Override
    @Transactional(readOnly = true)
    public ApiResponse.StoreDto findByQrCode(String qrCode) {
        Stores store = storeRepository.findByQrCodeAndIsActiveTrue(qrCode)
                .orElseThrow(() -> new ResourceNotFoundException("Store QR code is invalid or inactive."));
        return toDto(store);
    }

    private ApiResponse.StoreDto toDto(Stores s) {
        return ApiResponse.StoreDto.builder()
                .id(s.getId())
                .brandId(s.getBrand() != null ? s.getBrand().getId() : null)
                .brandName(s.getBrand() != null ? s.getBrand().getName() : "")
                .name(s.getName())
                .address(s.getAddress())
                .city(s.getCity())
                .district(s.getDistrict())
                .state(s.getState())
                .pincode(s.getPincode())
                .build();
    }
}