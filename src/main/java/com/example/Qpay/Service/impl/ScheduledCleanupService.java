package com.example.Qpay.Service.impl;
import com.example.Qpay.Entity.ShoppingSession;
import com.example.Qpay.enums.SessionStatus;
import com.example.Qpay.Repository.OtpLogRepository;
import com.example.Qpay.Repository.ShoppingSessionRepository;
import com.example.Qpay.Util.RedisKeys;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.OffsetDateTime;
import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class ScheduledCleanupService {

    private final OtpLogRepository otpLogRepository;
    private final ShoppingSessionRepository sessionRepository;
    private final RedisTemplate<String, Object> redisTemplate;

    /**
     * Delete expired OTP log entries every hour.
     */
    @Scheduled(fixedDelay = 3600_000, initialDelay = 60_000)
    @Transactional
    public void cleanExpiredOtpLogs() {
        try {
            OffsetDateTime threshold = OffsetDateTime.now().minusHours(24);
            List<com.example.Qpay.Entity.OtpLog> expired = otpLogRepository.findAll().stream()
                    .filter(o -> o.getExpiresAt().isBefore(threshold))
                    .toList();
            if (!expired.isEmpty()) {
                otpLogRepository.deleteAll(expired);
                log.info("Cleaned {} expired OTP log entries", expired.size());
            }
        } catch (Exception e) {
            log.error("OTP cleanup failed: {}", e.getMessage());
        }
    }

    /**
     * Mark shopping sessions abandoned if cart Redis key has expired but session is still ACTIVE.
     * Runs every 30 minutes.
     */
    @Scheduled(fixedDelay = 1_800_000, initialDelay = 120_000)
    @Transactional
    public void cleanAbandonedSessions() {
        try {
            // Find ACTIVE sessions older than 25 hours (cart TTL is 24h)
            OffsetDateTime cutoff = OffsetDateTime.now().minusHours(25);
            List<ShoppingSession> stale = sessionRepository.findAll().stream()
                    .filter(s -> s.getStatus() == SessionStatus.ACTIVE)
                    .filter(s -> s.getStartedAt().isBefore(cutoff))
                    .toList();

            for (ShoppingSession session : stale) {
                session.setStatus(SessionStatus.ABANDONED);
                session.setEndedAt(OffsetDateTime.now());
                sessionRepository.save(session);

                // Clean up any lingering Redis keys
                String cartKey = RedisKeys.cart(session.getId());
                redisTemplate.delete(cartKey);
            }

            if (!stale.isEmpty()) {
                log.info("Marked {} shopping sessions as ABANDONED", stale.size());
            }
        } catch (Exception e) {
            log.error("Session cleanup failed: {}", e.getMessage());
        }
    }
}
