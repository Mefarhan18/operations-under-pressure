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

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
class AuthorizationAndCrossTeamAccessTest {

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

    private User userTeamA;
    private User viewerTeamB;
    private User memberTeamB;
    private User leadTeamB;

    private Team teamA;
    private Team teamB;

    private WorkItem itemTeamB;

    @BeforeEach
    void setUp() {
        commentRepository.deleteAll();
        activityEventRepository.deleteAll();
        workItemRepository.deleteAll();
        idempotentRequestRepository.deleteAll();
        membershipRepository.deleteAll();
        userRepository.deleteAll();
        teamRepository.deleteAll();

        teamA = teamRepository.save(new Team("Team Alpha", "Alpha team"));
        teamB = teamRepository.save(new Team("Team Beta", "Beta team"));

        userTeamA = userRepository.save(new User("Alice Alpha", "alice@alpha.internal", passwordEncoder.encode("Pass123!")));
        viewerTeamB = userRepository.save(new User("Bob Viewer", "bob@beta.internal", passwordEncoder.encode("Pass123!")));
        memberTeamB = userRepository.save(new User("Charlie Member", "charlie@beta.internal", passwordEncoder.encode("Pass123!")));
        leadTeamB = userRepository.save(new User("Diana Lead", "diana@beta.internal", passwordEncoder.encode("Pass123!")));

        membershipRepository.save(new Membership(userTeamA, teamA, MembershipRole.MEMBER));
        membershipRepository.save(new Membership(viewerTeamB, teamB, MembershipRole.VIEWER));
        membershipRepository.save(new Membership(memberTeamB, teamB, MembershipRole.MEMBER));
        membershipRepository.save(new Membership(leadTeamB, teamB, MembershipRole.LEAD));

        itemTeamB = workItemRepository.save(new WorkItem(
                "Beta Payment Processing Task",
                "Internal task for Beta",
                WorkItemType.PAYMENT,
                WorkItemPriority.HIGH,
                WorkItemStatus.AWAITING_APPROVAL,
                teamB,
                null,
                memberTeamB,
                LocalDate.now().plusDays(3)
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
    void testCrossTeamAccessForbidden() throws Exception {
        // User from Team A tries to access item belonging to Team B
        String tokenAlpha = jwtService.generateToken(userTeamA);

        mockMvc.perform(get("/api/items/" + itemTeamB.getId())
                .header("Authorization", "Bearer " + tokenAlpha))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.status").value(403))
                .andExpect(jsonPath("$.error").value("Forbidden"));
    }

    @Test
    void testViewerCannotCreateItem() throws Exception {
        String tokenViewer = jwtService.generateToken(viewerTeamB);

        CreateWorkItemRequest request = new CreateWorkItemRequest(
                "Unauthorized Item",
                "Viewer cannot create",
                WorkItemType.TASK,
                WorkItemPriority.LOW,
                teamB.getId(),
                LocalDate.now().plusDays(1)
        );

        mockMvc.perform(post("/api/items")
                .header("Authorization", "Bearer " + tokenViewer)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.status").value(403));
    }

    @Test
    void testViewerCannotClaimItem() throws Exception {
        String tokenViewer = jwtService.generateToken(viewerTeamB);

        mockMvc.perform(post("/api/items/" + itemTeamB.getId() + "/claim")
                .header("Authorization", "Bearer " + tokenViewer))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.status").value(403));
    }

    @Test
    void testViewerCannotUpdateItem() throws Exception {
        String tokenViewer = jwtService.generateToken(viewerTeamB);

        UpdateWorkItemRequest request = new UpdateWorkItemRequest(
                itemTeamB.getVersion(),
                "Viewer Malicious Title",
                null,
                null,
                null
        );

        mockMvc.perform(patch("/api/items/" + itemTeamB.getId())
                .header("Authorization", "Bearer " + tokenViewer)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.status").value(403));
    }

    @Test
    void testMemberCannotApproveItem() throws Exception {
        // Member attempts to approve an item (transition AWAITING_APPROVAL -> RESOLVED)
        String tokenMember = jwtService.generateToken(memberTeamB);

        TransitionWorkItemRequest request = new TransitionWorkItemRequest(
                WorkItemStatus.RESOLVED,
                itemTeamB.getVersion()
        );

        mockMvc.perform(post("/api/items/" + itemTeamB.getId() + "/transition")
                .header("Authorization", "Bearer " + tokenMember)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.status").value(403))
                .andExpect(jsonPath("$.message").value(org.hamcrest.Matchers.containsString("requires one of [LEAD, ADMIN]")));
    }

    @Test
    void testLeadCanApproveItem() throws Exception {
        // Lead can approve an item in their team
        String tokenLead = jwtService.generateToken(leadTeamB);

        TransitionWorkItemRequest request = new TransitionWorkItemRequest(
                WorkItemStatus.RESOLVED,
                itemTeamB.getVersion()
        );

        mockMvc.perform(post("/api/items/" + itemTeamB.getId() + "/transition")
                .header("Authorization", "Bearer " + tokenLead)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("RESOLVED"));
    }

    @Test
    void testLeadOfAnotherTeamCannotApproveItem() throws Exception {
        // Lead of Team A tries to approve an item belonging to Team B
        User leadTeamA = userRepository.save(new User("Lead Alpha", "lead@alpha.internal", passwordEncoder.encode("Pass123!")));
        membershipRepository.save(new Membership(leadTeamA, teamA, MembershipRole.LEAD));
        String tokenLeadAlpha = jwtService.generateToken(leadTeamA);

        TransitionWorkItemRequest request = new TransitionWorkItemRequest(
                WorkItemStatus.RESOLVED,
                itemTeamB.getVersion()
        );

        mockMvc.perform(post("/api/items/" + itemTeamB.getId() + "/transition")
                .header("Authorization", "Bearer " + tokenLeadAlpha)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.status").value(403))
                .andExpect(jsonPath("$.message").value(org.hamcrest.Matchers.containsString("not a member of team")));
    }

    @Test
    void testUnauthorizedUserCannotAccessActivity() throws Exception {
        // User belonging only to Team A attempts to view activity events of Team B's item
        String tokenAlpha = jwtService.generateToken(userTeamA);

        mockMvc.perform(get("/api/items/" + itemTeamB.getId() + "/activity")
                .header("Authorization", "Bearer " + tokenAlpha))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.status").value(403))
                .andExpect(jsonPath("$.message").value(org.hamcrest.Matchers.containsString("not a member of team")));
    }

    @Test
    void testUnauthorizedUserCannotAccessOrAddComments() throws Exception {
        // User belonging only to Team A attempts to view comments of Team B's item
        String tokenAlpha = jwtService.generateToken(userTeamA);

        mockMvc.perform(get("/api/items/" + itemTeamB.getId() + "/comments")
                .header("Authorization", "Bearer " + tokenAlpha))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.status").value(403))
                .andExpect(jsonPath("$.message").value(org.hamcrest.Matchers.containsString("not a member of team")));

        // Also attempt to add a comment
        mockMvc.perform(post("/api/items/" + itemTeamB.getId() + "/comments")
                .header("Authorization", "Bearer " + tokenAlpha)
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"body\": \"Malicious comment from another team\"}"))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.status").value(403))
                .andExpect(jsonPath("$.message").value(org.hamcrest.Matchers.containsString("not a member of team")));
    }
}

