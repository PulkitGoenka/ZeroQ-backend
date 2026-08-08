package com.example.Qpay.Service.impl;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.example.Qpay.Document.ProductDocument;
import com.example.Qpay.DTO.RedisCart;
import com.example.Qpay.DTO.CartRequest;
import com.example.Qpay.DTO.ApiResponse;
import com.example.Qpay.Entity.*;
import com.example.Qpay.enums.SessionStatus;
import com.example.Qpay.ExceptionClass.GlobalExceptionHandler.*;
import com.example.Qpay.Repository.*;
import com.example.Qpay.Repository.mongo.ProductMongoRepository;
import com.example.Qpay.Security.UserPrincipal;
import com.example.Qpay.Service.CartService;
import com.example.Qpay.Util.RedisKeys;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

@Slf4j
@Service
@RequiredArgsConstructor
public class CartServiceImpl implements CartService {

    private final UserRepository userRepository;
    private final StoreRepository storeRepository;
    private final ShoppingSessionRepository sessionRepository;
    private final ScanHistoryRepository scanHistoryRepository;
    private final ProductMongoRepository productMongoRepository; // MongoDB
    private final RedisTemplate<String, Object> redisTemplate;
    private final ObjectMapper objectMapper;

    @Value("${session.cart-ttl-hours:24}")
    private int cartTtlHours;

    // ── Start Session ─────────────────────────────────────────────────────────

    @Override
    @Transactional
    public ApiResponse.SessionDto startSession(CartRequest.StartSession request, UserPrincipal principal) {
        if (sessionRepository.existsByUserIdAndStatus(principal.getId(), SessionStatus.ACTIVE)) {
            throw new SessionException("You already have an active shopping session. Please complete or end it first.");
        }

        User user = userRepository.findById(principal.getId())
                .orElseThrow(() -> new ResourceNotFoundException("User not found."));
        Stores store = storeRepository.findById(request.getStoreId())
                .filter(s -> Boolean.TRUE.equals(s.getIsActive()))
                .orElseThrow(() -> new ResourceNotFoundException("Store not found or inactive."));

        ShoppingSession session = ShoppingSession.builder()
                .user(user).store(store).status(SessionStatus.ACTIVE)
                .build();
        session = sessionRepository.save(session);

        String cartKey = RedisKeys.cart(session.getId());
        session.setRedisKey(cartKey);
        sessionRepository.save(session);

        RedisCart cart = RedisCart.builder()
                .sessionId(session.getId())
                .userId(user.getId())
                .storeId(store.getId())
                .brandId(store.getBrand().getId())
                .build();
        saveCart(cartKey, cart);

        log.info("Session {} started for user {} at store {}", session.getId(), user.getPhone(), store.getName());
        return ApiResponse.SessionDto.builder()
                .sessionId(session.getId()).status(SessionStatus.ACTIVE)
                .storeId(store.getId()).storeName(store.getName())
                .startedAt(session.getStartedAt()).build();
    }

