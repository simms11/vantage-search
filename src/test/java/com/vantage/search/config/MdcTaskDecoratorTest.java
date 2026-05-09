package com.vantage.search.config;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.slf4j.MDC;

import java.util.Map;
import java.util.concurrent.atomic.AtomicReference;

import static org.assertj.core.api.Assertions.assertThat;

class MdcTaskDecoratorTest {

    private final MdcTaskDecorator decorator = new MdcTaskDecorator();

    @AfterEach
    void clearMdc() {
        MDC.clear();
    }

    @Test
    void copiesParentMdcOntoTaskExecution() throws InterruptedException {
        MDC.put("traceId", "trace-abc");
        MDC.put("userId", "u-1");

        AtomicReference<String> seenTrace = new AtomicReference<>();
        AtomicReference<String> seenUser = new AtomicReference<>();

        Runnable task = decorator.decorate(() -> {
            seenTrace.set(MDC.get("traceId"));
            seenUser.set(MDC.get("userId"));
        });

        Thread worker = new Thread(task);
        worker.start();
        worker.join();

        assertThat(seenTrace.get()).isEqualTo("trace-abc");
        assertThat(seenUser.get()).isEqualTo("u-1");
    }

    @Test
    void clearsWorkerMdcAfterTaskCompletes() throws InterruptedException {
        MDC.put("traceId", "trace-xyz");
        Runnable task = decorator.decorate(() -> { /* no-op */ });

        AtomicReference<Map<String, String>> mdcAfterTask = new AtomicReference<>();
        Thread worker = new Thread(() -> {
            task.run();
            mdcAfterTask.set(MDC.getCopyOfContextMap());
        });
        worker.start();
        worker.join();

        // The worker (a pooled thread in production) must not leak the parent's
        // MDC to the next task scheduled on it.
        Map<String, String> after = mdcAfterTask.get();
        assertThat(after == null || after.isEmpty())
                .as("MDC must be cleared on the worker thread after the task runs")
                .isTrue();
    }
}
