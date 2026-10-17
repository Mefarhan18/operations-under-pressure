package com.newtonite.ops;

import com.newtonite.ops.activity.ActivityEventRepository;
import com.newtonite.ops.comment.CommentRepository;
import com.newtonite.ops.team.MembershipRepository;
import com.newtonite.ops.team.Team;
import com.newtonite.ops.team.TeamRepository;
import com.newtonite.ops.user.User;
import com.newtonite.ops.user.UserRepository;
import com.newtonite.ops.workitem.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.dao.OptimisticLockingFailureException;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;

import java.time.LocalDate;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@SpringBootTest
class WorkItemOptimisticLockingTest {

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
    private PlatformTransactionManager transactionManager;

    private User testUser;
    private Team testTeam;

    @BeforeEach
    void setUp() {
        commentRepository.deleteAll();
        activityEventRepository.deleteAll();
        workItemRepository.deleteAll();
        idempotentRequestRepository.deleteAll();
        membershipRepository.deleteAll();
        userRepository.deleteAll();
        teamRepository.deleteAll();

        testUser = userRepository.save(new User("Test Auditor", "test.auditor@newtonite.internal", "hash123"));
        testTeam = teamRepository.save(new Team("Platform SRE", "Site Reliability Engineering"));
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
    void testOptimisticLockingIncrementsVersionOnUpdate() {
        TransactionTemplate tx = new TransactionTemplate(transactionManager);

        Long workItemId = tx.execute(status -> {
            WorkItem item = new WorkItem(
                    "Network Latency Degradation",
                    "Spike in gateway packet loss",
                    WorkItemType.INCIDENT,
                    WorkItemPriority.HIGH,
                    WorkItemStatus.OPEN,
                    testTeam,
                    testUser,
                    testUser,
                    LocalDate.now().plusDays(1)
            );
            return workItemRepository.save(item).getId();
        });

        assertThat(workItemId).isNotNull();

        // Verify initial version is 0
        WorkItem initial = workItemRepository.findById(workItemId).orElseThrow();
        assertThat(initial.getVersion()).isEqualTo(0L);

        // Perform update in a new transaction
        tx.executeWithoutResult(status -> {
            WorkItem itemToUpdate = workItemRepository.findById(workItemId).orElseThrow();
            itemToUpdate.setStatus(WorkItemStatus.IN_PROGRESS);
            workItemRepository.save(itemToUpdate);
        });

        // Verify version incremented to 1
        WorkItem updated = workItemRepository.findById(workItemId).orElseThrow();
        assertThat(updated.getVersion()).isEqualTo(1L);
        assertThat(updated.getStatus()).isEqualTo(WorkItemStatus.IN_PROGRESS);
    }

    @Test
    void testConcurrentModificationThrowsOptimisticLockingFailure() {
        TransactionTemplate tx = new TransactionTemplate(transactionManager);

        Long workItemId = tx.execute(status -> {
            WorkItem item = new WorkItem(
                    "Compliance Certificate Expiry",
                    "SOC2 annual audit report refresh",
                    WorkItemType.COMPLIANCE,
                    WorkItemPriority.MEDIUM,
                    WorkItemStatus.OPEN,
                    testTeam,
                    testUser,
                    testUser,
                    LocalDate.now().plusDays(7)
            );
            return workItemRepository.save(item).getId();
        });

        // Simulate two concurrent clients loading the same item with version 0
        WorkItem clientA = workItemRepository.findById(workItemId).orElseThrow();
        WorkItem clientB = workItemRepository.findById(workItemId).orElseThrow();

        // Client A updates successfully and increments version to 1
        clientA.setStatus(WorkItemStatus.IN_PROGRESS);
        workItemRepository.saveAndFlush(clientA);

        // Client B tries to save with stale version 0
        clientB.setStatus(WorkItemStatus.RESOLVED);
        assertThatThrownBy(() -> workItemRepository.saveAndFlush(clientB))
                .isInstanceOf(OptimisticLockingFailureException.class);
    }
}
