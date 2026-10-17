package com.newtonite.ops.team;

public record TeamResponse(
    Long id,
    String name,
    String description
) {
    public static TeamResponse fromEntity(Team team) {
        return new TeamResponse(team.getId(), team.getName(), team.getDescription());
    }
}
