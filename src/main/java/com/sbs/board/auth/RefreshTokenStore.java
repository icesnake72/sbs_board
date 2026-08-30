package com.sbs.board.auth;

import java.util.Optional;

public interface RefreshTokenStore {
    void save(Long userId, String token, long ttlSeconds);

    Optional<Long> findUserId(String token);

    void deleteByToken(String token);
}
