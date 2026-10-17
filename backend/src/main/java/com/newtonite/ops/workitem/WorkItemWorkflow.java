package com.newtonite.ops.workitem;

public class WorkItemWorkflow {

    public static boolean isValidTransition(WorkItemStatus current, WorkItemStatus target) {
        if (current == null || target == null) {
            return false;
        }
        return switch (current) {
            case OPEN -> target == WorkItemStatus.IN_PROGRESS;
            case IN_PROGRESS -> target == WorkItemStatus.AWAITING_APPROVAL;
            case AWAITING_APPROVAL -> target == WorkItemStatus.RESOLVED;
            case RESOLVED -> target == WorkItemStatus.CLOSED;
            case CLOSED -> false;
        };
    }

    public static boolean isApprovalTransition(WorkItemStatus current, WorkItemStatus target) {
        return current == WorkItemStatus.AWAITING_APPROVAL && target == WorkItemStatus.RESOLVED;
    }

    public static String getAllowedTransitionsMessage() {
        return "Allowed transitions: OPEN -> IN_PROGRESS, IN_PROGRESS -> AWAITING_APPROVAL, " +
               "AWAITING_APPROVAL -> RESOLVED (requires LEAD or ADMIN role), RESOLVED -> CLOSED.";
    }
}
