package com.newtonite.ops.workitem.idempotency;

import com.newtonite.ops.user.User;
import jakarta.persistence.*;
import java.time.Instant;

@Entity
@Table(
    name = "idempotent_requests",
    uniqueConstraints = {
        @UniqueConstraint(name = "uk_idempotency_key_user", columnNames = {"user_id", "idempotency_key"})
    },
    indexes = {
        @Index(name = "idx_idempotent_requests_lookup", columnList = "user_id, idempotency_key"),
        @Index(name = "idx_idempotent_requests_expires", columnList = "expires_at")
    }
)
public class IdempotentRequest {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "idempotency_key", nullable = false, length = 128)
    private String idempotencyKey;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_id", nullable = false, foreignKey = @ForeignKey(name = "fk_idempotency_user"))
    private User user;

    @Column(name = "request_hash", nullable = false, length = 64)
    private String requestHash;

    @Column(name = "work_item_id", nullable = false)
    private Long workItemId;

    @Column(name = "response_status", nullable = false)
    private int responseStatus;

    @Column(name = "response_body", nullable = false, columnDefinition = "TEXT")
    private String responseBody;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @Column(name = "expires_at", nullable = false)
    private Instant expiresAt;

    protected IdempotentRequest() {}

    public IdempotentRequest(String idempotencyKey,
                             User user,
                             String requestHash,
                             Long workItemId,
                             int responseStatus,
                             String responseBody,
                             Instant expiresAt) {
        this.idempotencyKey = idempotencyKey;
        this.user = user;
        this.requestHash = requestHash;
        this.workItemId = workItemId;
        this.responseStatus = responseStatus;
        this.responseBody = responseBody;
        this.createdAt = Instant.now();
        this.expiresAt = expiresAt;
    }

    public Long getId() {
        return id;
    }

    public String getIdempotencyKey() {
        return idempotencyKey;
    }

    public User getUser() {
        return user;
    }

    public String getRequestHash() {
        return requestHash;
    }

    public Long getWorkItemId() {
        return workItemId;
    }

    public int getResponseStatus() {
        return responseStatus;
    }

    public String getResponseBody() {
        return responseBody;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public Instant getExpiresAt() {
        return expiresAt;
    }

    public boolean isExpired(Instant now) {
        return expiresAt != null && expiresAt.isBefore(now);
    }
}
