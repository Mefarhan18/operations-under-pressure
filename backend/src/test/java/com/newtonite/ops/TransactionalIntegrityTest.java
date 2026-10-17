package com.newtonite.ops;

import com.newtonite.ops.activity.ActivityEvent;
import com.newtonite.ops.activity.ActivityEventRepository;
import com.newtonite.ops.comment.CommentRepository;
import com.newtonite.ops.team.*;
import com.newtonite.ops.user.User;
import com.newtonite.ops.user.UserRepository;
import com.newtonite.ops.workitem.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.TransactionStatus;
import org.springframework.transaction.support.TransactionCallbackWithoutResult;
import org.springframework.transaction.support.TransactionTemplate;

import java.time.LocalDate;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@SpringBootTest
class TransactionalIntegrityTest {

    @Autowired
    private WorkItemService workItemService;

    @Autowired
    private WorkItemRepository workItemRepository;

    @Autowired
    private ActivityEventRepository activityEventRepository;

    @Autowired
    private CommentRepository commentRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private TeamRepository teamRepository;

    @Autowired
    private MembershipRepository membershipRepository;

    @Autowired
    private PlatformTransactionManager transactionManager;

    @Autowired
    private com.newtonite.ops.workitem.idempotency.IdempotentRequestRepository idempotentRequestRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    private User operator;
    private Team team;

    @BeforeEach
    void setUp() {
        commentRepository.deleteAll();
        activityEventRepository.deleteAll();
        workItemRepository.deleteAll();
        idempotentRequestRepository.deleteAll();
        membershipRepository.deleteAll();
        userRepository.deleteAll();
        teamRepository.deleteAll();

        team = teamRepository.save(new Team("Transactional Test Team", "Tx"));
        operator = userRepository.save(new User("Tx Operator", "operator@tx.internal", passwordEncoder.encode("Pass123!")));
        membershipRepository.save(new Membership(operator, team, MembershipRole.LEAD));
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
    void testItemCreationAndActivityEventAreCommittedTogether() {
        CreateWorkItemRequest req = new CreateWorkItemRequest(
                "Atomic Creation Test",
                "Testing atomicity of creation and activity event",
                WorkItemType.INCIDENT,
                WorkItemPriority.URGENT,
                team.getId(),
                LocalDate.now().plusDays(1)
        );

        WorkItemResponse created = workItemService.createItem(operator, req);

        // Verify work item exists
        WorkItem item = workItemRepository.findById(created.id()).orElseThrow();
        assertThat(item.getTitle()).isEqualTo("Atomic Creation Test");

        // Verify activity event was committed in the same transaction
        List<ActivityEvent> events = activityEventRepository.findByWorkItemIdOrderByCreatedAtDesc(item.getId());
        assertThat(events).hasSize(1);
        assertThat(events.get(0).getEventType()).isEqualTo("ITEM_CREATED");
        assertThat(events.get(0).getActor().getId()).isEqualTo(operator.getId());
    }

    @Test
    void testTransitionAndActivityEventAreCommittedTogether() {
        WorkItem item = workItemRepository.save(new WorkItem(
                "Atomic Transition Test",
                "Desc",
                WorkItemType.TASK,
                WorkItemPriority.HIGH,
                WorkItemStatus.OPEN,
                team,
                operator,
                operator,
                LocalDate.now().plusDays(2)
        ));

        // Perform transition to IN_PROGRESS
        TransitionWorkItemRequest req = new TransitionWorkItemRequest(WorkItemStatus.IN_PROGRESS, item.getVersion());
        workItemService.transitionItem(operator, item.getId(), req);

        // Verify item status updated
        WorkItem updated = workItemRepository.findById(item.getId()).orElseThrow();
        assertThat(updated.getStatus()).isEqualTo(WorkItemStatus.IN_PROGRESS);
        assertThat(updated.getVersion()).isEqualTo(1L);

        // Verify activity event was persisted
        List<ActivityEvent> events = activityEventRepository.findByWorkItemIdOrderByCreatedAtDesc(item.getId());
        assertThat(events).hasSize(1);
        assertThat(events.get(0).getEventType()).isEqualTo("STATUS_TRANSITION");
        assertThat(events.get(0).getOldValue()).isEqualTo("OPEN");
        assertThat(events.get(0).getNewValue()).isEqualTo("IN_PROGRESS");
    }

    @Test
    void testTransactionRollbackRevertsBothWorkItemAndActivityEvent() {
        TransactionTemplate tx = new TransactionTemplate(transactionManager);

        WorkItem item = workItemRepository.save(new WorkItem(
                "Rollback Target",
                "Desc",
                WorkItemType.TASK,
                WorkItemPriority.LOW,
                WorkItemStatus.OPEN,
                team,
                operator,
                operator,
                LocalDate.now().plusDays(3)
        ));
        Long itemId = item.getId();

        // Simulate a transaction failure: update item, create activity event, then throw RuntimeException
        assertThatThrownBy(() -> {
            tx.execute(new TransactionCallbackWithoutResult() {
                @Override
                protected void doInTransactionWithoutResult(TransactionStatus status) {
                    WorkItem entity = workItemRepository.findById(itemId).orElseThrow();
                    entity.setStatus(WorkItemStatus.IN_PROGRESS);
                    workItemRepository.save(entity);

                    activityEventRepository.save(new ActivityEvent(
                            entity,
                            operator,
                            "STATUS_TRANSITION",
                            "OPEN",
                            "IN_PROGRESS"
                    ));

                    // Force an unexpected transaction failure
                    throw new RuntimeException("Simulated unexpected failure during processing");
                }
            });
        }).isInstanceOf(RuntimeException.class)
          .hasMessageContaining("Simulated unexpected failure");

        // Verify atomicity: BOTH the item status change and the activity event were rolled back!
        WorkItem postRollback = workItemRepository.findById(itemId).orElseThrow();
        assertThat(postRollback.getStatus()).isEqualTo(WorkItemStatus.OPEN);
        assertThat(postRollback.getVersion()).isEqualTo(0L);

        List<ActivityEvent> events = activityEventRepository.findByWorkItemIdOrderByCreatedAtDesc(itemId);
        assertThat(events).isEmpty();
    }
}
