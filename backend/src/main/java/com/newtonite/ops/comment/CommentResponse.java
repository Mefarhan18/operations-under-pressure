package com.newtonite.ops.comment;

import java.time.Instant;

public record CommentResponse(
    Long id,
    Long workItemId,
    Long authorId,
    String authorName,
    String authorEmail,
    String body,
    Instant createdAt
) {
    public static CommentResponse fromEntity(Comment c) {
        return new CommentResponse(
            c.getId(),
            c.getWorkItem().getId(),
            c.getAuthor().getId(),
            c.getAuthor().getName(),
            c.getAuthor().getEmail(),
            c.getBody(),
            c.getCreatedAt()
        );
    }
}
