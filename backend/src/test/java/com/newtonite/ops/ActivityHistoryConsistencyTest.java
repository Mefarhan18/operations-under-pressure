package com.newtonite.ops;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.newtonite.ops.activity.ActivityEventRepository;
import com.newtonite.ops.comment.CommentRepository;
import com.newtonite.ops.comment.CreateCommentRequest;
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
import org.springframework.test.web.servlet.MvcResult;

import java.time.LocalDate;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
class ActivityHistoryConsistencyTest {

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

        team = teamRepository.save(new Team("Platform Security", "Security"));
        leadUser = userRepository.save(new User("Sec Lead", "seclead@platform.internal", passwordEncoder.encode("Pass123!")));
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
    void testActivityHistoryIsConsistentAcrossFullLifecycle() throws Exception {
        // 1. Create an item via POST /api/items
        CreateWorkItemRequest createReq = new CreateWorkItemRequest(
                "API Gateway DDoS Mitigation",
                "Mitigation for gateway spike",
                WorkItemType.INCIDENT,
                WorkItemPriority.HIGH,
                team.getId(),
                LocalDate.now().plusDays(2)
        );

        MvcResult createResult = mockMvc.perform(post("/api/items")
                .header("Authorization", "Bearer " + token)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(createReq)))
                .andExpect(status().isCreated())
                .andReturn();

        WorkItemResponse created = objectMapper.readValue(
                createResult.getResponse().getContentAsString(),
                WorkItemResponse.class
        );
        Long itemId = created.id();

        // 2. Claim the item via POST /api/items/{id}/claim
        mockMvc.perform(post("/api/items/" + itemId + "/claim")
                .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk());

        // 3. Update priority via PATCH /api/items/{id}
        UpdateWorkItemRequest updateReq = new UpdateWorkItemRequest(
                1L, // version after claim is 1
                null,
                null,
                WorkItemPriority.URGENT,
                null
        );
        mockMvc.perform(patch("/api/items/" + itemId)
                .header("Authorization", "Bearer " + token)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(updateReq)))
                .andExpect(status().isOk());

        // 4. Transition status via POST /api/items/{id}/transition
        TransitionWorkItemRequest transReq = new TransitionWorkItemRequest(
                WorkItemStatus.AWAITING_APPROVAL,
                2L // version after update is 2
        );
        mockMvc.perform(post("/api/items/" + itemId + "/transition")
                .header("Authorization", "Bearer " + token)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(transReq)))
                .andExpect(status().isOk());

        // 5. Add a comment via POST /api/items/{id}/comments
        CreateCommentRequest commentReq = new CreateCommentRequest(
                "Traffic rerouted through Cloudflare Spectrum. Latency normalized."
        );
        mockMvc.perform(post("/api/items/" + itemId + "/comments")
                .header("Authorization", "Bearer " + token)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(commentReq)))
                .andExpect(status().isCreated());

        // 6. Inspect activity history via GET /api/items/{id}/activity
        mockMvc.perform(get("/api/items/" + itemId + "/activity")
                .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isArray())
                .andExpect(jsonPath("$.length()").value(5))
                // Ordered by createdAt DESC
                .andExpect(jsonPath("$[0].eventType").value("COMMENT_ADDED"))
                .andExpect(jsonPath("$[1].eventType").value("STATUS_TRANSITION"))
                .andExpect(jsonPath("$[1].oldValue").value("IN_PROGRESS"))
                .andExpect(jsonPath("$[1].newValue").value("AWAITING_APPROVAL"))
                .andExpect(jsonPath("$[2].eventType").value("PRIORITY_CHANGE"))
                .andExpect(jsonPath("$[2].oldValue").value("HIGH"))
                .andExpect(jsonPath("$[2].newValue").value("URGENT"))
                .andExpect(jsonPath("$[3].eventType").value("CLAIMED"))
                .andExpect(jsonPath("$[3].actorName").value("Sec Lead"))
                .andExpect(jsonPath("$[4].eventType").value("ITEM_CREATED"));
    }
}
