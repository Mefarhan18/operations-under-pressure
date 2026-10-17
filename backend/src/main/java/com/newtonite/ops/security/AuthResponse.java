package com.newtonite.ops.security;

public record AuthResponse(
    String token,
    String tokenType,
    Long userId,
    String name,
    String email
) {
    public static AuthResponse bearer(String token, Long userId, String name, String email) {
        return new AuthResponse(token, "Bearer", userId, name, email);
    }
}