    // ── Scan Barcode ──────────────────────────────────────────────────────────
    @Override
    @Transactional
    public ApiResponse.CartDto scanBarcode(CartRequest.ScanBarcode request, UserPrincipal principal) {

        // 1. Active session load karo
        ActiveSessionContext ctx = loadActiveContext(principal.getId());

        // 2. MongoDB se product dhoondho — us store ke liye
        ProductDocument product = productMongoRepository
                .findByBarcodeAndStoreIdAndAvailable(request.getBarcode(), ctx.cart().getStoreId().toString())
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Product not found for barcode: " + request.getBarcode()));

        // 3. Us store ka price nikalo
        ProductDocument.StorePrice storePrice = product.getStorePrices().stream()
                .filter(sp -> sp.getStoreId().equals(ctx.cart().getStoreId().toString()))
                .filter(sp -> sp.isAvailable())
                .findFirst()
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Product not available at this store."));

        // 4. Discount calculate karo
        BigDecimal mrp           = product.getMrp();
        BigDecimal discountPrice = storePrice.getCurrentPrice();
        BigDecimal discountAmt   = mrp.subtract(discountPrice);
        BigDecimal discountPct   = mrp.compareTo(BigDecimal.ZERO) > 0
                ? discountAmt.multiply(BigDecimal.valueOf(100)).divide(mrp, 2, java.math.RoundingMode.HALF_UP)
                : BigDecimal.ZERO;

        // 5. Cart me add karo (already hai to quantity +1)
        RedisCart.CartItem existing = ctx.cart().getItems().get(request.getBarcode());
        if (existing != null) {
            existing.setQuantity(existing.getQuantity() + 1);
        } else {
            RedisCart.CartItem newItem = RedisCart.CartItem.builder()
                    .barcode(product.getBarcode())
                    .productMongoId(product.getId())
                    .productName(product.getName())
                    .imageUrl(product.getImageUrl())
                    .mrp(mrp)
                    .discountPrice(discountPrice)
                    .discountPct(discountPct)
                    .quantity(1)
                    .build();
            ctx.cart().getItems().put(request.getBarcode(), newItem);
        }

        // 6. Redis me save karo
        saveCart(ctx.cartKey(), ctx.cart());

        // 7. Scan history me bhi save karo (permanent record)
        ScanHistory scanHistory = ScanHistory.builder()
                .session(ctx.session())
                .user(ctx.session().getUser())        // ← user add karo
                .barcode(product.getBarcode())
                .productName(product.getName())
                .imageUrl(product.getImageUrl())
                .scannedPrice(discountPrice)
                .productMongoId(product.getId())      // ← productMongoId add karo
                .scannedAt(OffsetDateTime.now())
                .build();
        scanHistoryRepository.save(scanHistory);

        log.info("Barcode {} scanned in session {}", request.getBarcode(), ctx.session().getId());

        // 8. Updated cart return karo
        return buildCartDto(ctx.cart(), ctx.session().getStore());
    }
    // ── Update Quantity ───────────────────────────────────────────────────────

    @Override
    @Transactional(readOnly = true)
    public ApiResponse.CartDto updateQuantity(CartRequest.UpdateQuantity request, UserPrincipal principal) {
        ActiveSessionContext ctx = loadActiveContext(principal.getId());
        if (request.getQuantity() == 0) {
            ctx.cart().getItems().remove(request.getBarcode());
        } else {
            RedisCart.CartItem item = ctx.cart().getItems().get(request.getBarcode());
            if (item == null) throw new ResourceNotFoundException("Item not found in cart.");
            item.setQuantity(request.getQuantity());
        }
        saveCart(ctx.cartKey(), ctx.cart());
        return buildCartDto(ctx.cart(), ctx.session().getStore());
    }

    // ── Remove Item ───────────────────────────────────────────────────────────

    @Override
    @Transactional(readOnly = true)
    public ApiResponse.CartDto removeItem(CartRequest.RemoveItem request, UserPrincipal principal) {
        ActiveSessionContext ctx = loadActiveContext(principal.getId());
        ctx.cart().getItems().remove(request.getBarcode());
        // Scan history DELETE NAHI HOGI — by design
        saveCart(ctx.cartKey(), ctx.cart());
        return buildCartDto(ctx.cart(), ctx.session().getStore());
    }

    // ── Get Cart ──────────────────────────────────────────────────────────────

    @Override
    @Transactional(readOnly = true)
    public ApiResponse.CartDto getCart(UserPrincipal principal) {
        ActiveSessionContext ctx = loadActiveContext(principal.getId());
        return buildCartDto(ctx.cart(), ctx.session().getStore());
    }

    // ── Scan History ──────────────────────────────────────────────────────────

    @Override
    @Transactional(readOnly = true)
    public List<ApiResponse.ScanHistoryItemDto> getScanHistory(UserPrincipal principal) {
        ShoppingSession session = sessionRepository
                .findLatestByUserAndStatus(principal.getId(), SessionStatus.ACTIVE)
                .orElseThrow(() -> new SessionException("No active shopping session found."));

        return scanHistoryRepository.findBySessionId(session.getId()).stream()
                .map(sh -> ApiResponse.ScanHistoryItemDto.builder()
                        .barcode(sh.getBarcode())
                        .productName(sh.getProductName())
                        .imageUrl(sh.getImageUrl())
                        .discountPrice(sh.getScannedPrice())
                        .scannedAt(sh.getScannedAt())
                        .build())
                .toList();
    }



