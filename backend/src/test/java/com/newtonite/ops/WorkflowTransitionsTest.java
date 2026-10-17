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

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
class WorkflowTransitionsTest {

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
    private com.newtonite.ops.workitem.idempotency.IdempotentRequestRepository idempotentRequestRepository;

    @Autowired
    private JwtService jwtService;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Autowired
    private ObjectMapper objectMapper;

    private User leadUser;
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

        team = teamRepository.save(new Team("Core Operations", "Ops"));
        leadUser = userRepository.save(new User("Lead Alice", "alice@ops.internal", passwordEncoder.encode("Pass123!")));
        membershipRepository.save(new Membership(leadUser, team, MembershipRole.LEAD));
        token = jwtService.generateToken(leadUser);
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
    void testValidWorkflowTransitionsEndToEnd() throws Exception {
        // 1. Initial item in OPEN status
        WorkItem item = workItemRepository.save(new WorkItem(
                "Database Migration Ticket",
                "Schema upgrade",
                WorkItemType.TASK,
                WorkItemPriority.MEDIUM,
                WorkItemStatus.OPEN,
                team,
                leadUser,
                leadUser,
                LocalDate.now().plusDays(2)
        ));

        // 2. OPEN -> IN_PROGRESS
        TransitionWorkItemRequest toInProgress = new TransitionWorkItemRequest(
                WorkItemStatus.IN_PROGRESS,
                item.getVersion()
        );
        mockMvc.perform(post("/api/items/" + item.getId() + "/transition")
                .header("Authorization", "Bearer " + token)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(toInProgress)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("IN_PROGRESS"))
                .andExpect(jsonPath("$.version").value(1));

        // 3. IN_PROGRESS -> AWAITING_APPROVAL
        TransitionWorkItemRequest toAwaitingApproval = new TransitionWorkItemRequest(
                WorkItemStatus.AWAITING_APPROVAL,
                1L
        );
        mockMvc.perform(post("/api/items/" + item.getId() + "/transition")
                .header("Authorization", "Bearer " + token)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(toAwaitingApproval)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("AWAITING_APPROVAL"))
                .andExpect(jsonPath("$.version").value(2));

        // 4. AWAITING_APPROVAL -> RESOLVED (Lead approval)
        TransitionWorkItemRequest toResolved = new TransitionWorkItemRequest(
                WorkItemStatus.RESOLVED,
                2L
        );
        mockMvc.perform(post("/api/items/" + item.getId() + "/transition")
                .header("Authorization", "Bearer " + token)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(toResolved)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("RESOLVED"))
                .andExpect(jsonPath("$.version").value(3));

        // 5. RESOLVED -> CLOSED
        TransitionWorkItemRequest toClosed = new TransitionWorkItemRequest(
                WorkItemStatus.CLOSED,
                3L
        );
        mockMvc.perform(post("/api/items/" + item.getId() + "/transition")
                .header("Authorization", "Bearer " + token)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(toClosed)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("CLOSED"))
                .andExpect(jsonPath("$.version").value(4));
    }

    @Test
    void testInvalidTransitionsRejectedWithConflict() throws Exception {
        WorkItem item = workItemRepository.save(new WorkItem(
                "Open Ticket",
                "Testing invalid jump",
                WorkItemType.INCIDENT,
                WorkItemPriority.URGENT,
                WorkItemStatus.OPEN,
                team,
                leadUser,
                leadUser,
                LocalDate.now().plusDays(1)
        ));

        // Invalid: OPEN -> RESOLVED (must go through IN_PROGRESS and AWAITING_APPROVAL first)
        TransitionWorkItemRequest invalidJump = new TransitionWorkItemRequest(
                WorkItemStatus.RESOLVED,
                item.getVersion()
        );

        mockMvc.perform(post("/api/items/" + item.getId() + "/transition")
                .header("Authorization", "Bearer " + token)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(invalidJump)))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.status").value(409))
                .andExpect(jsonPath("$.error").value("Conflict"))
                .andExpect(jsonPath("$.message").value(org.hamcrest.Matchers.containsString("Invalid status transition")));
    }
}
