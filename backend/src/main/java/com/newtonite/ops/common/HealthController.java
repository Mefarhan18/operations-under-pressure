package com.newtonite.ops.common;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.DatabaseMetaData;
import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping
public class HealthController {

    private final DataSource dataSource;

    @Value("${spring.profiles.active:default}")
    private String activeProfile;

    public HealthController(DataSource dataSource) {
        this.dataSource = dataSource;
    }

    @GetMapping({"/api/v1/health", "/api/health"})
    public ResponseEntity<HealthResponse> getHealth() {
        Map<String, Object> details = new HashMap<>();
        boolean dbHealthy = false;

        try (Connection connection = dataSource.getConnection()) {
            DatabaseMetaData metaData = connection.getMetaData();
            details.put("database", "UP");
            details.put("databaseProductName", metaData.getDatabaseProductName());
            details.put("databaseProductVersion", metaData.getDatabaseProductVersion());
            details.put("databaseUrl", metaData.getURL());
            dbHealthy = true;
        } catch (Exception e) {
            details.put("database", "DOWN");
            details.put("databaseError", e.getMessage());
        }

        details.put("phase", "Phase 1 - Project Foundation");
        details.put("serviceName", "newtonite-ops-backend");

        if (dbHealthy) {
            return ResponseEntity.ok(HealthResponse.up(activeProfile, details));
        } else {
            return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE)
                    .body(HealthResponse.down(activeProfile, details));
        }
    }
}
