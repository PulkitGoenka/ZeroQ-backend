package com.example.Qpay.Service.impl;
import com.example.Qpay.DTO.AuthRequest;
import com.example.Qpay.DTO.ApiResponse;
import com.example.Qpay.Entity.OtpLog;
import com.example.Qpay.Entity.RefreshToken;
import com.example.Qpay.Entity.User;
import com.example.Qpay.ExceptionClass.GlobalExceptionHandler.*;
import com.example.Qpay.Repository.OtpLogRepository;
import com.example.Qpay.Repository.RefreshTokenRepository;
import com.example.Qpay.Repository.UserRepository;
import com.example.Qpay.Security.JwtUtil;
import com.example.Qpay.Service.AuthService;
import com.example.Qpay.Util.RedisKeys;
import com.example.Qpay.Service.SmsService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.security.SecureRandom;
import java.time.OffsetDateTime;
import java.util.concurrent.TimeUnit;

@Slf4j
@Service
@RequiredArgsConstructor
public class AuthServiceImpl implements AuthService {

    private final UserRepository userRepository;
    private final OtpLogRepository otpLogRepository;
    private final RefreshTokenRepository refreshTokenRepository;
    private final JwtUtil jwtUtil;
    private final PasswordEncoder passwordEncoder;
    private final RedisTemplate<String, Object> redisTemplate;
    private final SmsService smsService; // ✅ Twilio SMS service

    @Value("${otp.length:6}")
    private int otpLength;

    @Value("${otp.expiry-minutes:5}")
    private int otpExpiryMinutes;

    @Value("${otp.max-resend-per-hour:3}")
    private int maxResendPerHour;

    @Value("${otp.resend-cooldown-seconds:60}")
    private int resendCooldownSeconds;

    @Value("${jwt.expiration-ms}")
    private long jwtExpirationMs;

    // ── TEMPORARY: OTP bypass for testing while Twilio quota is exhausted ─────
    // Set OTP_BYPASS_ENABLED=true on Render to skip real SMS sending, and let
    // OTP_BYPASS_CODE (any phone + this code) log in without a real OTP.
    // Set OTP_BYPASS_ENABLED=false (or remove the var) to go back to normal
    // Twilio-verified OTP flow — no code changes needed either way.
    @Value("${otp.bypass-enabled:false}")
    private boolean otpBypassEnabled;

    @Value("${otp.bypass-code:000000}")
    private String otpBypassCode;

    // ── Send OTP ──────────────────────────────────────────────────────────────

    @Override
    @Transactional
    public ApiResponse.OtpSent sendOtp(AuthRequest.SendOtp request) {
        String phone = request.getPhone();
        boolean isNewUser = !userRepository.existsByPhone(phone);

        checkCooldown(phone);
        checkHourlyLimit(phone);

        String otp = generateOtp();
        storeOtpInRedis(phone, otp);
        saveOtpLog(phone, otp);

        // New user ka naam Redis mein save karo
        if (isNewUser && StringUtils.hasText(request.getName())) {
            redisTemplate.opsForValue().set(
                    "user:name:" + phone,
                    request.getName(),
                    otpExpiryMinutes, TimeUnit.MINUTES
            );
        }

        if (otpBypassEnabled) {
            // Twilio quota bacha rahe hain — SMS bheja hi nahi, sirf log me OTP print
            // (ya universal bypass code "${otp.bypass-code}" bhi use kar sakte ho).
            log.warn("OTP BYPASS active — not sending real SMS. OTP for {} is {} (bypass code: {})",
                    phone, otp, otpBypassCode);
        } else {
            smsService.sendOtp(phone, otp);
        }

        log.info("OTP sent to {} (newUser={})", phone, isNewUser);
        return ApiResponse.OtpSent.builder()
                .phone(phone)
                .newUser(isNewUser)
                .expirySeconds(otpExpiryMinutes * 60)
                .cooldownSeconds(resendCooldownSeconds)
                .build();
    }

    // ── Resend OTP ────────────────────────────────────────────────────────────

