package com.example.Qpay.Service;

import com.example.Qpay.DTO.PaymentRequest;
import com.example.Qpay.DTO.ApiResponse;
import com.example.Qpay.Security.UserPrincipal;

import java.util.List;

public interface PaymentService {

    /** Customer initiates online payment — returns Razorpay order details and exit QR */
    ApiResponse.PaymentInitiated initiateOnlinePayment(UserPrincipal principal);

    /** Customer chooses cash — returns counter QR for billing counter */
    ApiResponse.PaymentInitiated initiateCashPayment(UserPrincipal principal);

    /**
     * App sends Razorpay paymentId, orderId, and signature after SDK callback.
     * Verifies HMAC signature, decrements stock, closes session, and generates bill.
     */
    ApiResponse.BillDto verifyRazorpayPayment(PaymentRequest.VerifyRazorpay request);

    /**
     * Guard at exit gate scans QR — verifies and closes session.
     * This is an internal endpoint (no user auth needed).
     */
    ApiResponse.BillDto verifyExitQr(PaymentRequest.VerifyExitQr request);

    /**
     * Billing counter confirms cash payment after scanning counter QR.
     * This is an internal endpoint (no user auth needed).
     */
    ApiResponse.BillDto confirmCashPayment(PaymentRequest.ConfirmCashPayment request);

    /** Returns order/cart details for a counter QR (called by billing counter after QR scan). */
    ApiResponse.CounterCartDto getCounterCartDetails(String qrToken);

    /** Paginated payment history for the customer. */
    List<ApiResponse.PaymentHistoryDto> getPaymentHistory(UserPrincipal principal, int page, int size);
}