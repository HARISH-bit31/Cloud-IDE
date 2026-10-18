package com.cloudide.cloudide.controller;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.actuate.health.Health;
import org.springframework.boot.actuate.health.HealthIndicator;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.web.servlet.MockMvc;

import static org.hamcrest.Matchers.*;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest(properties = {
        "spring.datasource.url=jdbc:h2:mem:healthtestdb;DB_CLOSE_DELAY=-1;MODE=MySQL",
        "spring.datasource.driver-class-name=org.h2.Driver",
        "spring.jpa.database-platform=org.hibernate.dialect.H2Dialect",
        "spring.jpa.hibernate.ddl-auto=create-drop",
        "execution.worker.secret=test_worker_secret_12345",
        "management.endpoint.health.probes.enabled=true",
        "management.endpoint.health.group.readiness.include=readinessState,db",
        "management.endpoint.health.group.liveness.include=livenessState"
})
@AutoConfigureMockMvc
class HealthProbeSecurityTests {

    @Autowired
    private MockMvc mockMvc;

    @MockBean(name = "dbHealthContributor")
    private HealthIndicator dbHealthIndicator;

    @Test
    @DisplayName("1. Liveness probe returns 200 UP and does not leak internal details")
    void testLivenessProbeReturnsUp() throws Exception {
        when(dbHealthIndicator.getHealth(org.mockito.ArgumentMatchers.anyBoolean())).thenReturn(Health.up().build());
        when(dbHealthIndicator.health()).thenReturn(Health.up().build());

        mockMvc.perform(get("/actuator/health/liveness"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status", is("UP")))
                .andExpect(jsonPath("$.components").doesNotExist())
                .andExpect(jsonPath("$.details").doesNotExist());
    }

    @Test
    @DisplayName("2. Readiness probe returns 200 UP when database is healthy")
    void testReadinessProbeReturnsUpWhenDatabaseHealthy() throws Exception {
        when(dbHealthIndicator.getHealth(org.mockito.ArgumentMatchers.anyBoolean())).thenReturn(Health.up().build());
        when(dbHealthIndicator.health()).thenReturn(Health.up().build());

        mockMvc.perform(get("/actuator/health/readiness"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status", is("UP")))
                .andExpect(jsonPath("$.components").doesNotExist())
                .andExpect(jsonPath("$.details").doesNotExist());
    }

    @Test
    @DisplayName("3. Readiness probe returns 503 DOWN when database is unavailable")
    void testReadinessProbeReturnsDownWhenDatabaseFails() throws Exception {
        Health downHealth = Health.down().withDetail("error", "Database connection lost").build();
        when(dbHealthIndicator.getHealth(org.mockito.ArgumentMatchers.anyBoolean())).thenReturn(downHealth);
        when(dbHealthIndicator.health()).thenReturn(downHealth);

        mockMvc.perform(get("/actuator/health/readiness"))
                .andExpect(status().isServiceUnavailable())
                .andExpect(jsonPath("$.status", is("DOWN")))
                .andExpect(jsonPath("$.components").doesNotExist())
                .andExpect(jsonPath("$.details").doesNotExist())
                .andExpect(content().string(not(containsString("Database connection lost"))));
    }

    @Test
    @DisplayName("4. Liveness probe remains 200 UP even when database is unavailable")
    void testLivenessRemainsUpWhenDatabaseFails() throws Exception {
        // When MySQL fails, liveness must NOT fail, avoiding pod restart loops
        Health downHealth = Health.down().withDetail("error", "Connection refused: 3306").build();
        when(dbHealthIndicator.getHealth(org.mockito.ArgumentMatchers.anyBoolean())).thenReturn(downHealth);
        when(dbHealthIndicator.health()).thenReturn(downHealth);

        mockMvc.perform(get("/actuator/health/liveness"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status", is("UP")))
                .andExpect(jsonPath("$.components").doesNotExist())
                .andExpect(jsonPath("$.details").doesNotExist());
    }

    @Test
    @DisplayName("5. Backward-compatible /api/health returns 200 UP even when database is unavailable")
    void testExistingApiHealthRemainsUpWhenDatabaseFails() throws Exception {
        Health downHealth = Health.down().build();
        when(dbHealthIndicator.getHealth(org.mockito.ArgumentMatchers.anyBoolean())).thenReturn(downHealth);
        when(dbHealthIndicator.health()).thenReturn(downHealth);

        mockMvc.perform(get("/api/health"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status", is("UP")))
                .andExpect(jsonPath("$.service", is("Cloud IDE Backend")));
    }

    @Test
    @DisplayName("6. Health endpoints do not disclose credentials, connection strings, or stack traces")
    void testHealthEndpointsDoNotDiscloseCredentials() throws Exception {
        when(dbHealthIndicator.getHealth(org.mockito.ArgumentMatchers.anyBoolean())).thenReturn(Health.up().build());
        when(dbHealthIndicator.health()).thenReturn(Health.up().build());

        mockMvc.perform(get("/actuator/health"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status", is("UP")))
                .andExpect(jsonPath("$.components").doesNotExist())
                .andExpect(content().string(not(containsString("password"))))
                .andExpect(content().string(not(containsString("jdbc:"))))
                .andExpect(content().string(not(containsString("root"))));
    }

    @Test
    @DisplayName("7. Spring Security permits health probes without auth while protecting application endpoints")
    void testHealthProbesPublicWhileAppEndpointsProtected() throws Exception {
        when(dbHealthIndicator.getHealth(org.mockito.ArgumentMatchers.anyBoolean())).thenReturn(Health.up().build());
        when(dbHealthIndicator.health()).thenReturn(Health.up().build());

        // Probes are public
        mockMvc.perform(get("/actuator/health/liveness")).andExpect(status().isOk());
        mockMvc.perform(get("/actuator/health/readiness")).andExpect(status().isOk());
        mockMvc.perform(get("/api/health")).andExpect(status().isOk());

        // Application endpoint requires authentication
        mockMvc.perform(get("/api/projects")).andExpect(status().isUnauthorized());
    }
}