    @Override
    @Transactional
    public void endSession(UserPrincipal principal) {
        // 1. DB se active session dhoondho
        ShoppingSession session = sessionRepository
                .findLatestByUserAndStatus(principal.getId(), SessionStatus.ACTIVE)
                .orElseThrow(() -> new SessionException("No active session found."));

        // 2. Redis cart delete karo
        String cartKey = RedisKeys.cart(session.getId());
        redisTemplate.delete(cartKey);

        // 3. DB me ABANDONED mark karo
        session.setStatus(SessionStatus.ABANDONED);
        session.setEndedAt(OffsetDateTime.now());
        sessionRepository.save(session);

        log.info("Session {} ended (abandoned) for user {}",
                session.getId(), principal.getId());
    }

    // ── Internal ──────────────────────────────────────────────────────────────

    @Override
    public RedisCart getActiveRedisCart(UUID userId) {
        ShoppingSession session = sessionRepository
                .findLatestByUserAndStatus(userId, SessionStatus.ACTIVE)
                .orElseThrow(() -> new SessionException("No active shopping session found."));
        return loadCartFromRedis(RedisKeys.cart(session.getId()))
                .orElseThrow(() -> new SessionException("Cart has expired. Please start a new session."));
    }

    // ── Helpers ───────────────────────────────────────────────────────────────

    private record ActiveSessionContext(ShoppingSession session, RedisCart cart, String cartKey) {}

    private ActiveSessionContext loadActiveContext(UUID userId) {
        ShoppingSession session = sessionRepository
                .findLatestByUserAndStatus(userId, SessionStatus.ACTIVE)
                .orElseThrow(() -> new SessionException(
                        "No active shopping session. Please select a store and start scanning."));
        String cartKey = RedisKeys.cart(session.getId());
        RedisCart cart = loadCartFromRedis(cartKey)
                .orElseThrow(() -> new SessionException("Cart session expired. Please start a new session."));
        return new ActiveSessionContext(session, cart, cartKey);
    }

    private Optional<RedisCart> loadCartFromRedis(String key) {
        Object raw = redisTemplate.opsForValue().get(key);
        if (raw == null) return Optional.empty();
        try {
            String json = raw instanceof String s ? s : objectMapper.writeValueAsString(raw);
            return Optional.of(objectMapper.readValue(json, RedisCart.class));
        } catch (JsonProcessingException e) {
            log.error("Failed to deserialize cart: {}", e.getMessage());
            return Optional.empty();
        }
    }

    private void saveCart(String key, RedisCart cart) {
        try {
            redisTemplate.opsForValue().set(
                    key, objectMapper.writeValueAsString(cart), cartTtlHours, TimeUnit.HOURS);
        } catch (JsonProcessingException e) {
            throw new RuntimeException("Failed to save cart state.");
        }
    }

    private ApiResponse.CartDto buildCartDto(RedisCart cart, Stores store) {
        List<ApiResponse.CartItemDto> items = cart.getItems().values().stream()
                .map(i -> ApiResponse.CartItemDto.builder()
                        .barcode(i.getBarcode())
                        .productName(i.getProductName())
                        .imageUrl(i.getImageUrl())
                        .mrp(i.getMrp())
                        .discountPrice(i.getDiscountPrice())
                        .discountPct(i.getDiscountPct())
                        .quantity(i.getQuantity())
                        .lineTotal(i.getLineTotal())
                        .build())
                .toList();

        return ApiResponse.CartDto.builder()
                .sessionId(cart.getSessionId()).storeId(cart.getStoreId())
                .storeName(store.getName()).items(items)
                .subtotal(cart.getSubtotal()).totalDiscount(cart.getTotalDiscount())
                .totalAmount(cart.getTotalAmount()).itemCount(cart.getItemCount())
                .build();
    }
}