    @Override
    @Transactional
    public ApiResponse.OtpSent resendOtp(AuthRequest.ResendOtp request) {
        String phone = request.getPhone();

        if (Boolean.TRUE.equals(redisTemplate.hasKey(RedisKeys.otpCooldown(phone)))) {
            Long ttl = redisTemplate.getExpire(RedisKeys.otpCooldown(phone), TimeUnit.SECONDS);
            throw new RateLimitException(
                    "Please wait " + (ttl != null ? ttl : resendCooldownSeconds) + " seconds before resending OTP."
            );
        }

        checkHourlyLimit(phone);

        otpLogRepository.findLatestActiveByPhone(phone, OffsetDateTime.now())
                .ifPresent(otpLog -> {
                    otpLog.setResendCount(otpLog.getResendCount() + 1);
                    otpLogRepository.save(otpLog);
                });

        String otp = generateOtp();
        storeOtpInRedis(phone, otp);
        saveOtpLog(phone, otp);

        if (otpBypassEnabled) {
            log.warn("OTP BYPASS active — not resending real SMS. OTP for {} is {} (bypass code: {})",
                    phone, otp, otpBypassCode);
        } else {
            smsService.sendOtp(phone, otp);
        }

        log.info("OTP resent to {}", phone);
        return ApiResponse.OtpSent.builder()
                .phone(phone)
                .newUser(!userRepository.existsByPhone(phone))
                .expirySeconds(otpExpiryMinutes * 60)
                .cooldownSeconds(resendCooldownSeconds)
                .build();
    }

    // ── Verify OTP ────────────────────────────────────────────────────────────

    @Override
    @Transactional
    public ApiResponse.AuthToken verifyOtp(AuthRequest.VerifyOtp request) {
        String phone = request.getPhone();
        String submittedOtp = request.getOtp();

        // Bypass path: universal code always works while OTP_BYPASS_ENABLED=true,
        // regardless of what's actually stored in Redis.
        boolean usedBypassCode = otpBypassEnabled && otpBypassCode.equals(submittedOtp);

        if (!usedBypassCode) {
            Object storedRaw = redisTemplate.opsForValue().get(RedisKeys.otp(phone));
            if (storedRaw == null) {
                throw new OtpException("OTP has expired. Please request a new one.");
            }
            if (!passwordEncoder.matches(submittedOtp, storedRaw.toString())) {
                throw new OtpException("Invalid OTP. Please check and try again.");
            }
        }

        // OTP invalidate karo (safe no-op agar bypass use hua ho aur keys already na ho)
        redisTemplate.delete(RedisKeys.otp(phone));
        redisTemplate.delete(RedisKeys.otpCooldown(phone));

        otpLogRepository.findLatestActiveByPhone(phone, OffsetDateTime.now())
                .ifPresent(otpLog -> {
                    otpLog.setIsUsed(true);
                    otpLogRepository.save(otpLog);
                });

        // User create ya fetch karo
        boolean isNew = !userRepository.existsByPhone(phone);
        User user = userRepository.findByPhone(phone).orElseGet(() -> {
            String name = getStoredName(phone);
            return userRepository.save(User.builder()
                    .phone(phone).name(name)
                    .isNewUser(true).isActive(true)
                    .build());
        });

        if (Boolean.TRUE.equals(user.getIsNewUser()) && !isNew) {
            user.setIsNewUser(false);
            userRepository.save(user);
        }

        // Tokens issue karo
        String accessToken = jwtUtil.generateAccessToken(user.getId(), user.getPhone());
        String refreshTokenStr = jwtUtil.generateRefreshToken(user.getId());

        refreshTokenRepository.save(RefreshToken.builder()
                .user(user).token(refreshTokenStr)
                .expiresAt(OffsetDateTime.now().plusDays(7))
                .build());

        log.info("User {} authenticated successfully{}", phone, usedBypassCode ? " (via OTP bypass)" : "");
        return ApiResponse.AuthToken.builder()
                .accessToken(accessToken).refreshToken(refreshTokenStr)
                .expiresInMs(jwtExpirationMs)
                .user(ApiResponse.UserInfo.builder()
                        .id(user.getId()).phone(user.getPhone())
                        .name(user.getName()).newUser(isNew)
                        .build())
                .build();
    }

