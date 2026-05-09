package com.vantage.search.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.AsyncConfigurer;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;

import java.util.concurrent.Executor;
import java.util.concurrent.ThreadPoolExecutor;

@Configuration
@EnableAsync
@EnableScheduling
public class AsyncConfig implements AsyncConfigurer {

    /**
     * Exposing the executor as a Spring bean lets Spring Boot Actuator's
     * {@code TaskExecutorMetricsAutoConfiguration} bind Micrometer gauges
     * (executor.queued, executor.active, executor.pool.size, executor.completed)
     * automatically. A queue-depth dashboard plus an alert on sustained
     * non-zero queued tasks makes Ollama back-pressure visible.
     */
    @Bean(name = "aiAsyncExecutor")
    public ThreadPoolTaskExecutor aiAsyncExecutor() {
        ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
        executor.setCorePoolSize(4);
        executor.setMaxPoolSize(16);
        executor.setQueueCapacity(100);
        executor.setThreadNamePrefix("ai-async-");
        executor.setTaskDecorator(new MdcTaskDecorator());
        // When the queue saturates, run the task on the publishing thread instead
        // of silently dropping it. AbortPolicy would lose the @TransactionalEventListener
        // event after commit because Spring swallows the rejection there.
        executor.setRejectedExecutionHandler(new ThreadPoolExecutor.CallerRunsPolicy());
        executor.initialize();
        return executor;
    }

    @Override
    public Executor getAsyncExecutor() {
        return aiAsyncExecutor();
    }
}
