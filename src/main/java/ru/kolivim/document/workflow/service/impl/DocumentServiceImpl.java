package ru.kolivim.document.workflow.service.impl;

import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DataAccessException;
import org.springframework.data.jpa.domain.Specification;
import ru.kolivim.document.workflow.dto.DocumentDto;
import ru.kolivim.document.workflow.dto.SearchDocumentDto;
import ru.kolivim.document.workflow.dto.request.DocumentsRequestDto;
import ru.kolivim.document.workflow.dto.response.DocumentSubmitResponseDto;
import ru.kolivim.document.workflow.dto.response.DocumentPage;
import ru.kolivim.document.workflow.dto.response.PageResponseDto;
import ru.kolivim.document.workflow.entity.Document;
import ru.kolivim.document.workflow.entity.Document_;
import ru.kolivim.document.workflow.entity.History;
import ru.kolivim.document.workflow.entity.enums.Action;
import ru.kolivim.document.workflow.entity.enums.OperationStatus;
import ru.kolivim.document.workflow.entity.enums.Status;
import ru.kolivim.document.workflow.exception.ResourceNotFoundException;
import ru.kolivim.document.workflow.mapper.DocumentMapper;
import ru.kolivim.document.workflow.repository.DocumentRepository;
import ru.kolivim.document.workflow.repository.HistoryRepository;
import ru.kolivim.document.workflow.service.DocumentService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import ru.kolivim.document.workflow.utils.specification.SpecificationUtils;

import java.nio.charset.StandardCharsets;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.stream.Collectors;

import static ru.kolivim.document.workflow.dto.response.ApiResponse.success;

@Slf4j
@Service
@RequiredArgsConstructor
public class DocumentServiceImpl implements DocumentService {

    private final DocumentRepository documentRepository;

    private final HistoryRepository historyRepository;

    private final DocumentMapper documentMapper;




    /**
     @return Page со списком документов , удовлетворяющих полученным в параметрах условиям,
            таким как : статус, автор, период дат создания
     */
    @Override
    public Page<DocumentDto> getByFilter(SearchDocumentDto searchDocumentDto, Pageable pageable) {
        log.info("startMethod, к поиску получен searchDocumentDto: {}", searchDocumentDto);

        Specification documentSpecification = SpecificationUtils.in(Document_.STATUS, searchDocumentDto.getStatus())
                .and(SpecificationUtils.like(Document_.AUTHOR, searchDocumentDto.getAuthor()))
                .and(SpecificationUtils.betweenDate(
                                Document_.CREATE_DATE,
                                getSearchStartDate(searchDocumentDto.getStartDate()),
                                getSearchEndDate(searchDocumentDto.getEndDate())));


        /*
        Specification documentSpecification = SpecificationUtils.in(Document_.STATUS, searchDocumentDto.getStatus())
                .and(SpecificationUtils.like(Document_.AUTHOR, searchDocumentDto.getAuthor())
                        .and(SpecificationUtils.betweenDate(
                                Document_.CREATE_DATE,
                                getSearchStartDate(searchDocumentDto.getStartDate()),
                                getSearchEndDate(searchDocumentDto.getEndDate())))
                );
        */


        Page<Document> documents = documentRepository.findAll(documentSpecification, pageable);
        Page<DocumentDto> documentsDto = documents.map(documentMapper::entityToDto);

        log.info("endMethod, к возврату Page<DocumentDto>: {}", documentsDto);
        return documentsDto;
    }


