package com.newtonite.ops.workitem;

public record WorkItemSummaryResponse(
    long openCount,
    long urgentCount,
    long assignedToMeCount,
    long awaitingApprovalCount
) {}
