package com.newtonite.ops.security;

import com.newtonite.ops.team.Membership;
import com.newtonite.ops.team.MembershipRepository;
import com.newtonite.ops.team.MembershipRole;
import com.newtonite.ops.user.User;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Arrays;
import java.util.List;
import java.util.Optional;

@Service
@Transactional(readOnly = true)
public class TeamAuthorizationService {

    private final MembershipRepository membershipRepository;

    public TeamAuthorizationService(MembershipRepository membershipRepository) {
        this.membershipRepository = membershipRepository;
    }

    public Optional<Membership> getMembership(Long userId, Long teamId) {
        return membershipRepository.findByUserIdAndTeamId(userId, teamId);
    }

    public List<Long> getUserTeamIds(Long userId) {
        return membershipRepository.findByUserId(userId).stream()
                .map(m -> m.getTeam().getId())
                .toList();
    }

    public Membership requireTeamMembership(User user, Long teamId) {
        return getMembership(user.getId(), teamId)
                .orElseThrow(() -> new AccessDeniedException(
                        "Access denied: You are not a member of team ID " + teamId
                ));
    }

    public Membership requireRole(User user, Long teamId, MembershipRole... allowedRoles) {
        Membership membership = requireTeamMembership(user, teamId);
        boolean hasRole = Arrays.stream(allowedRoles)
                .anyMatch(role -> role == membership.getRole());

        if (!hasRole) {
            throw new AccessDeniedException(
                    "Access denied: This operation requires one of " + Arrays.toString(allowedRoles) +
                    " role in team '" + membership.getTeam().getName() + "', but your role is " + membership.getRole()
            );
        }
        return membership;
    }

    public Membership requireCanView(User user, Long teamId) {
        return requireRole(user, teamId,
                MembershipRole.VIEWER,
                MembershipRole.MEMBER,
                MembershipRole.LEAD,
                MembershipRole.ADMIN);
    }

    public Membership requireCanWrite(User user, Long teamId) {
        return requireRole(user, teamId,
                MembershipRole.MEMBER,
                MembershipRole.LEAD,
                MembershipRole.ADMIN);
    }

    public Membership requireCanApprove(User user, Long teamId) {
        return requireRole(user, teamId,
                MembershipRole.LEAD,
                MembershipRole.ADMIN);
    }

    public Membership requireCanAdmin(User user, Long teamId) {
        return requireRole(user, teamId, MembershipRole.ADMIN);
    }
}
