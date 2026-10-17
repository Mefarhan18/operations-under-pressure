package com.newtonite.ops.activity;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface ActivityEventRepository extends JpaRepository<ActivityEvent, Long> {
    List<ActivityEvent> findByWorkItemIdOrderByCreatedAtDesc(Long workItemId);
    List<ActivityEvent> findByActorId(Long actorId);
}
