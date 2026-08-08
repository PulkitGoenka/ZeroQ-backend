package com.example.Qpay.Service.impl;
import com.example.Qpay.DTO.*;
import com.example.Qpay.DTO.RedisCart;
import com.example.Qpay.Entity.*;
import com.example.Qpay.Repository.PaymentHistoryRepository;
import com.example.Qpay.Repository.ShoppingSessionRepository;
import  com.example.Qpay.Repository.RefreshTokenRepository;
import com.example.Qpay.Repository.mongo.ProductMongoRepository;
import com.example.Qpay.Security.UserPrincipal;
import com.example.Qpay.Service.PaymentService;
import com.example.Qpay.Util.QrCodeUtil;
import com.example.Qpay.ExceptionClass.GlobalExceptionHandler.*;
import com.example.Qpay.Repository.*;
import com.example.Qpay.Repository.StoreRepository;
import com.example.Qpay.Repository.UserRepository;
import com.example.Qpay.Service.CartService;
import com.example.Qpay.Util.RedisKeys;
import com.example.Qpay.enums.*;
import com.example.Qpay.enums.SessionStatus;
import jakarta.websocket.SessionException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

@Slf4j
@Service
@RequiredArgsConstructor
public class PaymentServiceImpl implements PaymentService {

    private final CartService cartService;
    private final UserRepository userRepository;
    private final StoreRepository storeRepository;
    private final OrderRepository orderRepository;
    private final PaymentHistoryRepository paymentHistoryRepository;
    private final ShoppingSessionRepository sessionRepository;
    private final ProductMongoRepository productMongoRepository; // MongoDB
    private final QrCodeUtil qrCodeUtil;
    private final RedisTemplate<String, Object> redisTemplate;

    @Value("${session.payment-qr-ttl-minutes:15}")
    private int paymentQrTtlMinutes;

    @Value("${session.cart-qr-ttl-minutes:30}")
    private int counterQrTtlMinutes;

    // ── Online Payment ────────────────────────────────────────────────────────

    @Override
    @Transactional
    public ApiResponse.PaymentInitiated initiateOnlinePayment(UserPrincipal principal) {
        RedisCart redisCart = cartService.getActiveRedisCart(principal.getId());
        if (redisCart.getItems().isEmpty()) {
            throw new PaymentException("Your cart is empty.");
        }

        ShoppingSession session = sessionRepository
                .findLatestByUserAndStatus(principal.getId(), SessionStatus.ACTIVE)
                .orElseThrow(() -> new PaymentException("No active session found."));

        Order order = buildOrder(session, redisCart, PaymentMethod.ONLINE);
        order.setOrderStatus(OrderStatus.PENDING_PAYMENT);

        String qrToken = qrCodeUtil.generateToken();
        order.setExitQrToken(qrToken);
        order = orderRepository.save(order);

        redisTemplate.opsForValue().set(
                RedisKeys.exitQr(qrToken), order.getId().toString(),
                paymentQrTtlMinutes, TimeUnit.MINUTES);

        return ApiResponse.PaymentInitiated.builder()
                .orderId(order.getId()).method(PaymentMethod.ONLINE)
                .totalAmount(order.getTotalAmount()).qrToken(qrToken)
                .qrImageBase64(qrCodeUtil.generateQrBase64(qrToken))
                .qrExpirySeconds(paymentQrTtlMinutes * 60).build();
    }

    // ── Cash Payment ──────────────────────────────────────────────────────────

    @Override
    @Transactional
    public ApiResponse.PaymentInitiated initiateCashPayment(UserPrincipal principal) {
        RedisCart redisCart = cartService.getActiveRedisCart(principal.getId());
        if (redisCart.getItems().isEmpty()) {
            throw new PaymentException("Your cart is empty.");
        }

        ShoppingSession session = sessionRepository
                .findLatestByUserAndStatus(principal.getId(), SessionStatus.ACTIVE)
                .orElseThrow(() -> new PaymentException("No active session found."));

        Order order = buildOrder(session, redisCart, PaymentMethod.CASH);
        order.setOrderStatus(OrderStatus.PENDING_PAYMENT);

        String qrToken = qrCodeUtil.generateToken();
        order.setCounterQrToken(qrToken);
        order = orderRepository.save(order);

        redisTemplate.opsForValue().set(
                RedisKeys.counterQr(qrToken), order.getId().toString(),
                counterQrTtlMinutes, TimeUnit.MINUTES);

        return ApiResponse.PaymentInitiated.builder()
                .orderId(order.getId()).method(PaymentMethod.CASH)
                .totalAmount(order.getTotalAmount()).qrToken(qrToken)
                .qrImageBase64(qrCodeUtil.generateQrBase64(qrToken))
                .qrExpirySeconds(counterQrTtlMinutes * 60).build();
    }

