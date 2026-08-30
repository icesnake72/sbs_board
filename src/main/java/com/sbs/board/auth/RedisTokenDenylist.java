package com.sbs.board.auth;

import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;

import java.time.Duration;

@Component
@RequiredArgsConstructor
public class RedisTokenDenylist implements TokenDenylist {
    private static final String KEY_PREFIX = "deny:";

    private final StringRedisTemplate redis;

    @Override
    public void deny(String jti, long remainingSeconds) {
        redis.opsForValue().set(KEY_PREFIX + jti, "1", Duration.ofSeconds(remainingSeconds));
    }

    @Override
    public boolean isDenied(String jti) {
        return redis.hasKey(KEY_PREFIX + jti);
    }
}
