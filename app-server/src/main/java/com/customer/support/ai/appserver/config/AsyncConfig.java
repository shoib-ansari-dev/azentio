package com.customer.support.ai.appserver.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;

@Configuration
@EnableAsync
public class AsyncConfig {

    @Bean("ingestionExecutor")
    public ThreadPoolTaskExecutor ingestionExecutor(
            @Value("${sentinel.thread-pool.ingestion.core}") int core,
            @Value("${sentinel.thread-pool.ingestion.max}") int max,
            @Value("${sentinel.thread-pool.ingestion.queue-capacity}") int queueCapacity) {
        return executor(core, max, queueCapacity, "ingestion-");
    }

    @Bean("detectionExecutor")
    public ThreadPoolTaskExecutor detectionExecutor(
            @Value("${sentinel.thread-pool.detection.core}") int core,
            @Value("${sentinel.thread-pool.detection.max}") int max,
            @Value("${sentinel.thread-pool.detection.queue-capacity}") int queueCapacity) {
        return executor(core, max, queueCapacity, "detection-");
    }

    @Bean("auditExecutor")
    public ThreadPoolTaskExecutor auditExecutor(
            @Value("${sentinel.thread-pool.audit.core}") int core,
            @Value("${sentinel.thread-pool.audit.max}") int max,
            @Value("${sentinel.thread-pool.audit.queue-capacity}") int queueCapacity) {
        return executor(core, max, queueCapacity, "audit-");
    }

    private ThreadPoolTaskExecutor executor(int core, int max, int queueCapacity, String prefix) {
        ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
        executor.setCorePoolSize(core);
        executor.setMaxPoolSize(max);
        executor.setQueueCapacity(queueCapacity);
        executor.setThreadNamePrefix(prefix);
        executor.initialize();
        return executor;
    }
}
