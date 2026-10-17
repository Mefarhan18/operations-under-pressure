package com.newtonite.ops.workitem;

import com.newtonite.ops.user.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

@Repository
public interface WorkItemRepository extends JpaRepository<WorkItem, Long>, JpaSpecificationExecutor<WorkItem> {

    List<WorkItem> findByTeamId(Long teamId);

    List<WorkItem> findByAssigneeId(Long assigneeId);

    List<WorkItem> findByStatus(WorkItemStatus status);

    List<WorkItem> findByPriority(WorkItemPriority priority);

    List<WorkItem> findByType(WorkItemType type);

    @Query("SELECT w FROM WorkItem w LEFT JOIN FETCH w.team LEFT JOIN FETCH w.assignee LEFT JOIN FETCH w.createdBy WHERE w.id = :id")
    Optional<WorkItem> findByIdWithDetails(@Param("id") Long id);

    @Query("SELECT w FROM WorkItem w LEFT JOIN FETCH w.team LEFT JOIN FETCH w.assignee LEFT JOIN FETCH w.createdBy ORDER BY w.createdAt DESC")
    List<WorkItem> findAllWithDetails();

    /**
     * Atomic claim operation:
     * Updates assignee and increments version only if assignee is currently null.
     * Prevents race conditions and guarantees that exactly one concurrent claimant succeeds.
     */
    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query("UPDATE WorkItem w SET w.assignee = :user, w.version = w.version + 1, " +
           "w.status = CASE WHEN w.status = 'OPEN' THEN com.newtonite.ops.workitem.WorkItemStatus.IN_PROGRESS ELSE w.status END, " +
           "w.updatedAt = :now " +
           "WHERE w.id = :id AND w.assignee IS NULL")
    int claimItemAtomically(@Param("id") Long id,
                            @Param("user") User user,
                            @Param("now") Instant now);
}