    /**
     @return Page со списком документов , удовлетворяющих полученным в параметрах условиям,
     таким как : статус, автор, период дат создания, название документа, дата обновления, внутренний id
     */
    @Override
    public Page<DocumentDto> getByAdvancedFilter(SearchDocumentDto searchDocumentDto, Pageable pageable) {
        log.info("startMethod, к поиску получен searchDocumentDto: {}", searchDocumentDto);

        Specification documentSpecification = SpecificationUtils.in(Document_.STATUS, searchDocumentDto.getStatus())
                .and(SpecificationUtils.like(Document_.AUTHOR, searchDocumentDto.getAuthor()))
                .and(SpecificationUtils.betweenDate(
                        Document_.CREATE_DATE,
                        getSearchStartDate(searchDocumentDto.getStartDate()),
                        getSearchEndDate(searchDocumentDto.getEndDate())))
                .and(SpecificationUtils.like(Document_.NAME, searchDocumentDto.getName()))
                .and(SpecificationUtils.equalDate(Document_.UPDATE_DATE, searchDocumentDto.getUpdateDate()))
                .and(SpecificationUtils.like(Document_.INNER_ID, searchDocumentDto.getInnerId()));


        Page<Document> documents = documentRepository.findAll(documentSpecification, pageable);
        Page<DocumentDto> documentsDto = documents.map(documentMapper::entityToDto);

        log.info("endMethod, к возврату Page<DocumentDto>: {}", documentsDto);
        return documentsDto;
    }


    private ZonedDateTime getSearchStartDate(ZonedDateTime dateTime) {
        log.info("startMethod, : {}", dateTime);
        return dateTime != null ? dateTime
                : ZonedDateTime.of(1900, 01, 01, 01, 01, 01, 1000,
                ZoneId.of("Europe/Moscow"));
    }

    private ZonedDateTime getSearchEndDate(ZonedDateTime dateTime) {
        log.info("startMethod, : {}", dateTime);
        return dateTime != null ? dateTime : ZonedDateTime.now();
    }


    @Override
    @Transactional
    public DocumentDto create(DocumentDto documentDto) {
        log.debug("startMethod, documentDto: {}", documentDto);

        Document document = documentMapper.dtoToNewEntity(documentDto);

        DocumentDto returnDocumentDto = documentMapper.entityToDto(documentRepository.save(document));

        log.debug("endMethod, к возврату documentDto: {}", documentDto);

        return returnDocumentDto;

//        return documentMapper.entityToDto(documentRepository.save(document));
    }


    @Override
    public DocumentDto getById(Long id) {
        log.debug("startMethod, id: {}", id);

        Document document = documentRepository.findById(id).orElseThrow(() -> new ResourceNotFoundException("Document not found"));

        DocumentDto returnDocumentDto = documentMapper.entityToDto(document);

        return returnDocumentDto;
    }


    @Override
    public Page<DocumentDto> getByIdList(Pageable pageable, List<Long> idList) {
        log.debug("startMethod, idList: {}", idList);

        Specification documentSpecification = SpecificationUtils.in(Document_.ID, idList);
        Page<Document> documents = documentRepository.findAll(documentSpecification, pageable);
        Page<DocumentDto> documentsDto = documents.map(documentMapper::entityToDto);

        log.debug("endMethod, Page documentsDto: {}", documentsDto);
        return documentsDto;
    }


    @Override
    public PageResponseDto /* ApiResponse */ getByIdListWithNoFound(Pageable pageable, List<Long> idList) {
        log.debug("startMethod, idList: {}", idList);

        Specification documentSpecification = SpecificationUtils.in(Document_.ID, idList);

        Page<Document> documents = documentRepository.findAll(documentSpecification, pageable);
        Page<DocumentDto> documentsDto = documents.map(documentMapper::entityToDto);

        List<Long> notExistingIdList = getNotExistingIds(idList);

        PageResponseDto pageResponseDto = new PageResponseDto(documentsDto, notExistingIdList,
                notExistingIdList.size(), idList.size());

        log.debug("endMethod, к возврату pageResponse: {}", pageResponseDto);
        return pageResponseDto;
    }


