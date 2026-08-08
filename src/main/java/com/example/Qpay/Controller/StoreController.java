package com.example.Qpay.Controller;

import com.example.Qpay.DTO.StoreRequest;
import com.example.Qpay.DTO.ApiResponse;
import com.example.Qpay.Service.StoreService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/stores")
@RequiredArgsConstructor
public class StoreController {

    private final StoreService storeService;

    @PostMapping("/by-pincode")
    public ResponseEntity<ApiResponse.Success<List<ApiResponse.StoreDto>>> findByPincode(
            @Valid @RequestBody StoreRequest.ByPincode request) {
        List<ApiResponse.StoreDto> stores = storeService.findByPincode(request);
        return ResponseEntity.ok(ApiResponse.Success.<List<ApiResponse.StoreDto>>builder()
                .message("Stores fetched successfully")
                .data(stores)
                .build());
    }

    @PostMapping("/by-state")
    public ResponseEntity<ApiResponse.Success<List<ApiResponse.StoreDto>>> findByState(
            @Valid @RequestBody StoreRequest.ByState request) {
        List<ApiResponse.StoreDto> stores = storeService.findByState(request);
        return ResponseEntity.ok(ApiResponse.Success.<List<ApiResponse.StoreDto>>builder()
                .message("Stores fetched successfully")
                .data(stores)
                .build());
    }

    // ── NAYA — District endpoint ──────────────────────────────
    @PostMapping("/by-district")
    public ResponseEntity<ApiResponse.Success<List<ApiResponse.StoreDto>>> findByDistrict(
            @Valid @RequestBody StoreRequest.ByDistrict request) {
        List<ApiResponse.StoreDto> stores = storeService.findByDistrict(request);
        return ResponseEntity.ok(ApiResponse.Success.<List<ApiResponse.StoreDto>>builder()
                .message("Stores fetched successfully")
                .data(stores)
                .build());
    }
    // ─────────────────────────────────────────────────────────

    @GetMapping("/by-qr/{qrCode}")
    public ResponseEntity<ApiResponse.Success<ApiResponse.StoreDto>> findByQr(
            @PathVariable String qrCode) {
        ApiResponse.StoreDto store = storeService.findByQrCode(qrCode);
        return ResponseEntity.ok(ApiResponse.Success.<ApiResponse.StoreDto>builder()
                .message("Store found")
                .data(store)
                .build());
    }
}