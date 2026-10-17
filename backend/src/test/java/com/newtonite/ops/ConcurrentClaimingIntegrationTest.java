package com.newtonite.ops;

import com.newtonite.ops.activity.ActivityEvent;
import com.newtonite.ops.activity.ActivityEventRepository;
import com.newtonite.ops.comment.CommentRepository;
import com.newtonite.ops.common.ConflictException;
import com.newtonite.ops.team.*;
import com.newtonite.ops.user.User;
import com.newtonite.ops.user.UserRepository;
import com.newtonite.ops.workitem.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.*;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
class ConcurrentClaimingIntegrationTest {

    @Autowired
    private WorkItemService workItemService;

    @Autowired
    private WorkItemRepository workItemRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private TeamRepository teamRepository;

    @Autowired
    private MembershipRepository membershipRepository;

    @Autowired
    private ActivityEventRepository activityEventRepository;

    @Autowired
    private CommentRepository commentRepository;

    @Autowired
    private com.newtonite.ops.workitem.idempotency.IdempotentRequestRepository idempotentRequestRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    private User operatorA;
    private User operatorB;
    private Team team;
    private Long workItemId;

    @BeforeEach
    void setUp() {
        commentRepository.deleteAll();
        activityEventRepository.deleteAll();
        workItemRepository.deleteAll();
        idempotentRequestRepository.deleteAll();
        membershipRepository.deleteAll();
        userRepository.deleteAll();
        teamRepository.deleteAll();

        team = teamRepository.save(new Team("Fraud & Risk Ops", "Fraud"));

        operatorA = userRepository.save(new User("Operator Alice", "alice.claim@newtonite.internal", passwordEncoder.encode("Pass123!")));
        operatorB = userRepository.save(new User("Operator Bob", "bob.claim@newtonite.internal", passwordEncoder.encode("Pass123!")));

        membershipRepository.save(new Membership(operatorA, team, MembershipRole.MEMBER));
        membershipRepository.save(new Membership(operatorB, team, MembershipRole.MEMBER));

        WorkItem unassignedItem = workItemRepository.save(new WorkItem(
                "Suspicious Card Velocity Spike",
                "Automated fraud trigger 4049",
                WorkItemType.INCIDENT,
                WorkItemPriority.URGENT,
                WorkItemStatus.OPEN,
                team,
                null, // Unassigned
                operatorA,
                LocalDate.now().plusDays(1)
        ));

        workItemId = unassignedItem.getId();
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
    void testTwoSimultaneousClaimRequestsResultInOnlyOneSuccess() throws Exception {
        ExecutorService executor = Executors.newFixedThreadPool(2);
        CountDownLatch startLatch = new CountDownLatch(1);
        CountDownLatch doneLatch = new CountDownLatch(2);

        List<WorkItemResponse> successes = Collections.synchronizedList(new ArrayList<>());
        List<Throwable> failures = Collections.synchronizedList(new ArrayList<>());

        // Thread 1: Operator A attempts to claim
        executor.submit(() -> {
            try {
                startLatch.await();
                WorkItemResponse res = workItemService.claimItem(operatorA, workItemId);
                successes.add(res);
            } catch (Throwable t) {
                failures.add(t);
            } finally {
                doneLatch.countDown();
            }
        });

        // Thread 2: Operator B attempts to claim at the exact same moment
        executor.submit(() -> {
            try {
                startLatch.await();
                WorkItemResponse res = workItemService.claimItem(operatorB, workItemId);
                successes.add(res);
            } catch (Throwable t) {
                failures.add(t);
            } finally {
                doneLatch.countDown();
            }
        });

        // Trigger both threads simultaneously
        startLatch.countDown();
        boolean finishedInTime = doneLatch.await(5, TimeUnit.SECONDS);
        executor.shutdown();

        assertThat(finishedInTime).isTrue();

        // Exactly one must succeed and exactly one must fail with ConflictException
        assertThat(successes).hasSize(1);
        assertThat(failures).hasSize(1);
        assertThat(failures.get(0)).isInstanceOf(ConflictException.class);
        assertThat(failures.get(0).getMessage()).contains("already been claimed");

        // Verify database state: item has single assignee and incremented version
        WorkItem finalItem = workItemRepository.findById(workItemId).orElseThrow();
        assertThat(finalItem.getAssignee()).isNotNull();
        assertThat(finalItem.getVersion()).isEqualTo(1L);

        // Verify activity history has exactly 1 CLAIMED event
        List<ActivityEvent> events = activityEventRepository.findByWorkItemIdOrderByCreatedAtDesc(workItemId);
        List<ActivityEvent> claimEvents = events.stream()
                .filter(e -> "CLAIMED".equals(e.getEventType()))
                .toList();
        assertThat(claimEvents).hasSize(1);
    }
}
