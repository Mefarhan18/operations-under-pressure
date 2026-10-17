package com.newtonite.ops.user;

import java.time.Instant;

public record UserResponse(
    Long id,
    String name,
    String email,
    boolean enabled,
    Instant createdAt
) {
    public static UserResponse fromEntity(User user) {
        return new UserResponse(
            user.getId(),
            user.getName(),
            user.getEmail(),
            user.isEnabled(),
            user.getCreatedAt()
        );
    }
}