    @Override
    public /* Page<DocumentDto> */ DocumentPage getByIdListWithExtendedPage(Pageable pageable, List<Long> idList) {
        log.debug("startMethod, idList: {}, pageable: {}", idList, pageable);

        Specification documentSpecification = SpecificationUtils.in(Document_.ID, idList);

        Page<Document> documents = documentRepository.findAll(documentSpecification, pageable);
        Page<DocumentDto> documentsDto = documents.map(documentMapper::entityToDto);

        List<Long> notExistingIdList = getNotExistingIds(idList);

        log.debug("endMethod, Page<DocumentDto> documentsDto: {}", documentsDto);

        return new DocumentPage<>(
                documentsDto.getContent(),
                pageable,
                documentsDto.getTotalElements(),
                notExistingIdList,
                notExistingIdList.size(),
                idList.size()
        );
    }


    @Override
    public List<DocumentSubmitResponseDto> submit(Pageable pageable, DocumentsRequestDto documentsRequestDto) {
        log.debug("startMethod, documentsRequestDto: {}, pageable: {}", documentsRequestDto, pageable);
        return submit(pageable, documentsRequestDto.getIds(), documentsRequestDto.getAuthor(), documentsRequestDto.getComment());
    }


    @Transactional
    public List<DocumentSubmitResponseDto> submit(Pageable pageable, List<Long> idList, String author, String comment) {
        log.debug("startMethod, размер полученного списка: {}, documentsRequestDto: {}, author: {}, comment:{}, pageable: {}",
                idList != null ? idList.size() : "NULL", idList, author, comment, pageable);

        List<DocumentSubmitResponseDto> documentSubmitResponseDtoList = new ArrayList<>();


        for (Long documentId : idList) {

            try {

                DocumentSubmitResponseDto result = submit(documentId, author, comment);
                documentSubmitResponseDtoList.add(result);
                log.debug("Документ {} обработан со статусом: {}", documentId, result.getOperationStatus());

            } catch (Exception e) {
                log.error("Ошибка при обработке документа c Id {}: {}", documentId, e.getMessage());
                documentSubmitResponseDtoList.add(new DocumentSubmitResponseDto(documentId, OperationStatus.ERROR));    // TODO Проверить нужный ли статус стоит
            }

        }


        log.debug("endMethod, submitDocumentDtoList: {}", documentSubmitResponseDtoList);
        return documentSubmitResponseDtoList;
    }


    /** Обработка одного документа в отдельной транзакции */
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public DocumentSubmitResponseDto submit(Long documentId, String author, String comment) {
        log.debug("Начало обработки документа c Id: {}, author: {}, comment: {}", documentId, author, comment);

        try {


//            Optional<Document> documentOpt = documentRepository.findById(documentId);
//
//            if (documentOpt.isEmpty()) {
//                log.debug("Документ c Id: {} не найден", documentId);
//                return new DocumentSubmitResponseDto(documentId, OperationStatus.NOT_FOUND);
//            }
//
//            Document document = documentOpt.get();
//            Status currentStatus = document.getStatus();
//
//
//            /** Проверка текущего статуса : */
//            if (currentStatus != Status.DRAFT) {
//
//                log.warn("Конфликт при обработке документа {}, {}",
//                        documentId, currentStatus == Status.SUBMITTED ?
//                                "Документ уже в статусе SUBMITTED" : "Документ не находится статусе DRAFT");
//
//                return new DocumentSubmitResponseDto(documentId, OperationStatus.CONFLICT);
//            }
//            /** !Проверка текущего статуса */


            boolean isDocumentExist = documentRepository.existsById(documentId);

            if (!isDocumentExist) {
                log.debug("Документ c Id: {} не найден", documentId);
                return new DocumentSubmitResponseDto(documentId, OperationStatus.NOT_FOUND);
            }


            /** Атомарное обновление статуса: */
            int updatedCount = documentRepository.updateStatusIfExpected(
                    documentId,
                    Status.DRAFT,
                    Status.SUBMITTED
            );


            if (updatedCount == 0) {

                /** Статус мог измениться в другом потоке */
                Status newStatus = documentRepository.findStatusById(documentId).orElse(Status.DRAFT);

                log.warn("Конфликт при обновлении документа {}: {}", documentId, newStatus == Status.SUBMITTED ?
                        "Документ уже в статусе SUBMITTED (конкурирующее обновление)"
                        : String.format("Статус документа изменился на %s", newStatus));
                return new DocumentSubmitResponseDto(documentId, OperationStatus.CONFLICT);

            } else {

                /* Document documentForHistory = documentRepository.getReferenceById(documentId); */

                Document document = documentRepository.findById(documentId)
                        .orElseThrow(() -> new RuntimeException("Документ после обновления не найден: " + documentId));

                /** Запись в историю: */
                History history = History.builder()
                        .document(document)
                        .action(Action.SUBMIT)
                        .author(author != null ? author : "SYSTEM")
                        .comment(comment)
                        .date(document.getUpdateDate())
                        .build();

                historyRepository.save(history);
                /** !Запись в историю */


                log.info("Документ с Id: {} успешно обработан, статус изменен с DRAFT на SUBMITTED", documentId);
                return new DocumentSubmitResponseDto(documentId, OperationStatus.SUCCESS);

            }
            /** !Атомарное обновление статуса */


        } catch (DataAccessException e) {
            log.error("Ошибка базы данных при обработке документа {}: {}", documentId, e.getMessage());
            return new DocumentSubmitResponseDto(documentId, OperationStatus.ERROR);                                    /** Ошибка базы данных */
        } catch (Exception e) {
            log.error("Неожиданная ошибка при обработке документа {}: {}", documentId, e.getMessage(), e);
            return new DocumentSubmitResponseDto(documentId, OperationStatus.ERROR);                                    /** Неожиданная ошибка */
        }

    }


