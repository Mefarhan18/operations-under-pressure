package com.newtonite.ops;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.newtonite.ops.activity.ActivityEventRepository;
import com.newtonite.ops.comment.CommentRepository;
import com.newtonite.ops.security.JwtService;
import com.newtonite.ops.team.*;
import com.newtonite.ops.user.User;
import com.newtonite.ops.user.UserRepository;
import com.newtonite.ops.workitem.*;
import com.newtonite.ops.workitem.idempotency.IdempotentRequest;
import com.newtonite.ops.workitem.idempotency.IdempotentRequestRepository;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import java.time.Instant;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.*;
import java.util.concurrent.*;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
class IdempotencyIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

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
    private IdempotentRequestRepository idempotentRequestRepository;

    @Autowired
    private WorkItemService workItemService;

    @Autowired
    private JwtService jwtService;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Autowired
    private ObjectMapper objectMapper;

    private User operator;
    private Team team;
    private String token;

    @BeforeEach
    void setUp() {
        commentRepository.deleteAll();
        activityEventRepository.deleteAll();
        workItemRepository.deleteAll();
        idempotentRequestRepository.deleteAll();
        membershipRepository.deleteAll();
        userRepository.deleteAll();
        teamRepository.deleteAll();

        team = teamRepository.save(new Team("Platform Infrastructure", "Infra & Cloud"));
        operator = userRepository.save(new User("DevOps Dave", "dave@infra.internal", passwordEncoder.encode("Pass123!")));
        membershipRepository.save(new Membership(operator, team, MembershipRole.MEMBER));
        token = jwtService.generateToken(operator);
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
    void testRepeatedRequestWithSameKeyAndPayloadReturnsIdenticalResponseWithoutCreatingDuplicate() throws Exception {
        String idempotencyKey = UUID.randomUUID().toString();
        CreateWorkItemRequest request = new CreateWorkItemRequest(
                "Kubernetes Cluster Upgrade Failure",
                "Node pool 3 failed to drain pods",
                WorkItemType.INCIDENT,
                WorkItemPriority.URGENT,
                team.getId(),
                LocalDate.now().plusDays(1)
        );

        // First call
        MvcResult firstResult = mockMvc.perform(post("/api/items")
                .header("Authorization", "Bearer " + token)
                .header("Idempotency-Key", idempotencyKey)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").exists())
                .andExpect(jsonPath("$.title").value("Kubernetes Cluster Upgrade Failure"))
                .andReturn();

        WorkItemResponse firstResponse = objectMapper.readValue(
                firstResult.getResponse().getContentAsString(),
                WorkItemResponse.class
        );

        assertThat(workItemRepository.count()).isEqualTo(1L);
        assertThat(idempotentRequestRepository.count()).isEqualTo(1L);

        // Second call with same key and same payload
        MvcResult secondResult = mockMvc.perform(post("/api/items")
                .header("Authorization", "Bearer " + token)
                .header("Idempotency-Key", idempotencyKey)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andReturn();

        WorkItemResponse secondResponse = objectMapper.readValue(
                secondResult.getResponse().getContentAsString(),
                WorkItemResponse.class
        );

        // Both responses should have identical item IDs and properties
        assertThat(secondResponse.id()).isEqualTo(firstResponse.id());
        assertThat(secondResponse.title()).isEqualTo(firstResponse.title());
        assertThat(secondResponse.teamId()).isEqualTo(firstResponse.teamId());

        // Database must STILL have only 1 work item (no duplicate creation!)
        assertThat(workItemRepository.count()).isEqualTo(1L);
    }

    @Test
    void testReusingKeyWithDifferentPayloadIsRejectedWithConflict() throws Exception {
        String idempotencyKey = UUID.randomUUID().toString();
        CreateWorkItemRequest request1 = new CreateWorkItemRequest(
                "Original Item",
                "Description 1",
                WorkItemType.TASK,
                WorkItemPriority.LOW,
                team.getId(),
                LocalDate.now().plusDays(2)
        );

        // First call succeeds
        mockMvc.perform(post("/api/items")
                .header("Authorization", "Bearer " + token)
                .header("Idempotency-Key", idempotencyKey)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request1)))
                .andExpect(status().isCreated());

        // Second call with SAME idempotency key but DIFFERENT payload
        CreateWorkItemRequest request2 = new CreateWorkItemRequest(
                "Modified Item Payload",
                "Different Description",
                WorkItemType.COMPLIANCE,
                WorkItemPriority.URGENT,
                team.getId(),
                LocalDate.now().plusDays(5)
        );

        mockMvc.perform(post("/api/items")
                .header("Authorization", "Bearer " + token)
                .header("Idempotency-Key", idempotencyKey)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request2)))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.status").value(409))
                .andExpect(jsonPath("$.error").value("Conflict"))
                .andExpect(jsonPath("$.message").value(org.hamcrest.Matchers.containsString("previously used with a different request payload")));

        // Only 1 item exists in DB
        assertThat(workItemRepository.count()).isEqualTo(1L);
    }

    @Test
    void testRequestWithoutIdempotencyKeyCreatesNormally() throws Exception {
        CreateWorkItemRequest request = new CreateWorkItemRequest(
                "Non-idempotent item",
                "Standard creation",
                WorkItemType.TASK,
                WorkItemPriority.MEDIUM,
                team.getId(),
                null
        );

        mockMvc.perform(post("/api/items")
                .header("Authorization", "Bearer " + token)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").exists());

        assertThat(workItemRepository.count()).isEqualTo(1L);
        assertThat(idempotentRequestRepository.count()).isEqualTo(0L);
    }

    @Test
    void testSimultaneousRequestsWithSameKeyResultInSingleItemCreated() throws Exception {
        String idempotencyKey = UUID.randomUUID().toString();
        CreateWorkItemRequest request = new CreateWorkItemRequest(
                "Concurrent Creation Test",
                "Testing multithreaded deduplication",
                WorkItemType.INCIDENT,
                WorkItemPriority.HIGH,
                team.getId(),
                LocalDate.now().plusDays(1)
        );

        int threads = 3;
        ExecutorService executor = Executors.newFixedThreadPool(threads);
        CountDownLatch startLatch = new CountDownLatch(1);
        CountDownLatch doneLatch = new CountDownLatch(threads);

        List<WorkItemResponse> results = Collections.synchronizedList(new ArrayList<>());
        List<Throwable> errors = Collections.synchronizedList(new ArrayList<>());

        for (int i = 0; i < threads; i++) {
            executor.submit(() -> {
                try {
                    startLatch.await();
                    WorkItemResponse resp = workItemService.createItem(operator, request, idempotencyKey);
                    results.add(resp);
                } catch (Throwable t) {
                    errors.add(t);
                } finally {
                    doneLatch.countDown();
                }
            });
        }

        // Release all threads simultaneously
        startLatch.countDown();
        boolean completed = doneLatch.await(5, TimeUnit.SECONDS);
        executor.shutdown();

        assertThat(completed).isTrue();
        // Database must have exactly 1 work item created!
        assertThat(workItemRepository.count()).isEqualTo(1L);

        // All successful thread responses must point to that exact same item
        assertThat(results).isNotEmpty();
        Long canonicalId = workItemRepository.findAll().get(0).getId();
        for (WorkItemResponse r : results) {
            assertThat(r.id()).isEqualTo(canonicalId);
        }
    }

    @Test
    void testRetentionPolicyPurgesExpiredKeys() {
        // Create an expired record directly
        IdempotentRequest expired = new IdempotentRequest(
                "expired-key-123",
                operator,
                "dummyhash",
                100L,
                201,
                "{}",
                Instant.now().minus(2, ChronoUnit.HOURS)
        );
        idempotentRequestRepository.save(expired);

        // Create an active record
        IdempotentRequest active = new IdempotentRequest(
                "active-key-456",
                operator,
                "dummyhash",
                101L,
                201,
                "{}",
                Instant.now().plus(24, ChronoUnit.HOURS)
        );
        idempotentRequestRepository.save(active);

        assertThat(idempotentRequestRepository.count()).isEqualTo(2L);

        // Purge expired keys
        int purged = workItemService.purgeExpiredIdempotencyKeys();
        assertThat(purged).isEqualTo(1);

        List<IdempotentRequest> remaining = idempotentRequestRepository.findAll();
        assertThat(remaining).hasSize(1);
        assertThat(remaining.get(0).getIdempotencyKey()).isEqualTo("active-key-456");
    }
}
