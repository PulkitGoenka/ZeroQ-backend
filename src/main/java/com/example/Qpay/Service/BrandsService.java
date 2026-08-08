package com.example.Qpay.Service;
import com.example.Qpay.DTO.ApiResponse;
import java.util.List;

public interface BrandsService {
    List<ApiResponse.BrandDto> getAllActiveBrands();
}
