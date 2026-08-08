package com.example.Qpay.Controller;


import com.example.Qpay.DTO.ApiResponse;
import com.example.Qpay.Service.BrandsService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/v1/brands")
@RequiredArgsConstructor
public class BrandController {

    private final BrandsService brandService;

    /**
     * GET /api/v1/brands
     * Returns all active brands. Public endpoint — no auth required.
     */
    @GetMapping
    public ResponseEntity<ApiResponse.Success<List<ApiResponse.BrandDto>>> getAllBrands() {
        List<ApiResponse.BrandDto> brands = brandService.getAllActiveBrands();
        return ResponseEntity.ok(ApiResponse.Success.<List<ApiResponse.BrandDto>>builder()
                .message("Brands fetched successfully")
                .data(brands)
                .build());
    }
}
