package com.newtonite.ops.comment;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface CommentRepository extends JpaRepository<Comment, Long> {
    List<Comment> findByWorkItemIdOrderByCreatedAtAsc(Long workItemId);
    List<Comment> findByAuthorId(Long authorId);
}
