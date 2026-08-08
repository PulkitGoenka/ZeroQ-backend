package com.example.Qpay.Controller;

import com.example.Qpay.DTO.PaymentRequest;
import com.example.Qpay.DTO.ApiResponse;
import com.example.Qpay.Entity.Order;
import com.example.Qpay.ExceptionClass.GlobalExceptionHandler;
import com.example.Qpay.Security.UserPrincipal;
import com.example.Qpay.Service.PaymentService;
import com.example.Qpay.Repository.OrderRepository;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/payment")
@RequiredArgsConstructor
public class PaymentController {

    private final PaymentService paymentService;
    private final OrderRepository orderRepository;

    /**
     * POST /api/v1/payment/initiate/online
     * Customer chooses to pay online.
     * Returns a QR code (base64 PNG) that the exit gate guard scans.
     * Once initiated, the customer cannot go back to editing the cart.
     */
    @PostMapping("/initiate/online")
    public ResponseEntity<ApiResponse.Success<ApiResponse.PaymentInitiated>> initiateOnline(
            @AuthenticationPrincipal UserPrincipal principal) {
        ApiResponse.PaymentInitiated result = paymentService.initiateOnlinePayment(principal);
        return ResponseEntity.ok(ApiResponse.Success.<ApiResponse.PaymentInitiated>builder()
                .message("Online payment initiated. Please scan the QR at the exit gate after paying.")
                .data(result)
                .build());
    }

    /**
     * GET /api/v1/payment/status/{orderId}
     * Frontend polls this to check if payment is COMPLETED
     */
    @GetMapping("/status/{orderId}")
    public ResponseEntity<ApiResponse.Success<Map<String, String>>> getPaymentStatus(
            @PathVariable UUID orderId,
            @AuthenticationPrincipal UserPrincipal principal) {
        Order order = orderRepository.findById(orderId)
                .orElseThrow(() -> new GlobalExceptionHandler.ResourceNotFoundException("Order not found"));
        return ResponseEntity.ok(ApiResponse.Success.<Map<String, String>>builder()
                .message("Status fetched")
                .data(Map.of("status", order.getPaymentStatus().name()))
                .build());
    }

    /**
     * POST /api/v1/payment/initiate/cash
     * Customer chooses to pay cash at counter.
     * Returns a QR code to show at the billing counter.
     */
    @PostMapping("/initiate/cash")
    public ResponseEntity<ApiResponse.Success<ApiResponse.PaymentInitiated>> initiateCash(
            @AuthenticationPrincipal UserPrincipal principal) {
        ApiResponse.PaymentInitiated result = paymentService.initiateCashPayment(principal);
        return ResponseEntity.ok(ApiResponse.Success.<ApiResponse.PaymentInitiated>builder()
                .message("Please show this QR code at the billing counter to pay cash.")
                .data(result)
                .build());
    }

    /**
     * POST /api/v1/payment/verify-exit-qr
     * INTERNAL — Called by the exit gate guard's device when they scan the customer's QR.
     * Verifies online payment, generates bill, ends session.
     */
    @PostMapping("/verify-exit-qr")
    public ResponseEntity<ApiResponse.Success<ApiResponse.BillDto>> verifyExitQr(
            @Valid @RequestBody PaymentRequest.VerifyExitQr request) {
        ApiResponse.BillDto bill = paymentService.verifyExitQr(request);
        return ResponseEntity.ok(ApiResponse.Success.<ApiResponse.BillDto>builder()
                .message("Payment verified. Customer may exit. Bill generated.")
                .data(bill)
                .build());
    }

    /**
     * GET /api/v1/payment/counter-cart/{qrToken}
     * INTERNAL — Called by the billing counter device when they scan the customer's QR.
     * Returns the customer's full cart/item list for the counter to display.
     */
    @GetMapping("/counter-cart/{qrToken}")
    public ResponseEntity<ApiResponse.Success<ApiResponse.CounterCartDto>> getCounterCart(
            @PathVariable String qrToken) {
        ApiResponse.CounterCartDto cart = paymentService.getCounterCartDetails(qrToken);
        return ResponseEntity.ok(ApiResponse.Success.<ApiResponse.CounterCartDto>builder()
                .message("Cart details fetched for billing counter")
                .data(cart)
                .build());
    }

    /**
     * POST /api/v1/payment/confirm-cash
     * INTERNAL — Billing counter confirms cash has been received and payment is done.
     * Generates bill, clears cart and scan history, ends session.
     */
    @PostMapping("/confirm-cash")
    public ResponseEntity<ApiResponse.Success<ApiResponse.BillDto>> confirmCash(
            @Valid @RequestBody PaymentRequest.ConfirmCashPayment request) {
        ApiResponse.BillDto bill = paymentService.confirmCashPayment(request);
        return ResponseEntity.ok(ApiResponse.Success.<ApiResponse.BillDto>builder()
                .message("Cash payment confirmed. Bill generated successfully.")
                .data(bill)
                .build());
    }

    /**
     * GET /api/v1/payment/history
     * Returns the customer's full payment history (permanent — survives session end).
     */
    @GetMapping("/history")
    public ResponseEntity<ApiResponse.Success<List<ApiResponse.PaymentHistoryDto>>> getHistory(
            @AuthenticationPrincipal UserPrincipal principal,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        List<ApiResponse.PaymentHistoryDto> history = paymentService.getPaymentHistory(principal, page, size);
        return ResponseEntity.ok(ApiResponse.Success.<List<ApiResponse.PaymentHistoryDto>>builder()
                .message("Payment history fetched successfully")
                .data(history)
                .build());
    }
}
