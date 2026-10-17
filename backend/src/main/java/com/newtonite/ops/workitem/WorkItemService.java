package com.newtonite.ops.workitem;

import com.newtonite.ops.activity.ActivityEvent;
import com.newtonite.ops.activity.ActivityEventRepository;
import com.newtonite.ops.activity.ActivityEventResponse;
import com.newtonite.ops.comment.Comment;
import com.newtonite.ops.comment.CommentRepository;
import com.newtonite.ops.comment.CommentResponse;
import com.newtonite.ops.comment.CreateCommentRequest;
import com.newtonite.ops.common.ConflictException;
import com.newtonite.ops.common.PagedResponse;
import com.newtonite.ops.common.ResourceNotFoundException;
import com.newtonite.ops.security.TeamAuthorizationService;
import com.newtonite.ops.team.Team;
import com.newtonite.ops.team.TeamRepository;
import com.newtonite.ops.user.User;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.newtonite.ops.workitem.idempotency.IdempotencyHelper;
import com.newtonite.ops.workitem.idempotency.IdempotentRequest;
import com.newtonite.ops.workitem.idempotency.IdempotentRequestRepository;
import org.springframework.dao.DataIntegrityViolationException;

import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.Optional;

@Service
@Transactional(readOnly = true)
public class WorkItemService {

    private final WorkItemRepository workItemRepository;
    private final TeamRepository teamRepository;
    private final ActivityEventRepository activityEventRepository;
    private final CommentRepository commentRepository;
    private final TeamAuthorizationService teamAuthorizationService;
    private final IdempotentRequestRepository idempotentRequestRepository;
    private final ObjectMapper objectMapper;

    public WorkItemService(WorkItemRepository workItemRepository,
                           TeamRepository teamRepository,
                           ActivityEventRepository activityEventRepository,
                           CommentRepository commentRepository,
                           TeamAuthorizationService teamAuthorizationService,
                           IdempotentRequestRepository idempotentRequestRepository,
                           ObjectMapper objectMapper) {
        this.workItemRepository = workItemRepository;
        this.teamRepository = teamRepository;
        this.activityEventRepository = activityEventRepository;
        this.commentRepository = commentRepository;
        this.teamAuthorizationService = teamAuthorizationService;
        this.idempotentRequestRepository = idempotentRequestRepository;
        this.objectMapper = objectMapper;
    }

    public PagedResponse<WorkItemResponse> getItems(User currentUser,
                                                    Long teamId,
                                                    WorkItemStatus status,
                                                    WorkItemPriority priority,
                                                    Long assigneeId,
                                                    String search,
                                                    int page,
                                                    int size,
                                                    String sortBy,
                                                    String sortDir) {
        if (page < 0) {
            throw new IllegalArgumentException("Page index cannot be negative");
        }
        if (size < 1 || size > 100) {
            throw new IllegalArgumentException("Page size must be between 1 and 100");
        }

        List<Long> userTeamIds = teamAuthorizationService.getUserTeamIds(currentUser.getId());

        if (teamId != null) {
            teamAuthorizationService.requireCanView(currentUser, teamId);
        }

        Sort.Direction direction = "asc".equalsIgnoreCase(sortDir) ? Sort.Direction.ASC : Sort.Direction.DESC;
        String sortProperty = (sortBy == null || sortBy.isBlank()) ? "createdAt" : sortBy;
        Pageable pageable = PageRequest.of(page, size, Sort.by(direction, sortProperty));

        Specification<WorkItem> spec = WorkItemSpecifications.withFilters(
                userTeamIds,
                teamId,
                status,
                priority,
                assigneeId,
                search
        );

        Page<WorkItem> pageResult = workItemRepository.findAll(spec, pageable);
        return PagedResponse.fromPage(pageResult.map(WorkItemResponse::fromEntity));
    }

    public WorkItemResponse getItemById(User currentUser, Long id) {
        WorkItem item = workItemRepository.findByIdWithDetails(id)
                .orElseThrow(() -> new ResourceNotFoundException("Work item not found with ID: " + id));

        teamAuthorizationService.requireCanView(currentUser, item.getTeam().getId());
        return WorkItemResponse.fromEntity(item);
    }

    @Transactional
    public WorkItemResponse createItem(User currentUser, CreateWorkItemRequest request) {
        return createItem(currentUser, request, null);
    }

