package com.newtonite.ops.user;

import java.time.Instant;
import java.util.List;

public record UserProfileResponse(
    Long id,
    String name,
    String email,
    boolean enabled,
    Instant createdAt,
    List<UserTeamMembershipDTO> memberships
) {}
