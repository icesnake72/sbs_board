package com.sbs.board.auth;

import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.util.Optional;


@Component
@RequiredArgsConstructor
public class RedisRefreshTokenStore implements RefreshTokenStore{

    private static final String TOKEN_KEY_PREFIX = "rt:";
    private static final String USER_KEY_PREFIX = "rt:user:";

    private final StringRedisTemplate redis;

    @Override
    public void save(Long userId, String token, long ttlSeconds) {
        // 기존에 토큰이 있으면 먼저 폐기하고 저장하도록 한다
        String oldToken = redis.opsForValue().get(USER_KEY_PREFIX + userId);
        if ( oldToken != null ) {
            redis.delete(TOKEN_KEY_PREFIX + oldToken );
        }

        Duration ttl = Duration.ofSeconds(ttlSeconds);
        redis.opsForValue().set(TOKEN_KEY_PREFIX+token, String.valueOf(userId), ttl);
        redis.opsForValue().set(USER_KEY_PREFIX+userId, token, ttl);
    }

    @Override
    public Optional<Long> findUserId(String token) {
        String userId = redis.opsForValue().get(TOKEN_KEY_PREFIX + token);
        return Optional.ofNullable(userId).map(Long::valueOf);
    }

    @Override
    public void deleteByToken(String token) {
        String userId = redis.opsForValue().get(TOKEN_KEY_PREFIX + token);
        redis.delete(TOKEN_KEY_PREFIX + token);
        if (userId != null) {
            redis.delete(USER_KEY_PREFIX + userId);
        }
    }
}