    @Transactional
    public WorkItemResponse createItem(User currentUser, CreateWorkItemRequest request, String idempotencyKey) {
        teamAuthorizationService.requireCanWrite(currentUser, request.teamId());

        String trimmedKey = (idempotencyKey != null && !idempotencyKey.isBlank()) ? idempotencyKey.trim() : null;

        // 1. Idempotency Check: Inspect if this key was already submitted by this user
        if (trimmedKey != null) {
            String requestHash = IdempotencyHelper.computeRequestFingerprint(request);
            Optional<IdempotentRequest> existing = idempotentRequestRepository
                    .findByUserIdAndIdempotencyKey(currentUser.getId(), trimmedKey);

            if (existing.isPresent()) {
                IdempotentRequest record = existing.get();
                if (!record.isExpired(Instant.now())) {
                    if (!record.getRequestHash().equals(requestHash)) {
                        throw new ConflictException("Idempotency key '" + trimmedKey +
                                "' was previously used with a different request payload. Reusing keys with modified payloads is rejected.");
                    }
                    try {
                        return objectMapper.readValue(record.getResponseBody(), WorkItemResponse.class);
                    } catch (JsonProcessingException e) {
                        return getItemById(currentUser, record.getWorkItemId());
                    }
                } else {
                    idempotentRequestRepository.delete(record);
                    idempotentRequestRepository.flush();
                }
            }
        }

        Team team = teamRepository.findById(request.teamId())
                .orElseThrow(() -> new ResourceNotFoundException("Team not found with ID: " + request.teamId()));

        WorkItem workItem = new WorkItem(
                request.title(),
                request.description(),
                request.type(),
                request.priority(),
                WorkItemStatus.OPEN,
                team,
                null,
                currentUser,
                request.dueDate()
        );

        WorkItem saved = workItemRepository.save(workItem);

        activityEventRepository.save(new ActivityEvent(
                saved,
                currentUser,
                "ITEM_CREATED",
                null,
                "Work item created with status OPEN and priority " + saved.getPriority()
        ));

        WorkItemResponse response = WorkItemResponse.fromEntity(saved);

        // 2. Idempotency Record: If key was provided, persist fingerprint and response
        if (trimmedKey != null) {
            String requestHash = IdempotencyHelper.computeRequestFingerprint(request);
            String responseJson;
            try {
                responseJson = objectMapper.writeValueAsString(response);
            } catch (JsonProcessingException e) {
                responseJson = "{}";
            }

            IdempotentRequest idempotentRecord = new IdempotentRequest(
                    trimmedKey,
                    currentUser,
                    requestHash,
                    saved.getId(),
                    201,
                    responseJson,
                    Instant.now().plus(Duration.ofHours(24))
            );

            try {
                idempotentRequestRepository.saveAndFlush(idempotentRecord);
            } catch (DataIntegrityViolationException e) {
                // Concurrent race with the same idempotency key
                Optional<IdempotentRequest> concurrentRecord = idempotentRequestRepository
                        .findByUserIdAndIdempotencyKey(currentUser.getId(), trimmedKey);
                if (concurrentRecord.isPresent()) {
                    if (concurrentRecord.get().getRequestHash().equals(requestHash)) {
                        try {
                            return objectMapper.readValue(concurrentRecord.get().getResponseBody(), WorkItemResponse.class);
                        } catch (JsonProcessingException ex) {
                            return getItemById(currentUser, concurrentRecord.get().getWorkItemId());
                        }
                    } else {
                        throw new ConflictException("Idempotency key '" + trimmedKey +
                                "' was previously used with a different request payload.");
                    }
                }
                throw new ConflictException("Concurrent duplicate request detected with idempotency key '" + trimmedKey + "'.");
            }
        }

        return response;
    }

    @Transactional
    public int purgeExpiredIdempotencyKeys() {
        return idempotentRequestRepository.deleteByExpiresAtBefore(Instant.now());
    }

    @Transactional
    public WorkItemResponse updateItem(User currentUser, Long id, UpdateWorkItemRequest request) {
        WorkItem item = workItemRepository.findByIdWithDetails(id)
                .orElseThrow(() -> new ResourceNotFoundException("Work item not found with ID: " + id));

        teamAuthorizationService.requireCanWrite(currentUser, item.getTeam().getId());

        if (!item.getVersion().equals(request.version())) {
            throw new ConflictException("Stale update detected: Work item version conflict. Current version is " +
                    item.getVersion() + ", but supplied version was " + request.version() + ". Please reload and try again.");
        }

        boolean generalFieldsChanged = false;

        if (request.title() != null && !request.title().isBlank()) {
            item.setTitle(request.title());
            generalFieldsChanged = true;
        }

        if (request.description() != null) {
            item.setDescription(request.description());
            generalFieldsChanged = true;
        }

        if (request.priority() != null && request.priority() != item.getPriority()) {
            activityEventRepository.save(new ActivityEvent(
                    item,
                    currentUser,
                    "PRIORITY_CHANGE",
                    item.getPriority().name(),
                    request.priority().name()
            ));
            item.setPriority(request.priority());
        }

        if (request.dueDate() != null) {
            item.setDueDate(request.dueDate());
            generalFieldsChanged = true;
        }

        WorkItem updated = workItemRepository.saveAndFlush(item);

        if (generalFieldsChanged) {
            activityEventRepository.save(new ActivityEvent(
                    updated,
                    currentUser,
                    "ITEM_UPDATED",
                    "Version " + request.version(),
                    "Version " + updated.getVersion()
            ));
        }

        return WorkItemResponse.fromEntity(updated);
    }

