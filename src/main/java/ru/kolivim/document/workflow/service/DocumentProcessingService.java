package ru.kolivim.document.workflow.service;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import ru.kolivim.document.workflow.client.DocumentApiClient;
import ru.kolivim.document.workflow.dto.request.DocumentsRequestDto;
import ru.kolivim.document.workflow.dto.response.DocumentBatchResponse;
import ru.kolivim.document.workflow.entity.Document;
import ru.kolivim.document.workflow.entity.enums.Status;
import ru.kolivim.document.workflow.repository.DocumentRepository;

import java.time.ZonedDateTime;
import java.util.List;
import java.util.stream.Collectors;

@Data
@Slf4j
@Service
@AllArgsConstructor
public class DocumentProcessingService {

    private final DocumentService documentService;

    private final DocumentApiClient apiClient;


    @Transactional
    public int processSubmitBatch(int batchSize) {
        log.info("startMethod поиск документов со статусом DRAFT, для отправки на согласование, размер пакета batchSize: {}",
                batchSize);

        List<Document> documents = documentService.getByStatus (Status.DRAFT, PageRequest.of(0, batchSize));

        if (documents.isEmpty()) {
            log.info("Нет документов со статусом DRAFT для обработки");
            return 0;
        }

        log.info("Найдено {} документов со статусом DRAFT", documents.size());

        List<Long> documentIds = documents.stream()
                .map(Document::getId)
                .collect(Collectors.toList());


        /** Отправляем на Submitted: */
        DocumentBatchResponse response = apiClient.submitDocuments(
                new DocumentsRequestDto(documentIds, "processSubmitBatch", ZonedDateTime.now().toString())
        );


        if (response.isSuccess() && response.getProcessedIds() != null) {
            log.info("Успешно отправлено на согласование {} документов статус SUBMITTED присвоен документам с Id: {}",
                    response.getProcessedIds().size(), response.getProcessedIds());
        }

        if (response.getFailedIds() != null && !response.getFailedIds().isEmpty()) {
            log.info("Не удалось отправить на согласование (SUBMITTED) {} документов, статус SUBMITTED не присвоен " +
                    "документам с Id: {}", response.getFailedIds().size(), response.getFailedIds());

            /** Отдельно запускать повторно нет необходимости - документы со статусом Draft будут получены
             * на следующем/их проходах через репозиторий */

        }

        return response.getProcessedIds() != null ? response.getProcessedIds().size() : 0;
    }


    @Transactional
    public int processApproveBatch(int batchSize) {
        log.info("startMethod поиск документов со статусом SUBMITTED, для отправки на утверждение, " +
                        "размер пакета batchSize: {}", batchSize);

        List<Document> documents = documentService.getByStatus (Status.SUBMITTED, PageRequest.of(0, batchSize));

        if (documents.isEmpty()) {
            log.info("Нет документов со статусом SUBMITTED для обработки");
            return 0;
        }

        log.info("Найдено для Утверждения (Approve) {} документов со статусом SUBMITTED", documents.size());

        List<Long> documentIds = documents.stream().map(Document::getId).collect(Collectors.toList());

        DocumentBatchResponse response = apiClient.approveDocuments(
                new DocumentsRequestDto(documentIds, "processApproveBatch", ZonedDateTime.now().toString())
        );


        if (response.isSuccess() && response.getProcessedIds() != null) {
            log.info("Успешно отправлено на утверждение (Approve) {} документов, " +
                            "статус Approve присвоен документам с Id: {}",
                    response.getProcessedIds().size(), response.getProcessedIds());
        }

        if (response.getFailedIds() != null && !response.getFailedIds().isEmpty()) {
            log.info("PЗавершилась ошибкой отправка на утверждение (Approve) {} документов, " +
                            "статус Approve не присвоен документам с Id: {}",
                    response.getFailedIds().size(), response.getFailedIds());
        }


        return response.getProcessedIds() != null ? response.getProcessedIds().size() : 0;
    }


}