    // ── Guard Exit QR ─────────────────────────────────────────────────────────

    @Override
    @Transactional
    public ApiResponse.BillDto verifyExitQr(PaymentRequest.VerifyExitQr request) {
        Order order = resolveOrderByExitQr(request.getQrToken());

        if (Boolean.TRUE.equals(order.getQrUsed()))
            throw new PaymentException("This QR code has already been used.");
        if (order.getOrderStatus() != OrderStatus.PENDING_PAYMENT)
            throw new PaymentException("Order is not in a payable state.");
        if (order.getPaymentMethod() != PaymentMethod.ONLINE)
            throw new PaymentException("This QR is not for an online payment.");

        order.setPaymentStatus(PaymentStatus.COMPLETED);
        order.setOrderStatus(OrderStatus.PAID);
        order.setQrUsed(true);
        order.setPaidAt(OffsetDateTime.now());
        orderRepository.save(order);

        PaymentHistory history = savePaymentHistory(order);
        decrementMongoStock(order); // MongoDB stock update
        endSession(order.getSession(), order.getUser().getId());

        return buildBillDto(order, history);
    }

    // ── Counter Cash Confirm ──────────────────────────────────────────────────

    @Override
    @Transactional
    public ApiResponse.BillDto confirmCashPayment(PaymentRequest.ConfirmCashPayment request) {
        Order order = resolveOrderByCounterQr(request.getQrToken());

        if (Boolean.TRUE.equals(order.getQrUsed()))
            throw new PaymentException("This QR code has already been scanned.");
        if (order.getOrderStatus() != OrderStatus.PENDING_PAYMENT)
            throw new PaymentException("Order is not awaiting payment.");
        if (order.getPaymentMethod() != PaymentMethod.CASH)
            throw new PaymentException("This QR is not for a cash payment.");

        order.setPaymentStatus(PaymentStatus.COMPLETED);
        order.setOrderStatus(OrderStatus.PAID);
        order.setQrUsed(true);
        order.setPaidAt(OffsetDateTime.now());
        orderRepository.save(order);

        PaymentHistory history = savePaymentHistory(order);
        decrementMongoStock(order); // MongoDB stock update
        endSession(order.getSession(), order.getUser().getId());

        return buildBillDto(order, history);
    }

    // ── Counter Cart Details ──────────────────────────────────────────────────

    @Override
    @Transactional(readOnly = true)
    public ApiResponse.CounterCartDto getCounterCartDetails(String qrToken) {
        Order order = resolveOrderByCounterQr(qrToken);
        if (Boolean.TRUE.equals(order.getQrUsed()))
            throw new PaymentException("This QR has already been processed.");

        Order withItems = orderRepository.findByIdWithItems(order.getId())
                .orElseThrow(() -> new ResourceNotFoundException("Order not found."));

        List<ApiResponse.CartItemDto> items = withItems.getItems().stream()
                .map(i -> ApiResponse.CartItemDto.builder()
                        .barcode(i.getBarcode()).productName(i.getProductName())
                        .mrp(i.getMrp()).discountPrice(i.getDiscountPrice())
                        .quantity(i.getQuantity()).lineTotal(i.getLineTotal()).build())
                .toList();

        return ApiResponse.CounterCartDto.builder()
                .orderId(withItems.getId())
                .userId(withItems.getUser().getId())
                .userName(withItems.getUser().getName())
                .userPhone(withItems.getUser().getPhone())
                .items(items).totalAmount(withItems.getTotalAmount())
                .storeName(withItems.getStore().getName()).build();
    }

    // ── Payment History ───────────────────────────────────────────────────────

    @Override
    @Transactional(readOnly = true)
    public List<ApiResponse.PaymentHistoryDto> getPaymentHistory(UserPrincipal principal, int page, int size) {
        Page<PaymentHistory> pageResult = paymentHistoryRepository
                .findByUserIdOrderByPaidAtDesc(principal.getId(), PageRequest.of(page, size));
        return pageResult.getContent().stream()
                .map(ph -> ApiResponse.PaymentHistoryDto.builder()
                        .id(ph.getId()).orderId(ph.getOrder().getId())
                        .billRef(ph.getBillRef()).paymentMethod(ph.getPaymentMethod())
                        .totalAmount(ph.getTotalAmount()).itemCount(ph.getItemCount())
                        .storeName(ph.getStore().getName())
                        .brandName(ph.getStore().getBrand().getName())
                        .paidAt(ph.getPaidAt()).build())
                .toList();
    }