    /*********/
//    /**
//     * Основной метод для пакетной обработки документов
//     * Каждый документ обрабатывается атомарно в отдельной транзакции
//     */
//    @Transactional
//    public List<DocumentProcessingResult> processDocuments(List<Integer> documentIds) {
//        log.info("Начало обработки {} документов", documentIds.size());
//
//        List<DocumentProcessingResult> results = new ArrayList<>();
//
//        for (Integer docId : documentIds) {
//            try {
//                DocumentProcessingResult result = processSingleDocument(docId);
//                results.add(result);
//                log.debug("Документ {} обработан: {}", docId, result.getResult());
//            } catch (Exception e) {
//                log.error("Ошибка при обработке документа {}: {}", docId, e.getMessage(), e);
//                results.add(DocumentProcessingResult.error(docId,
//                        "Системная ошибка: " + e.getMessage()));
//            }
//        }
//
//        // Статистика обработки
//        Map<DocumentProcessingResult.ResultStatus, Long> stats = results.stream()
//                .collect(Collectors.groupingBy(DocumentProcessingResult::getResult, Collectors.counting()));
//
//        log.info("Обработка завершена. Статистика: {}", stats);
//
//        return results;
//    }
//
//
//    /**
//     * Параллельная обработка с ограничением количества потоков
//     */
//    public List<DocumentProcessingResult> processDocumentsParallel(List<Integer> documentIds,
//                                                                   int threadPoolSize) {
//        log.info("Параллельная обработка {} документов в {} потоков",
//                documentIds.size(), threadPoolSize);
//
//        ExecutorService executor = Executors.newFixedThreadPool(threadPoolSize);
//        List<CompletableFuture<DocumentProcessingResult>> futures = new ArrayList<>();
//
//        try {
//            for (Integer docId : documentIds) {
//                CompletableFuture<DocumentProcessingResult> future =
//                        CompletableFuture.supplyAsync(() -> processSingleDocument(docId), executor);
//                futures.add(future);
//            }
//
//            // Ожидаем завершения всех задач
//            List<DocumentProcessingResult> results = futures.stream()
//                    .map(CompletableFuture::join)
//                    .collect(Collectors.toList());
//
//            log.info("Параллельная обработка завершена");
//            return results;
//
//        } finally {
//            executor.shutdown();
//        }
//    }
//
//    /**
//     * Обработка с повторными попытками для конфликтных ситуаций
//     */
//    @Transactional
//    public List<DocumentProcessingResult> processDocumentsWithRetry(List<Integer> documentIds,
//                                                                    int maxRetries) {
//        log.info("Обработка {} документов с повторными попытками (макс: {})",
//                documentIds.size(), maxRetries);
//
//        Set<Integer> remainingIds = new HashSet<>(documentIds);
//        List<DocumentProcessingResult> allResults = new ArrayList<>();
//        int attempt = 1;
//
//        while (!remainingIds.isEmpty() && attempt <= maxRetries) {
//            log.info("Попытка {}: обработка {} документов", attempt, remainingIds.size());
//
//            List<Integer> currentBatch = new ArrayList<>(remainingIds);
//            List<DocumentProcessingResult> attemptResults = processDocuments(currentBatch);
//
//            allResults.addAll(attemptResults);
//
//            // Оставляем только конфликтные для повторной обработки
//            remainingIds.clear();
//            for (DocumentProcessingResult result : attemptResults) {
//                if (result.getResult() == DocumentProcessingResult.ResultStatus.CONFLICT) {
//                    remainingIds.add(result.getDocumentId());
//                }
//            }
//
//            if (!remainingIds.isEmpty()) {
//                log.info("Конфликтных документов после попытки {}: {}",
//                        attempt, remainingIds.size());
//                try {
//                    Thread.sleep(100); // Пауза перед повторной попыткой
//                } catch (InterruptedException e) {
//                    Thread.currentThread().interrupt();
//                    break;
//                }
//            }
//
//            attempt++;
//        }
//
//        return allResults;
//    }
//
//    /**
//     * Получение истории изменений для документов
//     */
//    @Transactional(readOnly = true)
//    public Map<Integer, List<DocumentHistory>> getDocumentsHistory(List<Integer> documentIds) {
//        List<DocumentHistory> historyList = historyRepository.findByDocumentIdInOrderByChangedAtDesc(documentIds);
//        return historyList.stream()
//                .collect(Collectors.groupingBy(DocumentHistory::getDocumentId));
//    }
    /*********/


