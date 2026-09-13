package com.cloudide.execution.config;

import jakarta.annotation.PostConstruct;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;

@Component
public class WorkerSecretInterceptor implements HandlerInterceptor {

    private static final Logger log = LoggerFactory.getLogger(WorkerSecretInterceptor.class);
    public static final String SECRET_HEADER = "X-Execution-Worker-Secret";

    @Value("${worker.secret:}")
    private String configuredSecret;

    @PostConstruct
    public void validateSecret() {
        if (configuredSecret == null || configuredSecret.isBlank() || "replace-with-a-long-random-secret".equalsIgnoreCase(configuredSecret)) {
            throw new IllegalStateException("FATAL: EXECUTION_WORKER_SECRET is not configured. Execution Worker requires a valid secret.");
        }
    }

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) throws Exception {
        // Allow CORS pre-flight if any
        if ("OPTIONS".equalsIgnoreCase(request.getMethod())) {
            return true;
        }

        String providedSecret = request.getHeader(SECRET_HEADER);

        if (providedSecret == null || !providedSecret.equals(configuredSecret)) {
            log.warn("Unauthorized worker request to {} from remote IP {}", request.getRequestURI(), request.getRemoteAddr());
            response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
            response.setContentType("application/json");
            response.getWriter().write("{\"status\":401,\"error\":\"Unauthorized\",\"message\":\"Invalid or missing execution worker secret\"}");
            return false;
        }

        return true;
    }
}
