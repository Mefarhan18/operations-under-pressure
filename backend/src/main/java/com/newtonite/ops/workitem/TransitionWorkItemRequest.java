package com.newtonite.ops.workitem;

import jakarta.validation.constraints.NotNull;

public record TransitionWorkItemRequest(
    @NotNull(message = "Target status is required")
    WorkItemStatus targetStatus,

    @NotNull(message = "Version is required for concurrency control")
    Long version
) {}
