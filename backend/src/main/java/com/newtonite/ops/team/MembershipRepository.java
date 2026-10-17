package com.newtonite.ops.team;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface MembershipRepository extends JpaRepository<Membership, Long> {
    List<Membership> findByTeamId(Long teamId);
    List<Membership> findByUserId(Long userId);

    @org.springframework.data.jpa.repository.Query("SELECT m FROM Membership m JOIN FETCH m.team WHERE m.user.id = :userId")
    List<Membership> findByUserIdWithTeam(@org.springframework.data.repository.query.Param("userId") Long userId);

    Optional<Membership> findByUserIdAndTeamId(Long userId, Long teamId);
    boolean existsByUserIdAndTeamId(Long userId, Long teamId);
}
