package com.newtonite.ops;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.newtonite.ops.activity.ActivityEventRepository;
import com.newtonite.ops.comment.CommentRepository;
import com.newtonite.ops.security.JwtService;
import com.newtonite.ops.team.*;
import com.newtonite.ops.user.User;
import com.newtonite.ops.user.UserRepository;
import com.newtonite.ops.workitem.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.web.servlet.MockMvc;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.*;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
class StaleVersionRejectionTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private WorkItemService workItemService;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private TeamRepository teamRepository;

    @Autowired
    private MembershipRepository membershipRepository;

    @Autowired
    private WorkItemRepository workItemRepository;

    @Autowired
    private ActivityEventRepository activityEventRepository;

    @Autowired
    private CommentRepository commentRepository;

    @Autowired
    private com.newtonite.ops.workitem.idempotency.IdempotentRequestRepository idempotentRequestRepository;

    @Autowired
    private JwtService jwtService;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Autowired
    private ObjectMapper objectMapper;

    private User operator;
    private Team team;
    private String token;
    private WorkItem workItem;

    @BeforeEach
    void setUp() {
        commentRepository.deleteAll();
        activityEventRepository.deleteAll();
        workItemRepository.deleteAll();
        idempotentRequestRepository.deleteAll();
        membershipRepository.deleteAll();
        userRepository.deleteAll();
        teamRepository.deleteAll();

        team = teamRepository.save(new Team("Payment Reliability", "Payments"));
        operator = userRepository.save(new User("Operator John", "john@payments.internal", passwordEncoder.encode("Pass123!")));
        membershipRepository.save(new Membership(operator, team, MembershipRole.MEMBER));
        token = jwtService.generateToken(operator);

        workItem = workItemRepository.save(new WorkItem(
                "Clearing Service Batch Delay",
                "Delay in batch 1002",
                WorkItemType.PAYMENT,
                WorkItemPriority.HIGH,
                WorkItemStatus.OPEN,
                team,
                null,
                operator,
                LocalDate.now().plusDays(1)
        ));
    }

    @org.junit.jupiter.api.AfterEach
    void tearDown() {
        commentRepository.deleteAll();
        activityEventRepository.deleteAll();
        workItemRepository.deleteAll();
        idempotentRequestRepository.deleteAll();
        membershipRepository.deleteAll();
        userRepository.deleteAll();
        teamRepository.deleteAll();
    }

    @Test
    void testStaleVersionRejectedOnPatch() throws Exception {
        // Current version is 0. Send request with stale version 99
        UpdateWorkItemRequest staleRequest = new UpdateWorkItemRequest(
                99L,
                "Updated Title",
                "Updated Description",
                WorkItemPriority.URGENT,
                null
        );

        mockMvc.perform(patch("/api/items/" + workItem.getId())
                .header("Authorization", "Bearer " + token)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(staleRequest)))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.status").value(409))
                .andExpect(jsonPath("$.error").value("Conflict"))
                .andExpect(jsonPath("$.message").value(org.hamcrest.Matchers.containsString("Stale update detected")));
    }

    @Test
    void testStaleVersionRejectedOnTransition() throws Exception {
        // Current version is 0. Send transition request with wrong version 5
        TransitionWorkItemRequest staleRequest = new TransitionWorkItemRequest(
                WorkItemStatus.IN_PROGRESS,
                5L
        );

        mockMvc.perform(post("/api/items/" + workItem.getId() + "/transition")
                .header("Authorization", "Bearer " + token)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(staleRequest)))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.status").value(409))
                .andExpect(jsonPath("$.message").value(org.hamcrest.Matchers.containsString("Stale update detected")));
    }

    @Test
    void testMatchingVersionSucceedsAndSubsequentStaleUpdateIsRejected() throws Exception {
        // 1. Initial update with matching version 0
        UpdateWorkItemRequest validRequest = new UpdateWorkItemRequest(
                0L,
                "Updated Title Successfully",
                "Fresh Description",
                WorkItemPriority.URGENT,
                null
        );

        mockMvc.perform(patch("/api/items/" + workItem.getId())
                .header("Authorization", "Bearer " + token)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(validRequest)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.version").value(1))
                .andExpect(jsonPath("$.title").value("Updated Title Successfully"));

        // 2. Client still holds old version 0 and tries to update again -> 409 Conflict!
        UpdateWorkItemRequest oldVersionRequest = new UpdateWorkItemRequest(
                0L,
                "Another Title that should fail",
                null,
                null,
                null
        );

        mockMvc.perform(patch("/api/items/" + workItem.getId())
                .header("Authorization", "Bearer " + token)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(oldVersionRequest)))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.status").value(409))
                .andExpect(jsonPath("$.message").value(org.hamcrest.Matchers.containsString("Stale update detected")));
    }

    @Test
    void testSimultaneousConcurrentUpdatesWithSameInitialVersion() throws Exception {
        // Both threads read version 0
        Long initialVersion = 0L;
        assertThat(workItem.getVersion()).isEqualTo(initialVersion);

        ExecutorService executor = Executors.newFixedThreadPool(2);
        CountDownLatch startLatch = new CountDownLatch(1);
        CountDownLatch doneLatch = new CountDownLatch(2);

        List<WorkItemResponse> successes = Collections.synchronizedList(new ArrayList<>());
        List<Throwable> failures = Collections.synchronizedList(new ArrayList<>());

        // Thread 1: Updates title to "Title from Thread 1"
        executor.submit(() -> {
            try {
                startLatch.await();
                UpdateWorkItemRequest req1 = new UpdateWorkItemRequest(
                        initialVersion,
                        "Title from Thread 1",
                        "Desc 1",
                        WorkItemPriority.HIGH,
                        null
                );
                WorkItemResponse res = workItemService.updateItem(operator, workItem.getId(), req1);
                successes.add(res);
            } catch (Throwable t) {
                failures.add(t);
            } finally {
                doneLatch.countDown();
            }
        });

        // Thread 2: Concurrently updates title to "Title from Thread 2" with the SAME initial version 0
        executor.submit(() -> {
            try {
                startLatch.await();
                UpdateWorkItemRequest req2 = new UpdateWorkItemRequest(
                        initialVersion,
                        "Title from Thread 2",
                        "Desc 2",
                        WorkItemPriority.URGENT,
                        null
                );
                WorkItemResponse res = workItemService.updateItem(operator, workItem.getId(), req2);
                successes.add(res);
            } catch (Throwable t) {
                failures.add(t);
            } finally {
                doneLatch.countDown();
            }
        });

        // Fire both threads simultaneously
        startLatch.countDown();
        boolean finished = doneLatch.await(5, TimeUnit.SECONDS);
        executor.shutdown();

        assertThat(finished).isTrue();

        // Exactly one update must succeed, and one must fail
        assertThat(successes).hasSize(1);
        assertThat(failures).hasSize(1);

        // Failure is either a ConflictException or OptimisticLockingFailureException
        Throwable failure = failures.get(0);
        assertThat(failure).matches(t ->
                t instanceof com.newtonite.ops.common.ConflictException ||
                t instanceof org.springframework.dao.OptimisticLockingFailureException ||
                t.getCause() instanceof org.springframework.dao.OptimisticLockingFailureException ||
                t.getMessage().contains("Stale update") ||
                t.getMessage().contains("optimistic")
        );

        // Verify database state: exactly 1 update succeeded, version is 1
        WorkItem finalItem = workItemRepository.findById(workItem.getId()).orElseThrow();
        assertThat(finalItem.getVersion()).isEqualTo(1L);

        // The winning update's data remains intact
        String winningTitle = successes.get(0).title();
        assertThat(finalItem.getTitle()).isEqualTo(winningTitle);

        // Failed operations do not create misleading success activity events
        List<com.newtonite.ops.activity.ActivityEvent> updateEvents = activityEventRepository
                .findByWorkItemIdOrderByCreatedAtDesc(workItem.getId())
                .stream()
                .filter(e -> "ITEM_UPDATED".equals(e.getEventType()))
                .toList();
        assertThat(updateEvents).hasSize(1);
    }
}
