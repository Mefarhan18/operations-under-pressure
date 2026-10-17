package com.newtonite.ops.activity;

import java.time.Instant;

public record ActivityEventResponse(
    Long id,
    Long workItemId,
    Long actorId,
    String actorName,
    String actorEmail,
    String eventType,
    String oldValue,
    String newValue,
    Instant createdAt
) {
    public static ActivityEventResponse fromEntity(ActivityEvent a) {
        return new ActivityEventResponse(
            a.getId(),
            a.getWorkItem().getId(),
            a.getActor().getId(),
            a.getActor().getName(),
            a.getActor().getEmail(),
            a.getEventType(),
            a.getOldValue(),
            a.getNewValue(),
            a.getCreatedAt()
        );
    }
}
