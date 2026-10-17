package com.newtonite.ops.common;

import java.time.Instant;
import java.util.Map;

public record HealthResponse(
    String status,
    String service,
    String environment,
    Instant timestamp,
    Map<String, Object> details
) {
    public static HealthResponse up(String environment, Map<String, Object> details) {
        return new HealthResponse("UP", "newtonite-ops-backend", environment, Instant.now(), details);
    }

    public static HealthResponse down(String environment, Map<String, Object> details) {
        return new HealthResponse("DOWN", "newtonite-ops-backend", environment, Instant.now(), details);
    }
}
