package com.newtonite.ops.workitem;

import com.newtonite.ops.team.Team;
import com.newtonite.ops.user.User;
import jakarta.persistence.*;
import java.time.Instant;
import java.time.LocalDate;
import java.util.Objects;

@Entity
@Table(name = "work_items", indexes = {
    @Index(name = "idx_work_items_team", columnList = "team_id"),
    @Index(name = "idx_work_items_assignee", columnList = "assignee_id"),
    @Index(name = "idx_work_items_status", columnList = "status"),
    @Index(name = "idx_work_items_priority", columnList = "priority"),
    @Index(name = "idx_work_items_type", columnList = "type")
})
public class WorkItem {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 255)
    private String title;

    @Column(columnDefinition = "TEXT")
    private String description;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    private WorkItemType type;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private WorkItemPriority priority;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    private WorkItemStatus status;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "team_id", nullable = false)
    private Team team;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "assignee_id")
    private User assignee;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "created_by_id", nullable = false)
    private User createdBy;

    @Column(name = "due_date")
    private LocalDate dueDate;

    @Version
    @Column(nullable = false)
    private Long version = 0L;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt = Instant.now();

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt = Instant.now();

    public WorkItem() {
    }

    public WorkItem(String title, String description, WorkItemType type,
                    WorkItemPriority priority, WorkItemStatus status,
                    Team team, User assignee, User createdBy, LocalDate dueDate) {
        this.title = title;
        this.description = description;
        this.type = type;
        this.priority = priority;
        this.status = status;
        this.team = team;
        this.assignee = assignee;
        this.createdBy = createdBy;
        this.dueDate = dueDate;
        this.createdAt = Instant.now();
        this.updatedAt = Instant.now();
    }

    @PrePersist
    public void onPrePersist() {
        if (createdAt == null) {
            createdAt = Instant.now();
        }
        updatedAt = Instant.now();
    }

    @PreUpdate
    public void onPreUpdate() {
        updatedAt = Instant.now();
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public WorkItemType getType() {
        return type;
    }

    public void setType(WorkItemType type) {
        this.type = type;
    }

    public WorkItemPriority getPriority() {
        return priority;
    }

    public void setPriority(WorkItemPriority priority) {
        this.priority = priority;
    }

    public WorkItemStatus getStatus() {
        return status;
    }

    public void setStatus(WorkItemStatus status) {
        this.status = status;
    }

    public Team getTeam() {
        return team;
    }

    public void setTeam(Team team) {
        this.team = team;
    }

    public User getAssignee() {
        return assignee;
    }

    public void setAssignee(User assignee) {
        this.assignee = assignee;
    }

    public User getCreatedBy() {
        return createdBy;
    }

    public void setCreatedBy(User createdBy) {
        this.createdBy = createdBy;
    }

    public LocalDate getDueDate() {
        return dueDate;
    }

    public void setDueDate(LocalDate dueDate) {
        this.dueDate = dueDate;
    }

    public Long getVersion() {
        return version;
    }

    public void setVersion(Long version) {
        this.version = version;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(Instant createdAt) {
        this.createdAt = createdAt;
    }

    public Instant getUpdatedAt() {
        return updatedAt;
    }

    public void setUpdatedAt(Instant updatedAt) {
        this.updatedAt = updatedAt;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        WorkItem workItem = (WorkItem) o;
        return Objects.equals(id, workItem.id);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id);
    }
}
