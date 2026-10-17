package com.newtonite.ops.workitem;

import java.time.Instant;
import java.time.LocalDate;

public record WorkItemResponse(
    Long id,
    String title,
    String description,
    WorkItemType type,
    WorkItemPriority priority,
    WorkItemStatus status,
    Long teamId,
    String teamName,
    Long assigneeId,
    String assigneeName,
    Long createdById,
    String createdByName,
    LocalDate dueDate,
    Long version,
    Instant createdAt,
    Instant updatedAt
) {
    public static WorkItemResponse fromEntity(WorkItem w) {
        return new WorkItemResponse(
            w.getId(),
            w.getTitle(),
            w.getDescription(),
            w.getType(),
            w.getPriority(),
            w.getStatus(),
            w.getTeam() != null ? w.getTeam().getId() : null,
            w.getTeam() != null ? w.getTeam().getName() : null,
            w.getAssignee() != null ? w.getAssignee().getId() : null,
            w.getAssignee() != null ? w.getAssignee().getName() : null,
            w.getCreatedBy() != null ? w.getCreatedBy().getId() : null,
            w.getCreatedBy() != null ? w.getCreatedBy().getName() : null,
            w.getDueDate(),
            w.getVersion(),
            w.getCreatedAt(),
            w.getUpdatedAt()
        );
    }
}
