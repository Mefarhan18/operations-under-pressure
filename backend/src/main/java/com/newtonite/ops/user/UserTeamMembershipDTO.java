package com.newtonite.ops.user;

import com.newtonite.ops.team.MembershipRole;

public record UserTeamMembershipDTO(
    Long teamId,
    String teamName,
    MembershipRole role
) {}