    // ── Private Helpers ───────────────────────────────────────────────────────

    private Order buildOrder(ShoppingSession session, RedisCart cart, PaymentMethod method) {
        User user = userRepository.getReferenceById(session.getUser().getId());
        Stores store = storeRepository.getReferenceById(session.getStore().getId());

        Order order = Order.builder()
                .session(session).user(user).store(store)
                .paymentMethod(method).subtotal(cart.getSubtotal())
                .totalDiscount(cart.getTotalDiscount()).totalAmount(cart.getTotalAmount())
                .build();

        List<OrderItem> orderItems = cart.getItems().values().stream()
                .map(ci -> OrderItem.builder()
                        .order(order)
                        .barcode(ci.getBarcode())
                        .productName(ci.getProductName())
                        .productMongoId(ci.getProductMongoId()) // MongoDB ID
                        .mrp(ci.getMrp())
                        .discountPrice(ci.getDiscountPrice())
                        .quantity(ci.getQuantity())
                        .lineTotal(ci.getLineTotal())
                        .build())
                .toList();

        order.getItems().addAll(orderItems);
        return order;
    }

    private PaymentHistory savePaymentHistory(Order order) {
        String billRef = "BILL-" + order.getId().toString().substring(0, 8).toUpperCase();
        return paymentHistoryRepository.save(PaymentHistory.builder()
                .order(order).user(order.getUser()).store(order.getStore())
                .paymentMethod(order.getPaymentMethod())
                .totalAmount(order.getTotalAmount())
                .itemCount(order.getItems().stream().mapToInt(OrderItem::getQuantity).sum())
                .billRef(billRef).build());
    }

    /**
     * MongoDB mein stock decrement karo — payment ke baad.
     * Stock 0 hone pe available = false automatically.
     */
    private void decrementMongoStock(Order order) {
        String storeId = order.getStore().getId().toString();
        order.getItems().forEach(item -> {
            try {
                productMongoRepository.findByBarcodeAndActiveTrue(item.getBarcode())
                        .ifPresent(product -> {
                            product.decrementStock(storeId, item.getQuantity());
                            productMongoRepository.save(product);
                        });
            } catch (Exception e) {
                log.error("Stock decrement failed for {}: {}", item.getBarcode(), e.getMessage());
            }
        });
    }

    private void endSession(ShoppingSession session, UUID userId) {
        session.setStatus(SessionStatus.COMPLETED);
        session.setEndedAt(OffsetDateTime.now());
        sessionRepository.save(session);
        redisTemplate.delete(RedisKeys.cart(session.getId()));
        log.info("Session {} ended for user {}", session.getId(), userId);
    }

    private ApiResponse.BillDto buildBillDto(Order order, PaymentHistory history) {
        List<ApiResponse.BillItemDto> billItems = order.getItems().stream()
                .map(i -> ApiResponse.BillItemDto.builder()
                        .barcode(i.getBarcode()).productName(i.getProductName())
                        .mrp(i.getMrp()).discountPrice(i.getDiscountPrice())
                        .quantity(i.getQuantity()).lineTotal(i.getLineTotal()).build())
                .toList();

        return ApiResponse.BillDto.builder()
                .orderId(order.getId()).billRef(history.getBillRef())
                .paymentMethod(order.getPaymentMethod()).items(billItems)
                .subtotal(order.getSubtotal()).totalDiscount(order.getTotalDiscount())
                .totalAmount(order.getTotalAmount()).paidAt(order.getPaidAt())
                .storeName(order.getStore().getName())
                .brandName(order.getStore().getBrand().getName()).build();
    }

    private Order resolveOrderByExitQr(String token) {
        Object cached = redisTemplate.opsForValue().get(RedisKeys.exitQr(token));
        if (cached != null)
            return orderRepository.findByIdWithItems(UUID.fromString(cached.toString()))
                    .orElseThrow(() -> new ResourceNotFoundException("Order not found."));
        return orderRepository.findByExitQrToken(token)
                .orElseThrow(() -> new PaymentException("Invalid or expired QR code."));
    }

    private Order resolveOrderByCounterQr(String token) {
        Object cached = redisTemplate.opsForValue().get(RedisKeys.counterQr(token));
        if (cached != null)
            return orderRepository.findByIdWithItems(UUID.fromString(cached.toString()))
                    .orElseThrow(() -> new ResourceNotFoundException("Order not found."));
        return orderRepository.findByCounterQrToken(token)
                .orElseThrow(() -> new PaymentException("Invalid or expired QR code."));
    }
}