    public List<Long> getNotExistingIds(List<Long> idList) {
        log.debug("startMethod, idList: {}", idList);

        List<Long> existingIdInList = documentRepository.findAllExistingIds(idList);
        Set<Long> existingIdSet = new HashSet<>(existingIdInList);

        List<Long> notExistingIdInList = idList.stream()
                .filter(id -> !existingIdSet.contains(id))
                .collect(Collectors.toList());

        log.debug("endMethod, notExistingIdInList: {}", notExistingIdInList);
        return notExistingIdInList;
    }


    public static UUID generateInnerId(String author, String name, ZonedDateTime date) {
        log.debug("startMethod, author: {}, name: {}, date: {}", author, name, date);

        DateTimeFormatter formatter = DateTimeFormatter.ISO_INSTANT;
        String formattedDate = date.format(formatter);

        String source = String.format("%s|%s|%s",
                author != null ? author : "",
                name != null ? name : "",
                formattedDate
        );

//        UUID namespace = UUID.fromString("00000000-0000-0000-0000-000000000001");

        log.debug("endMethod");
        return UUID.nameUUIDFromBytes(source.getBytes(StandardCharsets.UTF_8));
    }


    /** Устаревшие реализации далее */
    /******************************************************************************************************************/


    @Override
    @Deprecated
    public List<Document> findByStatusAuthorDate(Status status, Optional<String> author, Optional<ZonedDateTime> startDate, Optional<ZonedDateTime> endDate) {

        List<Document> documents = new ArrayList<>(documentRepository.findByStatus(status));

        author.ifPresent(s -> documents.retainAll(documentRepository.findByAuthor(s)));
        startDate.ifPresent(zonedDateTime -> documents.retainAll(documentRepository.findByCreateDateAfter(zonedDateTime)));
        endDate.ifPresent(zonedDateTime -> documents.retainAll(documentRepository.findByCreateDateBefore(zonedDateTime)));

        return documents;
    }


