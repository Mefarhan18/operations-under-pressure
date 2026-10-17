package com.newtonite.ops.security;

import com.newtonite.ops.team.Membership;
import com.newtonite.ops.team.MembershipRepository;
import com.newtonite.ops.user.User;
import com.newtonite.ops.user.UserProfileResponse;
import com.newtonite.ops.user.UserRepository;
import com.newtonite.ops.user.UserTeamMembershipDTO;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.DisabledException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping
public class AuthController {

    private final AuthenticationManager authenticationManager;
    private final JwtService jwtService;
    private final UserRepository userRepository;
    private final MembershipRepository membershipRepository;

    public AuthController(AuthenticationManager authenticationManager,
                          JwtService jwtService,
                          UserRepository userRepository,
                          MembershipRepository membershipRepository) {
        this.authenticationManager = authenticationManager;
        this.jwtService = jwtService;
        this.userRepository = userRepository;
        this.membershipRepository = membershipRepository;
    }

    @PostMapping("/api/auth/login")
    public ResponseEntity<AuthResponse> login(@Valid @RequestBody LoginRequest request) {
        try {
            authenticationManager.authenticate(
                    new UsernamePasswordAuthenticationToken(request.email(), request.password())
            );
        } catch (BadCredentialsException e) {
            throw new BadCredentialsException("Invalid email or password");
        } catch (DisabledException e) {
            throw new DisabledException("Account is disabled");
        }

        User user = userRepository.findByEmail(request.email())
                .orElseThrow(() -> new BadCredentialsException("Invalid email or password"));

        if (!user.isEnabled()) {
            throw new DisabledException("Account is disabled");
        }

        String token = jwtService.generateToken(user);
        return ResponseEntity.ok(AuthResponse.bearer(token, user.getId(), user.getName(), user.getEmail()));
    }

    @org.springframework.transaction.annotation.Transactional(readOnly = true)
    @GetMapping("/api/me")
    public ResponseEntity<UserProfileResponse> getCurrentUser(Authentication authentication) {
        if (authentication == null || !authentication.isAuthenticated()) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }

        String email;
        if (authentication.getPrincipal() instanceof UserDetails userDetails) {
            email = userDetails.getUsername();
        } else {
            email = authentication.getName();
        }

        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new BadCredentialsException("User not found"));

        List<Membership> memberships = membershipRepository.findByUserIdWithTeam(user.getId());
        List<UserTeamMembershipDTO> membershipDTOs = memberships.stream()
                .map(m -> new UserTeamMembershipDTO(
                        m.getTeam().getId(),
                        m.getTeam().getName(),
                        m.getRole()
                ))
                .toList();

        UserProfileResponse profile = new UserProfileResponse(
                user.getId(),
                user.getName(),
                user.getEmail(),
                user.isEnabled(),
                user.getCreatedAt(),
                membershipDTOs
        );

        return ResponseEntity.ok(profile);
    }
}