    @Transactional
    public WorkItemResponse claimItem(User currentUser, Long id) {
        WorkItem item = workItemRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Work item not found with ID: " + id));

        teamAuthorizationService.requireCanWrite(currentUser, item.getTeam().getId());

        if (item.getAssignee() != null) {
            throw new ConflictException("Work item " + id + " has already been claimed by " +
                    (item.getAssignee() != null ? item.getAssignee().getName() : "another user") + ".");
        }

        int updatedRows = workItemRepository.claimItemAtomically(id, currentUser, Instant.now());
        if (updatedRows == 0) {
            throw new ConflictException("Work item " + id + " has already been claimed by another concurrent user.");
        }

        WorkItem freshItem = workItemRepository.findByIdWithDetails(id)
                .orElseThrow(() -> new ResourceNotFoundException("Work item not found after claiming: " + id));

        activityEventRepository.save(new ActivityEvent(
                freshItem,
                currentUser,
                "CLAIMED",
                null,
                currentUser.getName()
        ));

        return WorkItemResponse.fromEntity(freshItem);
    }

    @Transactional
    public WorkItemResponse transitionItem(User currentUser, Long id, TransitionWorkItemRequest request) {
        WorkItem item = workItemRepository.findByIdWithDetails(id)
                .orElseThrow(() -> new ResourceNotFoundException("Work item not found with ID: " + id));

        teamAuthorizationService.requireCanWrite(currentUser, item.getTeam().getId());

        if (WorkItemWorkflow.isApprovalTransition(item.getStatus(), request.targetStatus())) {
            teamAuthorizationService.requireCanApprove(currentUser, item.getTeam().getId());
        }

        if (!WorkItemWorkflow.isValidTransition(item.getStatus(), request.targetStatus())) {
            throw new ConflictException("Invalid status transition from " + item.getStatus() +
                    " to " + request.targetStatus() + ". " + WorkItemWorkflow.getAllowedTransitionsMessage());
        }

        if (!item.getVersion().equals(request.version())) {
            throw new ConflictException("Stale update detected: Work item version conflict. Current version is " +
                    item.getVersion() + ", but supplied version was " + request.version() + ". Please reload and try again.");
        }

        WorkItemStatus oldStatus = item.getStatus();
        item.setStatus(request.targetStatus());

        WorkItem saved = workItemRepository.saveAndFlush(item);

        activityEventRepository.save(new ActivityEvent(
                saved,
                currentUser,
                "STATUS_TRANSITION",
                oldStatus.name(),
                request.targetStatus().name()
        ));

        return WorkItemResponse.fromEntity(saved);
    }

    public List<ActivityEventResponse> getItemActivity(User currentUser, Long id) {
        WorkItem item = workItemRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Work item not found with ID: " + id));

        teamAuthorizationService.requireCanView(currentUser, item.getTeam().getId());

        return activityEventRepository.findByWorkItemIdOrderByCreatedAtDesc(id).stream()
                .map(ActivityEventResponse::fromEntity)
                .toList();
    }

    @Transactional
    public CommentResponse addComment(User currentUser, Long id, CreateCommentRequest request) {
        WorkItem item = workItemRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Work item not found with ID: " + id));

        teamAuthorizationService.requireCanWrite(currentUser, item.getTeam().getId());

        Comment comment = new Comment(item, currentUser, request.body());
        Comment savedComment = commentRepository.save(comment);

        String preview = request.body().length() > 50
                ? request.body().substring(0, 50) + "..."
                : request.body();

        activityEventRepository.save(new ActivityEvent(
                item,
                currentUser,
                "COMMENT_ADDED",
                null,
                preview
        ));

        return CommentResponse.fromEntity(savedComment);
    }

    public List<CommentResponse> getItemComments(User currentUser, Long id) {
        WorkItem item = workItemRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Work item not found with ID: " + id));

        teamAuthorizationService.requireCanView(currentUser, item.getTeam().getId());

        return commentRepository.findByWorkItemIdOrderByCreatedAtAsc(id).stream()
                .map(CommentResponse::fromEntity)
                .toList();
    }

    public WorkItemSummaryResponse getSummary(User currentUser) {
        List<Long> userTeamIds = teamAuthorizationService.getUserTeamIds(currentUser.getId());
        if (userTeamIds.isEmpty()) {
            return new WorkItemSummaryResponse(0, 0, 0, 0);
        }

        long openCount = workItemRepository.count(
                WorkItemSpecifications.withFilters(userTeamIds, null, WorkItemStatus.OPEN, null, null, null)
        );
        long urgentCount = workItemRepository.count(
                WorkItemSpecifications.withFilters(userTeamIds, null, null, WorkItemPriority.URGENT, null, null)
        );
        long assignedToMeCount = workItemRepository.count(
                WorkItemSpecifications.withFilters(userTeamIds, null, null, null, currentUser.getId(), null)
        );
        long awaitingApprovalCount = workItemRepository.count(
                WorkItemSpecifications.withFilters(userTeamIds, null, WorkItemStatus.AWAITING_APPROVAL, null, null, null)
        );

        return new WorkItemSummaryResponse(openCount, urgentCount, assignedToMeCount, awaitingApprovalCount);
    }
}
