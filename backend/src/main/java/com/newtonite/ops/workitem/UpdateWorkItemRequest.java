package com.newtonite.ops.workitem;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.time.LocalDate;

public record UpdateWorkItemRequest(
    @NotNull(message = "Version is required for concurrency control")
    Long version,

    @Size(max = 255, message = "Title cannot exceed 255 characters")
    String title,

    @Size(max = 5000, message = "Description cannot exceed 5000 characters")
    String description,

    WorkItemPriority priority,

    LocalDate dueDate
) {}
