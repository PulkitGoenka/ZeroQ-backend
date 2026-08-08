package com.example.Qpay.Controller;


import com.example.Qpay.DTO.CartRequest;
import com.example.Qpay.DTO.ApiResponse;
import com.example.Qpay.Security.UserPrincipal;
import com.example.Qpay.Service.CartService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/cart")
@RequiredArgsConstructor
public class CartController {

    private final CartService cartService;

    /**
     * POST /api/v1/cart/session/start
     * Customer selects a store and starts a shopping session.
     * Session is also created when first barcode is scanned — this endpoint is called right after store selection.
     */
    @PostMapping("/session/start")
    public ResponseEntity<ApiResponse.Success<ApiResponse.SessionDto>> startSession(
            @Valid @RequestBody CartRequest.StartSession request,
            @AuthenticationPrincipal UserPrincipal principal) {
        ApiResponse.SessionDto session = cartService.startSession(request, principal);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.Success.<ApiResponse.SessionDto>builder()
                        .message("Shopping session started")
                        .data(session)
                        .build());
    }

    /**
     * POST /api/v1/cart/session/end
     * User manually ends session (no products bought, or wants to switch store).
     * Requires JWT — user must be logged in.
     */
    @PostMapping("/session/end")
    public ResponseEntity<ApiResponse.Success<String>> endSession(
            @AuthenticationPrincipal UserPrincipal principal) {
        cartService.endSession(principal);
        return ResponseEntity.ok(ApiResponse.Success.<String>builder()
                .message("Session ended successfully")
                .data(null)
                .build());
    }

    /**
     * POST /api/v1/cart/
     * Scan a product barcode — adds to cart AND records in permanent scan history.
     * Session must already be active.
     */
    @PostMapping("/scan")
    public ResponseEntity<ApiResponse.Success<ApiResponse.CartDto>> scanBarcode(
            @Valid @RequestBody CartRequest.ScanBarcode request,
            @AuthenticationPrincipal UserPrincipal principal) {
        ApiResponse.CartDto cart = cartService.scanBarcode(request, principal);
        return ResponseEntity.ok(ApiResponse.Success.<ApiResponse.CartDto>builder()
                .message("Product scanned and added to cart")
                .data(cart)
                .build());
    }

    /**
     * GET /api/v1/cart
     * Get the current cart state for the active session.
     */
    @GetMapping
    public ResponseEntity<ApiResponse.Success<ApiResponse.CartDto>> getCart(
            @AuthenticationPrincipal UserPrincipal principal) {
        ApiResponse.CartDto cart = cartService.getCart(principal);
        return ResponseEntity.ok(ApiResponse.Success.<ApiResponse.CartDto>builder()
                .message("Cart fetched successfully")
                .data(cart)
                .build());
    }

    /**
     * PATCH /api/v1/cart/quantity
     * Update quantity of an item in cart. Setting quantity to 0 removes from cart.
     * NOTE: Removing from cart does NOT remove from scan history.
     */
    @PatchMapping("/quantity")
    public ResponseEntity<ApiResponse.Success<ApiResponse.CartDto>> updateQuantity(
            @Valid @RequestBody CartRequest.UpdateQuantity request,
            @AuthenticationPrincipal UserPrincipal principal) {
        ApiResponse.CartDto cart = cartService.updateQuantity(request, principal);
        return ResponseEntity.ok(ApiResponse.Success.<ApiResponse.CartDto>builder()
                .message("Cart updated successfully")
                .data(cart)
                .build());
    }

    /**
     * DELETE /api/v1/cart/item
     * Remove an item from cart. Scan history is preserved.
     */
    @DeleteMapping("/item")
    public ResponseEntity<ApiResponse.Success<ApiResponse.CartDto>> removeItem(
            @Valid @RequestBody CartRequest.RemoveItem request,
            @AuthenticationPrincipal UserPrincipal principal) {
        ApiResponse.CartDto cart = cartService.removeItem(request, principal);
        return ResponseEntity.ok(ApiResponse.Success.<ApiResponse.CartDto>builder()
                .message("Item removed from cart. Scan history is preserved.")
                .data(cart)
                .build());
    }

    /**
     * GET /api/v1/cart/scan-history
     * Returns all barcodes scanned in the current session (never cleared, even if removed from cart).
     */
    @GetMapping("/scan-history")
    public ResponseEntity<ApiResponse.Success<List<ApiResponse.ScanHistoryItemDto>>> getScanHistory(
            @AuthenticationPrincipal UserPrincipal principal) {
        List<ApiResponse.ScanHistoryItemDto> history = cartService.getScanHistory(principal);
        return ResponseEntity.ok(ApiResponse.Success.<List<ApiResponse.ScanHistoryItemDto>>builder()
                .message("Scan history fetched successfully")
                .data(history)
                .build());
    }
}

