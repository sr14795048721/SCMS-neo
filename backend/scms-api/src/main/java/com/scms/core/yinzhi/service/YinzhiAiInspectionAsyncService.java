package com.scms.core.yinzhi.service;

import com.scms.core.iot.repository.IotDeviceTelemetryEventRepository;
import jakarta.annotation.PreDestroy;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.atomic.AtomicInteger;

@Service
public class YinzhiAiInspectionAsyncService {

    private static final Logger log = LoggerFactory.getLogger(YinzhiAiInspectionAsyncService.class);

    private final IotDeviceTelemetryEventRepository eventRepository;
    private final YinzhiAiInspectionService inspectionService;
    private final ExecutorService executor;

    public YinzhiAiInspectionAsyncService(
            IotDeviceTelemetryEventRepository eventRepository,
            YinzhiAiInspectionService inspectionService
    ) {
        this.eventRepository = eventRepository;
        this.inspectionService = inspectionService;
        this.executor = Executors.newFixedThreadPool(4, new NamedThreadFactory());
    }

    public void inspectAfterCommit(Long telemetryEventId) {
        if (telemetryEventId == null) {
            return;
        }
        Runnable task = () -> inspect(telemetryEventId);
        if (TransactionSynchronizationManager.isSynchronizationActive()) {
            TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
                @Override
                public void afterCommit() {
                    executor.execute(task);
                }
            });
            return;
        }
        executor.execute(task);
    }

    private void inspect(Long telemetryEventId) {
        try {
            eventRepository.findById(telemetryEventId)
                    .ifPresentOrElse(
                            inspectionService::inspectTelemetryEvent,
                            () -> log.warn("Yinzhi AI inspection skipped, telemetry event not found id={}", telemetryEventId)
                    );
        } catch (RuntimeException exception) {
            log.warn("Yinzhi AI inspection async task failed telemetryEventId={}", telemetryEventId, exception);
        }
    }

    @PreDestroy
    public void shutdown() {
        executor.shutdown();
    }

    private static final class NamedThreadFactory implements ThreadFactory {
        private final AtomicInteger sequence = new AtomicInteger(1);

        @Override
        public Thread newThread(Runnable runnable) {
            Thread thread = new Thread(runnable, "yinzhi-ai-inspection-" + sequence.getAndIncrement());
            thread.setDaemon(true);
            return thread;
        }
    }
}
