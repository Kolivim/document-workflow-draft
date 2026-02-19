package ru.kolivim.document.workflow.scheduler;

import ru.kolivim.document.workflow.entity.Document;
import ru.kolivim.document.workflow.entity.enums.Status;
import ru.kolivim.document.workflow.service.DocumentService;
import ru.kolivim.document.workflow.service.RegisterService;
import jakarta.annotation.PostConstruct;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

@Slf4j
@Component
public class WorkerScheduler {
    private final RegisterService registerService;

    private final DocumentService documentService;

    @Value("${batchSize}")
    Integer batchSize;

    public WorkerScheduler(RegisterService registerService, DocumentService documentService) {
        this.registerService = registerService;
        this.documentService = documentService;
    }

    @PostConstruct
    public void init() {
    }

    @Scheduled(cron = "0 */2 * * * *")
    protected void approve() {
        List<Document> documents = documentService.findByStatusAuthorDate(Status.SUBMITTED, Optional.empty(), Optional.empty(), Optional.empty());
        workWithBatchSize(documents, batchSize, Status.SUBMITTED);
    }

    @Scheduled(cron = "0 */2 * * * *")
    protected void submit() {
        List<Document> documents = documentService.findByStatusAuthorDate(Status.DRAFT, Optional.empty(), Optional.empty(), Optional.empty());
        workWithBatchSize(documents, batchSize, Status.DRAFT);
    }

    private void workWithBatchSize(List<Document> documents,
                                  Integer batchSize,
                                  Status status) {
        List<Long> ids = documents.stream().map(Document::getId).toList();
        if (ids.size() > batchSize) {
            Map<Integer, List<Long>> partitioned = IntStream.range(0, ids.size())
                    .boxed()
                    .collect(Collectors.groupingBy(i -> i / batchSize,
                            Collectors.mapping(ids::get, Collectors.toList())));

            List<List<Long>> parts = new ArrayList<>(partitioned.values());
            for (List<Long> part: parts){
                checkStatus(status, part);
            }
        } else {
            checkStatus(status, ids);
        }
    }
    private void checkStatus(Status status, List<Long> list){
        if (status == Status.DRAFT){
            documentService.submit(list);
            log.info("SUBMITTED documents {}", list.size());
            log.info("ids {}", Arrays.toString(list.toArray()));
        } else {
            registerService.approve(list);
            log.info("APPROVED documents {}", list.size());
            log.info("ids {}", Arrays.toString(list.toArray()));
        }
    }
}
