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

    // brandId String → UUID converter
    // null/blank → null return karo (query me filter nahi lagega)
    private UUID parseBrandId(String raw) {
        if (raw == null || raw.isBlank()) return null;
        try {
            return UUID.fromString(raw);
        } catch (IllegalArgumentException ex) {
            throw new IllegalArgumentException("Invalid brandId: " + raw);
        }
    }

    @Override
    @Transactional(readOnly = true)
    public List<ApiResponse.StoreDto> findByPincode(StoreRequest.ByPincode request) {
        UUID brandId = parseBrandId(request.getBrandId());
        List<Stores> stores = storeRepository.findByBrandAndPincode(brandId, request.getPincode());
        if (stores.isEmpty()) {
            throw new ResourceNotFoundException(
                    "No stores found for this pincode. Try searching by district or state.");
        }
        return stores.stream().map(this::toDto).toList();
    }

    @Override
    @Transactional(readOnly = true)
    public List<ApiResponse.StoreDto> findByState(StoreRequest.ByState request) {
        UUID brandId = parseBrandId(request.getBrandId());
        List<Stores> stores = storeRepository.findByBrandAndState(brandId, request.getState());
        if (stores.isEmpty()) {
            throw new ResourceNotFoundException(
                    "No stores found in this state for the selected brand.");
        }
        return stores.stream().map(this::toDto).toList();
    }

    // ── NAYA ──────────────────────────────────────────────────
    @Override
    @Transactional(readOnly = true)
    public List<ApiResponse.StoreDto> findByDistrict(StoreRequest.ByDistrict request) {
        UUID brandId = parseBrandId(request.getBrandId());
        List<Stores> stores = storeRepository.findByBrandAndDistrict(brandId, request.getDistrict());
        if (stores.isEmpty()) {
            throw new ResourceNotFoundException(
                    "No stores found in this district. Try searching by state.");
        }
        return stores.stream().map(this::toDto).toList();
    }
    // ──────────────────────────────────────────────────────────

    @Override
    @Transactional(readOnly = true)
    public ApiResponse.StoreDto findByQrCode(String qrCode) {
        Stores store = storeRepository.findByQrCodeAndIsActiveTrue(qrCode)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Store QR code is invalid or the store is inactive."));
        return toDto(store);
    }

    private ApiResponse.StoreDto toDto(Stores s) {
        return ApiResponse.StoreDto.builder()
                .id(s.getId())
                .brandId(s.getBrand().getId())
                .brandName(s.getBrand().getName())
                .name(s.getName())
                .address(s.getAddress())
                .city(s.getCity())
                .district(s.getDistrict())  // district bhi bhejo — frontend me dikhega
                .state(s.getState())
                .pincode(s.getPincode())
                .build();
    }
}