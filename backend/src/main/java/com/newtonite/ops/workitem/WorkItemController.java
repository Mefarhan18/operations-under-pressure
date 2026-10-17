package com.newtonite.ops.workitem;

import com.newtonite.ops.activity.ActivityEventResponse;
import com.newtonite.ops.comment.CommentResponse;
import com.newtonite.ops.comment.CreateCommentRequest;
import com.newtonite.ops.common.PagedResponse;
import com.newtonite.ops.security.AuthHelper;
import com.newtonite.ops.user.User;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping
public class WorkItemController {

    private final WorkItemService workItemService;
    private final AuthHelper authHelper;

    public WorkItemController(WorkItemService workItemService, AuthHelper authHelper) {
        this.workItemService = workItemService;
        this.authHelper = authHelper;
    }

    @GetMapping({"/api/items", "/api/v1/work-items"})
    public ResponseEntity<PagedResponse<WorkItemResponse>> getItems(
            Authentication authentication,
            @RequestParam(required = false) Long teamId,
            @RequestParam(required = false) WorkItemStatus status,
            @RequestParam(required = false) WorkItemPriority priority,
            @RequestParam(required = false) Long assigneeId,
            @RequestParam(required = false) String search,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(defaultValue = "createdAt") String sortBy,
            @RequestParam(defaultValue = "desc") String sortDir
    ) {
        User user = authHelper.getCurrentUser(authentication);
        PagedResponse<WorkItemResponse> response = workItemService.getItems(
                user, teamId, status, priority, assigneeId, search, page, size, sortBy, sortDir
        );
        return ResponseEntity.ok(response);
    }

    @PostMapping({"/api/items", "/api/v1/work-items"})
    public ResponseEntity<WorkItemResponse> createItem(
            Authentication authentication,
            @RequestHeader(value = "Idempotency-Key", required = false) String idempotencyKey,
            @Valid @RequestBody CreateWorkItemRequest request
    ) {
        User user = authHelper.getCurrentUser(authentication);
        WorkItemResponse response = workItemService.createItem(user, request, idempotencyKey);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @GetMapping({"/api/items/{id}", "/api/v1/work-items/{id}"})
    public ResponseEntity<WorkItemResponse> getItemById(
            Authentication authentication,
            @PathVariable Long id
    ) {
        User user = authHelper.getCurrentUser(authentication);
        WorkItemResponse response = workItemService.getItemById(user, id);
        return ResponseEntity.ok(response);
    }

    @PatchMapping({"/api/items/{id}", "/api/v1/work-items/{id}"})
    public ResponseEntity<WorkItemResponse> updateItem(
            Authentication authentication,
            @PathVariable Long id,
            @Valid @RequestBody UpdateWorkItemRequest request
    ) {
        User user = authHelper.getCurrentUser(authentication);
        WorkItemResponse response = workItemService.updateItem(user, id, request);
        return ResponseEntity.ok(response);
    }

    @PostMapping({"/api/items/{id}/claim", "/api/v1/work-items/{id}/claim"})
    public ResponseEntity<WorkItemResponse> claimItem(
            Authentication authentication,
            @PathVariable Long id
    ) {
        User user = authHelper.getCurrentUser(authentication);
        WorkItemResponse response = workItemService.claimItem(user, id);
        return ResponseEntity.ok(response);
    }

    @PostMapping({"/api/items/{id}/transition", "/api/v1/work-items/{id}/transition"})
    public ResponseEntity<WorkItemResponse> transitionItem(
            Authentication authentication,
            @PathVariable Long id,
            @Valid @RequestBody TransitionWorkItemRequest request
    ) {
        User user = authHelper.getCurrentUser(authentication);
        WorkItemResponse response = workItemService.transitionItem(user, id, request);
        return ResponseEntity.ok(response);
    }

    @GetMapping({"/api/items/{id}/activity", "/api/v1/work-items/{id}/activity"})
    public ResponseEntity<List<ActivityEventResponse>> getItemActivity(
            Authentication authentication,
            @PathVariable Long id
    ) {
        User user = authHelper.getCurrentUser(authentication);
        List<ActivityEventResponse> response = workItemService.getItemActivity(user, id);
        return ResponseEntity.ok(response);
    }

    @PostMapping({"/api/items/{id}/comments", "/api/v1/work-items/{id}/comments"})
    public ResponseEntity<CommentResponse> addComment(
            Authentication authentication,
            @PathVariable Long id,
            @Valid @RequestBody CreateCommentRequest request
    ) {
        User user = authHelper.getCurrentUser(authentication);
        CommentResponse response = workItemService.addComment(user, id, request);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @GetMapping({"/api/items/{id}/comments", "/api/v1/work-items/{id}/comments"})
    public ResponseEntity<List<CommentResponse>> getItemComments(
            Authentication authentication,
            @PathVariable Long id
    ) {
        User user = authHelper.getCurrentUser(authentication);
        List<CommentResponse> response = workItemService.getItemComments(user, id);
        return ResponseEntity.ok(response);
    }

    @GetMapping({"/api/items/summary", "/api/v1/work-items/summary"})
    public ResponseEntity<WorkItemSummaryResponse> getSummary(Authentication authentication) {
        User user = authHelper.getCurrentUser(authentication);
        WorkItemSummaryResponse response = workItemService.getSummary(user);
        return ResponseEntity.ok(response);
    }
}
