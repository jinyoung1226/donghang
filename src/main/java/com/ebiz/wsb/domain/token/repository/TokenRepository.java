package com.ebiz.wsb.domain.token.repository;

import java.util.concurrent.TimeUnit;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.ValueOperations;
import org.springframework.stereotype.Repository;

@Repository
public class TokenRepository {
    private final long REFRESH_TOKEN_EXPIRE = 60 * 60 * 24 * 15;
    private final String REFRESH_TOKEN_PREFIX = "refreshToken:";

    private RedisTemplate<String, String> redisTemplate;
    private ValueOperations<String, String> valueOperations;

    public TokenRepository(RedisTemplate<String, String> redisTemplate) {
        this.redisTemplate = redisTemplate;
        this.valueOperations = redisTemplate.opsForValue();
    }

    public void save(Long guardianId, String refreshToken) {
        valueOperations.set(REFRESH_TOKEN_PREFIX + guardianId, refreshToken);
        redisTemplate.expire(REFRESH_TOKEN_PREFIX + guardianId, REFRESH_TOKEN_EXPIRE, TimeUnit.SECONDS);
    }

    public String findById(Long guardianId) {
        return valueOperations.get(REFRESH_TOKEN_PREFIX + guardianId);
    }

    public void deleteId(Long guardianId) {
        redisTemplate.delete(REFRESH_TOKEN_PREFIX + guardianId);
    }
}
