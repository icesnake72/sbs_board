package com.sbs.board.auth;

public interface TokenDenylist {
    void deny(String jti, long remainingSeconds);
    boolean isDenied(String jti);
}
