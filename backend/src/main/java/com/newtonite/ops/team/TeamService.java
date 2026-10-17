package com.newtonite.ops.team;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

@Service
@Transactional(readOnly = true)
public class TeamService {

    private final TeamRepository teamRepository;
    private final MembershipRepository membershipRepository;

    public TeamService(TeamRepository teamRepository, MembershipRepository membershipRepository) {
        this.teamRepository = teamRepository;
        this.membershipRepository = membershipRepository;
    }

    public List<TeamResponse> getAllTeams() {
        return teamRepository.findAll().stream()
                .map(TeamResponse::fromEntity)
                .toList();
    }

    public Optional<TeamResponse> getTeamById(Long id) {
        return teamRepository.findById(id).map(TeamResponse::fromEntity);
    }

    public List<MembershipResponse> getTeamMemberships(Long teamId) {
        return membershipRepository.findByTeamId(teamId).stream()
                .map(MembershipResponse::fromEntity)
                .toList();
    }
}
