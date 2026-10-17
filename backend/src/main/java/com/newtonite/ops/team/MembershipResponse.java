package com.newtonite.ops.team;

import java.time.Instant;

public record MembershipResponse(
    Long id,
    Long userId,
    String userName,
    String userEmail,
    Long teamId,
    String teamName,
    MembershipRole role,
    Instant createdAt
) {
    public static MembershipResponse fromEntity(Membership m) {
        return new MembershipResponse(
            m.getId(),
            m.getUser().getId(),
            m.getUser().getName(),
            m.getUser().getEmail(),
            m.getTeam().getId(),
            m.getTeam().getName(),
            m.getRole(),
            m.getCreatedAt()
        );
    }
}
