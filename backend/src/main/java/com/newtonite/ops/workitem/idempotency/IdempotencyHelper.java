package com.newtonite.ops.workitem.idempotency;

import com.newtonite.ops.workitem.CreateWorkItemRequest;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HexFormat;

public class IdempotencyHelper {

    public static String computeRequestFingerprint(CreateWorkItemRequest request) {
        if (request == null) {
            return "";
        }

        String normalized = String.join("|",
                request.title() != null ? request.title().trim() : "",
                request.description() != null ? request.description().trim() : "",
                request.type() != null ? request.type().name() : "",
                request.priority() != null ? request.priority().name() : "",
                request.teamId() != null ? request.teamId().toString() : "",
                request.dueDate() != null ? request.dueDate().toString() : ""
        );

        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hashBytes = digest.digest(normalized.getBytes(StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(hashBytes);
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException("SHA-256 algorithm not available", e);
        }
    }
}