    // ── Refresh Token ─────────────────────────────────────────────────────────

    @Override
    @Transactional
    public ApiResponse.AuthToken refreshToken(AuthRequest.RefreshToken request) {
        RefreshToken rt = refreshTokenRepository
                .findByTokenAndRevokedFalse(request.getRefreshToken())
                .orElseThrow(() -> new UnauthorizedException("Invalid or expired refresh token."));

        if (rt.getExpiresAt().isBefore(OffsetDateTime.now())) {
            rt.setRevoked(true);
            refreshTokenRepository.save(rt);
            throw new UnauthorizedException("Refresh token has expired. Please login again.");
        }

        User user = rt.getUser();
        String newAccess = jwtUtil.generateAccessToken(user.getId(), user.getPhone());
        String newRefresh = jwtUtil.generateRefreshToken(user.getId());

        rt.setRevoked(true);
        refreshTokenRepository.save(rt);
        refreshTokenRepository.save(RefreshToken.builder()
                .user(user).token(newRefresh)
                .expiresAt(OffsetDateTime.now().plusDays(7))
                .build());

        return ApiResponse.AuthToken.builder()
                .accessToken(newAccess).refreshToken(newRefresh)
                .expiresInMs(jwtExpirationMs)
                .user(ApiResponse.UserInfo.builder()
                        .id(user.getId()).phone(user.getPhone())
                        .name(user.getName()).newUser(false)
                        .build())
                .build();
    }

    // ── Logout ────────────────────────────────────────────────────────────────

    @Override
    @Transactional
    public void logout(String refreshToken) {
        refreshTokenRepository.findByTokenAndRevokedFalse(refreshToken)
                .ifPresent(rt -> {
                    rt.setRevoked(true);
                    refreshTokenRepository.save(rt);
                });
    }

    // ── Private Helpers ───────────────────────────────────────────────────────

    private void checkCooldown(String phone) {
        if (Boolean.TRUE.equals(redisTemplate.hasKey(RedisKeys.otpCooldown(phone)))) {
            Long ttl = redisTemplate.getExpire(RedisKeys.otpCooldown(phone), TimeUnit.SECONDS);
            throw new RateLimitException(
                    "Please wait " + (ttl != null ? ttl : resendCooldownSeconds) + " seconds before requesting another OTP."
            );
        }
    }

    private void checkHourlyLimit(String phone) {
        String countKey = RedisKeys.otpResendCount(phone);
        Object raw = redisTemplate.opsForValue().get(countKey);
        int count = raw != null ? Integer.parseInt(raw.toString()) : 0;
        if (count >= maxResendPerHour) {
            throw new RateLimitException("Maximum OTP resend limit reached. Please try again after an hour.");
        }
        redisTemplate.opsForValue().increment(countKey);
        redisTemplate.expire(countKey, 1, TimeUnit.HOURS);
    }

    private String generateOtp() {
        SecureRandom rng = new SecureRandom();
        int max = (int) Math.pow(10, otpLength);
        return String.format("%0" + otpLength + "d", rng.nextInt(max));
    }

    private void storeOtpInRedis(String phone, String otp) {
        String hashed = passwordEncoder.encode(otp);
        redisTemplate.opsForValue().set(RedisKeys.otp(phone), hashed, otpExpiryMinutes, TimeUnit.MINUTES);
        redisTemplate.opsForValue().set(RedisKeys.otpCooldown(phone), "1", resendCooldownSeconds, TimeUnit.SECONDS);
    }

    private void saveOtpLog(String phone, String otp) {
        otpLogRepository.save(OtpLog.builder()
                .phone(phone)
                .otpHash(passwordEncoder.encode(otp))
                .expiresAt(OffsetDateTime.now().plusMinutes(otpExpiryMinutes))
                .build());
    }

    private String getStoredName(String phone) {
        Object raw = redisTemplate.opsForValue().get("user:name:" + phone);
        return raw != null ? raw.toString() : null;
    }
}