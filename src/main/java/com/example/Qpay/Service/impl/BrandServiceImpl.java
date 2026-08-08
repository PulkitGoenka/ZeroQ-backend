package com.example.Qpay.Service.impl;
import com.example.Qpay.DTO.ApiResponse;
import com.example.Qpay.Repository.BrandsRepository;
import com.example.Qpay.Service.BrandsService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class BrandServiceImpl implements BrandsService {

    private final BrandsRepository brandRepository;

    @Override
    @Transactional(readOnly = true)
    public List<ApiResponse.BrandDto> getAllActiveBrands() {
        return brandRepository.findAllByIsActiveTrueOrderByNameAsc().stream()
                .map(b -> ApiResponse.BrandDto.builder()
                        .id(b.getId())
                        .name(b.getName())
                        .slug(b.getSlug())
                        .logoUrl(b.getLogoUrl())
                        .build())
                .toList();
    }
}
