package ru.kolivim.document.workflow.service;

//import com.example.docservice.client.DocumentApiClient;
//import com.example.docservice.dto.DocumentBatchRequest;
//import com.example.docservice.dto.DocumentBatchResponse;
//import com.example.docservice.model.Document;
//import com.example.docservice.model.Document.DocumentStatus;
//import com.example.docservice.repository.DocumentRepository;
import lombok.AllArgsConstructor;
import lombok.NoArgsConstructor;
import lombok.RequiredArgsConstructor;
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

@Slf4j
@Service
//@NoArgsConstructor
@AllArgsConstructor
//@RequiredArgsConstructor
public class DocumentProcessingService {

    /** ТехДолг : вынести в сервис и вызвать из него уже */
    private final DocumentRepository documentRepository;

    private final DocumentApiClient apiClient;


    /*
    public DocumentProcessingService(DocumentRepository documentRepository, DocumentApiClient apiClient) {
        this.documentRepository = documentRepository;
        this.apiClient = apiClient;
    }
    */


    @Transactional
    public int processSubmitBatch(int batchSize) {
        log.info("startMethod поиск документов со статусом DRAFT, для отправки на согласование, размер пакета batchSize: {}",
                batchSize);

        /** ТехДолг : переписать на спецификации, после тестирования, на нём и проверить */
        List<Document> documents = documentRepository.findDocumentsByStatusWithLock(
                Status.DRAFT, PageRequest.of(0, batchSize)
        );

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
                /* new DocumentBatchRequest(documentIds, "SUBMIT") */
        );

        /** Вычитываем результат выполнения после отправки на Submitted: */
        if (response.isSuccess() && response.getProcessedIds() != null) {

            // TODO : проверить зачем ещё раз статус изменять ? Он же уже изменён в Сервисе пакетной обработки? Может только перезапустить для проваленных отправку нужно, т.к. они есть в БД ?
//            updateDocumentsStatus(response.getProcessedIds(), Status.SUBMITTED);

            log.info("Успешно отправлено на согласование {} документов", response.getProcessedIds().size());
        }

        if (response.getFailedIds() != null && !response.getFailedIds().isEmpty()) {

            log.info("Не удалось отправить на согласование {} документов", response.getFailedIds().size());

            // SC : Здесь можно добавить логику для повторной обработки или логирования ошибок - В принципе они на следующем круге перевызовутся из БД

        }

        return response.getProcessedIds() != null ? response.getProcessedIds().size() : 0;
    }

    @Transactional
    public int processApproveBatch(int batchSize) {
        log.info("startMethod поиск документов со статусом SUBMITTED, для отправки на утверждение, размер пакета batchSize: {}",
                batchSize);

        List<Document> documents = documentRepository.findDocumentsByStatusWithLock(
                Status.SUBMITTED,
                PageRequest.of(0, batchSize)
        );

        if (documents.isEmpty()) {
            log.info("Нет документов со статусом SUBMITTED для обработки");
            return 0;
        }

        log.info("Найдено {} документов со статусом SUBMITTED", documents.size());

        List<Long> documentIds = documents.stream()
                .map(Document::getId)
                .collect(Collectors.toList());

        DocumentBatchResponse response = apiClient.approveDocuments(
                new DocumentsRequestDto(documentIds, "processApproveBatch", ZonedDateTime.now().toString())
//                new DocumentBatchRequest(documentIds, "APPROVE")
        );

        if (response.isSuccess() && response.getProcessedIds() != null) {

//            updateDocumentsStatus(response.getProcessedIds(), Status.APPROVED);

            log.info("Успешно отправлено на утверждение {} документов", response.getProcessedIds().size());
        }

        if (response.getFailedIds() != null && !response.getFailedIds().isEmpty()) {
            log.info("Не удалось отправить на утверждение {} документов", response.getFailedIds().size());
        }

        return response.getProcessedIds() != null ? response.getProcessedIds().size() : 0;
    }


//    // TODO ПО Идее бредовый метод, его быть не должно
//    @Transactional
//    protected void updateDocumentsStatus(List<Long> documentIds, Status newStatus) {
//
//        List<Document> documents = documentRepository.findAllById(documentIds);
//        documents.forEach(doc -> doc.setStatus(newStatus));
//        documentRepository.saveAll(documents);
//
//        log.info("Обновлен статус {} документов на {}", documents.size(), newStatus);
//    }

}
