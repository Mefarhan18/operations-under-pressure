package com.newtonite.ops;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.newtonite.ops.activity.ActivityEventRepository;
import com.newtonite.ops.comment.CommentRepository;
import com.newtonite.ops.security.AuthResponse;
import com.newtonite.ops.security.LoginRequest;
import com.newtonite.ops.team.MembershipRepository;
import com.newtonite.ops.team.TeamRepository;
import com.newtonite.ops.user.User;
import com.newtonite.ops.user.UserRepository;
import com.newtonite.ops.workitem.WorkItemRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
class AuthenticationIntegrationTest {

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
    private PasswordEncoder passwordEncoder;

    @Autowired
    private ObjectMapper objectMapper;

    @BeforeEach
    void setUp() {
        commentRepository.deleteAll();
        activityEventRepository.deleteAll();
        workItemRepository.deleteAll();
        idempotentRequestRepository.deleteAll();
        membershipRepository.deleteAll();
        userRepository.deleteAll();
        teamRepository.deleteAll();

        User user = new User("Test Member", "member@newtonite.internal", passwordEncoder.encode("Password123!"));
        userRepository.save(user);
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
    void testSuccessfulLogin() throws Exception {
        LoginRequest request = new LoginRequest("member@newtonite.internal", "Password123!");

        mockMvc.perform(post("/api/auth/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.token").isString())
                .andExpect(jsonPath("$.tokenType").value("Bearer"))
                .andExpect(jsonPath("$.email").value("member@newtonite.internal"))
                .andExpect(jsonPath("$.name").value("Test Member"));
    }

    @Test
    void testUnsuccessfulLoginWithWrongPassword() throws Exception {
        LoginRequest request = new LoginRequest("member@newtonite.internal", "WrongPassword!");

        mockMvc.perform(post("/api/auth/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.status").value(401))
                .andExpect(jsonPath("$.error").value("Unauthorized"));
    }

    @Test
    void testUnsuccessfulLoginWithNonExistentUser() throws Exception {
        LoginRequest request = new LoginRequest("nonexistent@newtonite.internal", "Password123!");

        mockMvc.perform(post("/api/auth/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.status").value(401));
    }

    @Test
    void testGetCurrentUserWithValidToken() throws Exception {
        // First login to get a valid signed JWT
        LoginRequest loginRequest = new LoginRequest("member@newtonite.internal", "Password123!");
        MvcResult loginResult = mockMvc.perform(post("/api/auth/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(loginRequest)))
                .andExpect(status().isOk())
                .andReturn();

        AuthResponse authResponse = objectMapper.readValue(
                loginResult.getResponse().getContentAsString(),
                AuthResponse.class
        );

        mockMvc.perform(get("/api/me")
                .header("Authorization", "Bearer " + authResponse.token()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.email").value("member@newtonite.internal"))
                .andExpect(jsonPath("$.name").value("Test Member"))
                .andExpect(jsonPath("$.enabled").value(true));
    }

    @Test
    void testGetCurrentUserWithoutTokenFailsUnauthorized() throws Exception {
        mockMvc.perform(get("/api/me"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.status").value(401))
                .andExpect(jsonPath("$.error").value("Unauthorized"));
    }

    @Test
    void testGetCurrentUserWithInvalidTokenFailsUnauthorized() throws Exception {
        mockMvc.perform(get("/api/me")
                .header("Authorization", "Bearer invalid.jwt.token.here"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.status").value(401));
    }
}
