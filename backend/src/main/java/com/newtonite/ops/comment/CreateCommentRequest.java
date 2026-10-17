package com.newtonite.ops.comment;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record CreateCommentRequest(
    @NotBlank(message = "Comment body is required")
    @Size(max = 5000, message = "Comment body cannot exceed 5000 characters")
    String body
) {}
