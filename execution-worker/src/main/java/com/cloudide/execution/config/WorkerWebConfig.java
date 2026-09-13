package com.cloudide.execution.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

@Configuration
public class WorkerWebConfig implements WebMvcConfigurer {

    private final WorkerSecretInterceptor workerSecretInterceptor;

    public WorkerWebConfig(WorkerSecretInterceptor workerSecretInterceptor) {
        this.workerSecretInterceptor = workerSecretInterceptor;
    }

    @Override
    public void addInterceptors(InterceptorRegistry registry) {
        registry.addInterceptor(workerSecretInterceptor)
                .addPathPatterns("/internal/**");
    }
}
