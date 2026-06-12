package com.selfstudy.modules.reservation;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.dao.DataAccessException;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.util.Objects;

/**
 * 自习室座位短时占位（防连点 / 并发提交）：Redis SET key NX + EX。
 * <p>
 * 已在 {@link com.selfstudy.modules.bas.service.impl.BasAppointmentServiceImpl#appointment} 中与 Bitmap 预约串联：
 * {@code seatDay} 键段为 {@code bizDate:slotStart:slotEnd}，流程结束在 {@code finally} 中 {@link #releaseHold}。
 * 可通过 {@code study.seat-lock.enabled=false} 关闭本 Bean。
 * </p>
 */
@Service
@ConditionalOnProperty(name = "study.seat-lock.enabled", havingValue = "true", matchIfMissing = true)
public class StudySeatLockService {

    private static final String KEY_PREFIX = "study:seat:hold:";

    private final Logger log = LoggerFactory.getLogger(getClass());

    private final StringRedisTemplate stringRedisTemplate;

    @Value("${study.seat-lock.ttl-seconds:300}")
    private int ttlSeconds;

    public StudySeatLockService(StringRedisTemplate stringRedisTemplate) {
        this.stringRedisTemplate = stringRedisTemplate;
    }

    private String buildKey(Long seatId, String seatDay) {
        return KEY_PREFIX + Objects.requireNonNull(seatDay, "seatDay") + ":" + seatId;
    }

    /**
     * 尝试占位：同一 seatDay + seatId 在 TTL 内只允许一个 userId 成功。
     *
     * @return true 占位成功；false 已被他人占位
     */
    public boolean tryHold(Long seatId, String seatDay, Long userId) {
        if (seatId == null || userId == null) {
            return false;
        }
        String key = buildKey(seatId, seatDay);
        String value = String.valueOf(userId);
        try {
            Boolean ok = stringRedisTemplate.opsForValue()
                    .setIfAbsent(key, value, Duration.ofSeconds(Math.max(1, ttlSeconds)));
            return Boolean.TRUE.equals(ok);
        } catch (DataAccessException e) {
            log.error("Redis seat hold failed, key={}", key, e);
            throw e;
        }
    }

    /**
     * 仅当占位值为当前用户时删除，避免误删他人锁。
     */
    public void releaseHold(Long seatId, String seatDay, Long userId) {
        if (seatId == null || userId == null || seatDay == null) {
            return;
        }
        String key = buildKey(seatId, seatDay);
        try {
            String current = stringRedisTemplate.opsForValue().get(key);
            if (String.valueOf(userId).equals(current)) {
                stringRedisTemplate.delete(key);
            }
        } catch (DataAccessException e) {
            log.warn("Redis seat release failed, key={}", key, e);
        }
    }
}
