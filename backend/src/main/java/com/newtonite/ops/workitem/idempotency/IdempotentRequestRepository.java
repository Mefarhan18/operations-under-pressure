package com.newtonite.ops.workitem.idempotency;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.Instant;
import java.util.Optional;

@Repository
public interface IdempotentRequestRepository extends JpaRepository<IdempotentRequest, Long> {

    Optional<IdempotentRequest> findByUserIdAndIdempotencyKey(Long userId, String idempotencyKey);

    @Modifying
    @Query("DELETE FROM IdempotentRequest r WHERE r.expiresAt < :now")
    int deleteByExpiresAtBefore(@Param("now") Instant now);
}
