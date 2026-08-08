package com.example.Qpay.Util;


import java.util.UUID;

/**
 * Centralised Redis key factory – keeps key formats consistent across services.
 *
 * Key scheme:
 *   otp:{phone}                      → OTP string (TTL = otp.expiry-minutes)
 *   otp:cooldown:{phone}             → cooldown sentinel (TTL = otp.resend-cooldown-seconds)
 *   otp:resend-count:{phone}:{hour}  → resend counter per hour
 *   cart:{sessionId}                 → RedisCart JSON (TTL = session.cart-ttl-hours)
 *   payment:exit-qr:{token}          → orderId string (TTL = session.payment-qr-ttl-minutes)
 *   payment:counter-qr:{token}       → orderId string (TTL = session.cart-qr-ttl-minutes)
 */
public final class RedisKeys {

    private RedisKeys() {}

    public static String otp(String phone) {
        return "otp:" + phone;
    }

    public static String otpCooldown(String phone) {
        return "otp:cooldown:" + phone;
    }

    public static String otpResendCount(String phone) {
        // hourly bucket
        long hour = System.currentTimeMillis() / (1000 * 60 * 60);
        return "otp:resend-count:" + phone + ":" + hour;
    }

    public static String cart(UUID sessionId) {
        return "cart:" + sessionId;
    }

    public static String exitQr(String token) {
        return "payment:exit-qr:" + token;
    }

    public static String counterQr(String token) {
        return "payment:counter-qr:" + token;
    }

    public static String activeSession(UUID userId) {
        return "session:active:" + userId;
    }
}
