package com.newtonite.ops.workitem;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.time.LocalDate;

public record CreateWorkItemRequest(
    @NotBlank(message = "Title is required")
    @Size(max = 255, message = "Title cannot exceed 255 characters")
    String title,

    @Size(max = 5000, message = "Description cannot exceed 5000 characters")
    String description,

    @NotNull(message = "Work item type is required")
    WorkItemType type,

    @NotNull(message = "Priority is required")
    WorkItemPriority priority,

    @NotNull(message = "Team ID is required")
    Long teamId,

    LocalDate dueDate
) {}
