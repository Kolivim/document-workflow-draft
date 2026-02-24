package ru.kolivim.document.workflow.worker;

import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Async;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import ru.kolivim.document.workflow.config.BatchConfig;
import ru.kolivim.document.workflow.service.DocumentProcessingService;

@Slf4j
@Component
@AllArgsConstructor
public class ApproveWorker {

    private final DocumentProcessingService processingService;

    private final BatchConfig batchConfig;


    /*
    public ApproveWorker(DocumentProcessingService processingService, BatchConfig batchConfig) {
        this.processingService = processingService;
        this.batchConfig = batchConfig;
    }
    */


    @Async("taskExecutor")
    @Scheduled(fixedDelayString = "${app.batch.approve.fixed-delay}")
    public void processApproveBatch() {
        log.debug("startMethod");

        long startTime = System.currentTimeMillis();

        try {

            int processedCount = processingService.processApproveBatch(batchConfig.getSize());
            long duration = System.currentTimeMillis() - startTime;

            log.debug("Отправлено на Утверждение {} документов за {} мс", processedCount, duration);

        } catch (Exception e) {
            log.error("CATCH в ApproveWorker, e: {}", e.getMessage());
        }

        log.debug("endMethod");
    }

}