    @Deprecated
    @Transactional
    public List<DocumentSubmitResponseDto> submitOld(Pageable pageable, List<Long> idList) {
        log.debug("startMethod, idList: {}, pageable: {}", idList, pageable);

        List<DocumentSubmitResponseDto> documentSubmitResponseDtoList = new ArrayList<>();


        for (Long id: idList){

            if (documentRepository.findById(id).isPresent()){

                if (documentRepository.findById(id).get().getStatus() != Status.DRAFT){
                    documentSubmitResponseDtoList.add(new DocumentSubmitResponseDto(id, OperationStatus.CONFLICT));

                } else {

                    try {
                        update(documentRepository.findById(id).get());
                        documentSubmitResponseDtoList.add(new DocumentSubmitResponseDto(id, OperationStatus.SUCCESS));
                    } catch (Exception e){
                        documentSubmitResponseDtoList.add(new DocumentSubmitResponseDto(id, OperationStatus.ERROR));
                    }

                }

            } else {
                documentSubmitResponseDtoList.add(new DocumentSubmitResponseDto(id, OperationStatus.NOT_FOUND));
            }

        }


        log.debug("endMethod, submitDocumentDtoList: {}", documentSubmitResponseDtoList);
        return documentSubmitResponseDtoList;
    }


    @Override
    @Deprecated
    public DocumentDto entityToDto(Document document) {
        return documentMapper.entityToDto(document);
    }


    @Override
    @Deprecated
    public List<DocumentDto> entitiesToDtos(List<Document> documents) {return documentMapper.entitiesToDtos(documents);}


    @Override
    @Transactional
    public Document update(Document document) {

        Document newDocument = documentRepository.findById(document.getId()).orElseThrow();

        if (document.getCreateDate() != null) {
            newDocument.setCreateDate(document.getCreateDate());
        }

        if (document.getAuthor() != null) {
            newDocument.setAuthor(document.getAuthor());
        }

        newDocument.setStatus(Status.SUBMITTED);
        if (document.getName() != null) {
            newDocument.setName(document.getName());
        }

        Set<History> historySet = generateHistorySubmit(document);
        //document.getHistorySet().clear();

        newDocument.getHistorySet().addAll(historySet);
        //historyRepository.saveAll(historySet);
//        for (History history1 : historySet) {
//            history1.setDocument(document);
//        }

        if (document.getInnerId() != null) {
            newDocument.setInnerId(document.getInnerId());
        }

//        newDocument.setUpdateTime(ZonedDateTime.now());

        documentRepository.save(newDocument);
        return newDocument;
    }


    /** Вынести в Util GenerateDate */
    private String generateAuthor(){
        List<String> authors = List.of("Steven Spielberg", "Martin Scorsese", "Christopher Nolan", "Alfred Hitchcock",
                "Stanley Kubrick");
        Random rand = new Random();
        int n = rand.nextInt(authors.size());
        return authors.get(n);
    }


    /** Вынести в Util GenerateDate */
    private String generateTitle(){
        List<String> titles = List.of("Citizen Kane", "Casablanca", "The Godfather", "Gone with the Wind",
                "Lawrence of Arabia", "The Wizard of Oz");
        Random rand = new Random();
        int n = rand.nextInt(titles.size());
        return titles.get(n);
    }


    /** Вынести в Util GenerateDate */
    @Transactional(propagation = Propagation.MANDATORY)
    Set<History> generateHistorySubmit(Document document){
        Set<History> histories = new HashSet<>();
        History history = History.builder()
                .action(Action.SUBMIT)
                .date(ZonedDateTime.now())
                .author(document.getAuthor())
                .document(document)
                .build();
        histories.add(history);
        historyRepository.saveAll(histories);
        return histories;
    }

}
