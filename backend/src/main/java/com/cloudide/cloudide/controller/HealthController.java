package com.cloudide.cloudide.controller;

import com.cloudide.cloudide.dto.HealthResponse;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDateTime;

@RestController
@RequestMapping("/api")
public class HealthController {

    /**
     * Basic application process liveness endpoint.
     * Returns UP when the Spring Boot application context is running.
     * Retained for backward compatibility with smoke tests and external monitors.
     * Intentionally does not depend on MySQL connectivity to prevent restart loops
     * during temporary database unavailability.
     * For dependency-aware readiness, use Spring Boot Actuator /actuator/health/readiness.
     */
    @GetMapping("/health")
    public ResponseEntity<HealthResponse> getHealthStatus() {
        HealthResponse response = HealthResponse.builder()
                .status("UP")
                .service("Cloud IDE Backend")
                .timestamp(LocalDateTime.now())
                .build();
        return ResponseEntity.ok(response);
    }
}
