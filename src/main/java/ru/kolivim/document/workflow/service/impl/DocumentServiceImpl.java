package ru.kolivim.document.workflow.service.impl;

import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DataAccessException;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.data.repository.query.Param;
import org.springframework.transaction.support.TransactionTemplate;
import ru.kolivim.document.workflow.dto.DocumentDto;
import ru.kolivim.document.workflow.dto.request.SearchDocumentDto;
import ru.kolivim.document.workflow.dto.request.DocumentsRequestDto;
import ru.kolivim.document.workflow.dto.response.DocumentSubmitResponseDto;
import ru.kolivim.document.workflow.dto.response.DocumentPage;
import ru.kolivim.document.workflow.dto.response.PageResponseDto;
import ru.kolivim.document.workflow.entity.Document;
import ru.kolivim.document.workflow.entity.Document_;
import ru.kolivim.document.workflow.entity.History;
import ru.kolivim.document.workflow.entity.Register;
import ru.kolivim.document.workflow.entity.enums.Action;
import ru.kolivim.document.workflow.entity.enums.OperationStatus;
import ru.kolivim.document.workflow.entity.enums.Status;
import ru.kolivim.document.workflow.exception.RegisterSaveException;
import ru.kolivim.document.workflow.exception.ResourceNotFoundException;
import ru.kolivim.document.workflow.mapper.DocumentMapper;
import ru.kolivim.document.workflow.repository.DocumentRepository;
import ru.kolivim.document.workflow.repository.HistoryRepository;
import ru.kolivim.document.workflow.repository.RegisterRepository;
import ru.kolivim.document.workflow.service.DocumentService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import ru.kolivim.document.workflow.util.specification.SpecificationUtils;

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

    private final RegisterRepository registerRepository;

    private final TransactionTemplate transactionTemplate;

    private final DocumentMapper documentMapper;




    /**
     @return Page со списком документов, удовлетворяющих полученным в параметрах условиям,
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

        Page<Document> documents = documentRepository.findAll(documentSpecification, pageable);
        Page<DocumentDto> documentsDto = documents.map(documentMapper::entityToDto);

        log.info("endMethod, к возврату Page<DocumentDto>: {}", documentsDto);
        return documentsDto;
    }


    /**
     @return Page со списком документов, удовлетворяющих полученным в параметрах условиям,
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
        log.info("startMethod, documentDto: {}", documentDto);

        Document document = documentMapper.dtoToNewEntity(documentDto);

        DocumentDto returnDocumentDto = documentMapper.entityToDto(documentRepository.save(document));

        log.info("endMethod, к возврату documentDto: {}", documentDto);

        return returnDocumentDto;
    }


    @Override
    public Document getDocumentById(Long id) {
        log.info("startMethod, id: {}", id);
        return documentRepository.findById(id).orElseThrow(() -> new ResourceNotFoundException("Document not found"));
    }


    @Override
    public Optional<Document> getDocumentOptionalById(Long id) {
        log.info("startMethod, id: {}", id);
        return documentRepository.findById(id);
    }


    @Override
    public Optional<Status> getStatusOptionalById(Long id) {
        log.info("startMethod, id: {}", id);
        return documentRepository.findStatusById(id);
    }


    @Override
    public long registerCountByDocumentId(Long documentId) {
        log.info("startMethod, documentId: {}", documentId);
        return registerRepository.countByDocumentId(documentId);
    }


    @Override
    public DocumentDto getById(Long id) {
        log.info("startMethod, id: {}", id);

        Document document = documentRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Документ не найден для id: ".concat(id.toString())));

        DocumentDto returnDocumentDto = documentMapper.entityToDto(document);

        return returnDocumentDto;
    }


    @Override
    public Page<DocumentDto> getByIdList(Pageable pageable, List<Long> idList) {
        log.info("startMethod, idList: {}", idList);

        Specification documentSpecification = SpecificationUtils.in(Document_.ID, idList);
        Page<Document> documents = documentRepository.findAll(documentSpecification, pageable);
        Page<DocumentDto> documentsDto = documents.map(documentMapper::entityToDto);

        log.info("endMethod, Page documentsDto: {}", documentsDto);
        return documentsDto;
    }


    @Override
    public PageResponseDto getByIdListWithNoFound(Pageable pageable, List<Long> idList) {
        log.info("startMethod, idList: {}", idList);

        Specification documentSpecification = SpecificationUtils.in(Document_.ID, idList);

        Page<Document> documents = documentRepository.findAll(documentSpecification, pageable);
        Page<DocumentDto> documentsDto = documents.map(documentMapper::entityToDto);

        List<Long> notExistingIdList = getNotExistingIds(idList);

        PageResponseDto pageResponseDto = new PageResponseDto(documentsDto, notExistingIdList,
                notExistingIdList.size(), idList.size());

        log.info("endMethod, к возврату pageResponse: {}", pageResponseDto);
        return pageResponseDto;
    }


    @Override
    public DocumentPage getByIdListWithExtendedPage(Pageable pageable, List<Long> idList) {
        log.info("startMethod, idList: {}, pageable: {}", idList, pageable);

        Specification documentSpecification = SpecificationUtils.in(Document_.ID, idList);

        Page<Document> documents = documentRepository.findAll(documentSpecification, pageable);
        Page<DocumentDto> documentsDto = documents.map(documentMapper::entityToDto);

        List<Long> notExistingIdList = getNotExistingIds(idList);

        log.info("endMethod, Page<DocumentDto> documentsDto: {}", documentsDto);

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
        log.info("startMethod, documentsRequestDto: {}, pageable: {}", documentsRequestDto, pageable);
        return submit(pageable, documentsRequestDto.getIds(), documentsRequestDto.getAuthor(), documentsRequestDto.getComment());
    }


    @Transactional
    public List<DocumentSubmitResponseDto> submit(Pageable pageable, List<Long> idList, String author, String comment) {
        log.info("startMethod, начало отправки на утверждение (SUBMITTED) полученного списка размером: {}, " +
                        "documentsRequestDto: {}, author: {}, comment:{}, pageable: {}",
                idList != null ? idList.size() : "NULL", idList, author, comment, pageable);

        List<DocumentSubmitResponseDto> documentSubmitResponseDtoList = new ArrayList<>();

        if(idList == null) return documentSubmitResponseDtoList;

        for (int i = 1; i <= idList.size(); i++) {

            Long documentId = idList.get(i-1);

            try {

                DocumentSubmitResponseDto result = submit(documentId, author, comment);
                documentSubmitResponseDtoList.add(result);

                log.info("Документ {} из {}, с Id: {} отправлен на утверждение (перевод в статус SUBMITTED) со статусом" +
                                "операции OperationStatus: {}",
                        i, idList.size(), documentId, result.getOperationStatus());


            } catch (Exception e) {
                log.error("Ошибка при обработке документа {} из {}, с Id {}: {}",
                        i, idList.size(), documentId, e.getMessage());
                documentSubmitResponseDtoList.add(new DocumentSubmitResponseDto(documentId,
                        OperationStatus.REGISTER_ERROR));
            }

        }


        log.info("endMethod, завершена отправка на утверждение (SUBMITTED) полученного списка размером: {}, " +
                "submitDocumentDtoList: {}", idList != null ? idList.size() : "NULL", documentSubmitResponseDtoList);
        return documentSubmitResponseDtoList;
    }


    /** Обработка одного документа в отдельной транзакции */
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public DocumentSubmitResponseDto submit(Long documentId, String author, String comment) {
        log.info("Начало обработки документа c Id: {}, author: {}, comment: {}", documentId, author, comment);

        try {

            boolean isDocumentExist = documentRepository.existsById(documentId);

            if (!isDocumentExist) {
                log.info("Документ c Id: {} не найден", documentId);
                return new DocumentSubmitResponseDto(documentId, OperationStatus.NOT_FOUND);
            }


            /** Атомарное обновление статуса: */
            int updatedCount = documentRepository.updateStatusIfExpected(
                    documentId,
                    Status.DRAFT,
                    Status.SUBMITTED
            );


            if (updatedCount == 0) {

                Status newStatus = documentRepository.findStatusById(documentId).orElse(Status.DRAFT);

                log.warn("Конфликт при обновлении документа {}: {}", documentId, newStatus == Status.SUBMITTED ?
                        "Документ уже в статусе SUBMITTED (конкурирующее обновление)"
                        : String.format("Статус документа изменился на %s", newStatus));
                return new DocumentSubmitResponseDto(documentId, OperationStatus.CONFLICT);

            } else {

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


                log.info("Документ с Id: {} успешно обработан, статус изменен с DRAFT на SUBMITTED", documentId);
                return new DocumentSubmitResponseDto(documentId, OperationStatus.SUCCESS);

            }


        } catch (DataAccessException e) {
            log.error("Ошибка базы данных при обработке документа {}: {}", documentId, e.getMessage());
            return new DocumentSubmitResponseDto(documentId, OperationStatus.REGISTER_ERROR);
        } catch (Exception e) {
            log.error("Неожиданная ошибка при обработке документа {}: {}", documentId, e.getMessage(), e);
            return new DocumentSubmitResponseDto(documentId, OperationStatus.REGISTER_ERROR);
        }

    }


    /** Пакетная обработка Approve: */
    @Override
    public List<DocumentSubmitResponseDto> approve(Pageable pageable, DocumentsRequestDto documentsRequestDto) {
        log.info("startMethod, documentsRequestDto: {}, pageable: {}", documentsRequestDto, pageable);
        return approve(pageable, documentsRequestDto.getIds(), documentsRequestDto.getAuthor(), documentsRequestDto.getComment());
    }


    @Transactional
    public List<DocumentSubmitResponseDto> approve(Pageable pageable, List<Long> idList, String author, String comment) {
        log.info("startMethod, начало утверждения (APPROVED) полученного списка размером: {}, documentsRequestDto: {}, " +
                        "author: {}, comment:{}, pageable: {}", idList != null ? idList.size() : "NULL",
                        idList, author, comment, pageable);

        List<DocumentSubmitResponseDto> documentSubmitResponseDtoList = new ArrayList<>();

        if(idList == null) return documentSubmitResponseDtoList;

        for (int i = 1; i <= idList.size(); i++) {

            Long documentId = idList.get(i-1);

            DocumentSubmitResponseDto result = null;

            try {

                result = approveWithRollback(documentId, author, comment);
                documentSubmitResponseDtoList.add(result);

            } catch (RegisterSaveException e) {

                log.info("Обработан RegisterSaveException с e.getDocumentId(): {} для documentId: {}, утверждение " +
                                "(перевод в статус APPROVE) завершено со статусом операции OperationStatus: {}",
                        e.getDocumentId(), documentId, OperationStatus.CONFLICT);

                result = new DocumentSubmitResponseDto(e.getDocumentId(), OperationStatus.CONFLICT);
                documentSubmitResponseDtoList.add(result);

            }


            /*
            DocumentSubmitResponseDto result = approveWithRollback(documentId, author, comment);
            documentSubmitResponseDtoList.add(result);
            */


            log.info("Документ {} из {}, с Id: {} утверждение (перевод в статус APPROVE) завершено со статусом " +
                    "операции OperationStatus: {}", i, idList.size(), documentId,
                    result == null ? "NULL" : result.getOperationStatus());

        }


        log.info("endMethod, завершено утверждение (APPROVED) полученного списка размером: {}, к возврату " +
                "submitDocumentDtoList: {}", idList != null ? idList.size() : "NULL", documentSubmitResponseDtoList);
        return documentSubmitResponseDtoList;
    }


    /** Approve одного документа в отдельной транзакции */
    @Transactional(propagation = Propagation.REQUIRES_NEW, rollbackFor = {RegisterSaveException.class, RuntimeException.class})
    public DocumentSubmitResponseDto approve(Long documentId, String author, String comment) {
        log.info("Начало обработки документа c Id: {}, author: {}, comment: {}", documentId, author, comment);


        try {

            boolean isDocumentExist = documentRepository.existsById(documentId);

            if (!isDocumentExist) {
                log.info("Документ c Id: {} не найден", documentId);
                return new DocumentSubmitResponseDto(documentId, OperationStatus.NOT_FOUND);
            }


            boolean isDocumentStatusSubmitted = documentRepository.existsByIdAndStatus(documentId, Status.SUBMITTED);
            if (!isDocumentStatusSubmitted) {
                log.info("Документ c Id: {} имеет статус, отличный от SUBMITTED", documentId);
                return new DocumentSubmitResponseDto(documentId, OperationStatus.CONFLICT);
            }


            /** Запись в Реестр: */
            try {

                int registerInserted = registerRepository.insertIfNotExists(documentId);

                if (registerInserted == 0) {                                                                            /** Запись уже есть */

                    log.info("Конфликт при создании записи в Реестре для документа {}, запись уже существует", documentId);

                    return new DocumentSubmitResponseDto(documentId, OperationStatus.CONFLICT);

                } else {

                    /** Обновление статуса: */
                    int updatedCount = documentRepository.updateStatusIfExpected(
                            documentId,
                            Status.SUBMITTED,
                            Status.APPROVED
                    );


                    if (updatedCount == 0) {

                        /** Статус мог измениться в другом потоке */
                        Status newStatus = documentRepository.findStatusById(documentId).orElse(Status.SUBMITTED);

                        log.info("Конфликт при обновлении документа {}: {}", documentId, newStatus == Status.APPROVED ?
                                "Документ уже в статусе APPROVED (конкурирующее обновление)"
                                : String.format("Статус документа изменился на %s", newStatus));

                        return new DocumentSubmitResponseDto(documentId, OperationStatus.CONFLICT);

                    } else {

                        Document document = documentRepository.getById(documentId);

                        /** Запись в историю: */
                        History history = History.builder()
                                .document(document)
                                .action(Action.APPROVE)
                                .author(author != null ? author : "SYSTEM")
                                .comment(comment)
                                .date(document.getUpdateDate())
                                .build();

                        historyRepository.save(history);


                        log.info("Документ с Id: {} успешно обработан, статус изменен с SUBMITTED на APPROVED", documentId);
                        return new DocumentSubmitResponseDto(documentId, OperationStatus.SUCCESS);

                    }

                }

            } catch (DataAccessException e) {

                log.error("Ошибка БД при сохранении в реестр для документа {}: {}", documentId, e.getMessage());

                return new DocumentSubmitResponseDto(documentId, OperationStatus.REGISTER_ERROR);

            }


        } catch (DataAccessException e) {
            log.error("Ошибка базы данных при обработке документа {}: {}", documentId, e.getMessage());
            return new DocumentSubmitResponseDto(documentId, OperationStatus.REGISTER_ERROR);
        } catch (RegisterSaveException e) {
            log.error("Ошибка при сохранении документа {}: {}", documentId, e.getMessage(), e);
            return new DocumentSubmitResponseDto(documentId, OperationStatus.REGISTER_ERROR);
        } catch (Exception e) {
            log.error("Ошибка при обработке документа {}: {}", documentId, e.getMessage(), e);
            return new DocumentSubmitResponseDto(documentId, OperationStatus.REGISTER_ERROR);
        }

    }


    @Deprecated
    @Transactional
    public List<DocumentSubmitResponseDto> approveRollb(Pageable pageable, List<Long> idList, String author, String comment) {
        log.info("startMethod, начало утверждения (APPROVED) полученного списка размером: {}, documentsRequestDto: {}, " +
                        "author: {}, comment:{}, pageable: {}", idList != null ? idList.size() : "NULL",
                idList, author, comment, pageable);

        List<DocumentSubmitResponseDto> documentSubmitResponseDtoList = new ArrayList<>();

        if(idList == null) return documentSubmitResponseDtoList;

        for (int i = 1; i <= idList.size(); i++) {

            Long documentId = idList.get(i-1);

            DocumentSubmitResponseDto result = approveWithRollbackNoException(documentId, author, comment);
            documentSubmitResponseDtoList.add(result);

            log.info("Документ {} из {}, с Id: {} утверждение (перевод в статус APPROVE) завершено со статусом " +
                            "операции OperationStatus: {}", i, idList.size(), documentId,
                    result == null ? "NULL" : result.getOperationStatus());

        }


        log.info("endMethod, завершено утверждение (APPROVED) полученного списка размером: {}, к возврату " +
                "submitDocumentDtoList: {}", idList != null ? idList.size() : "NULL", documentSubmitResponseDtoList);
        return documentSubmitResponseDtoList;
    }


    @Deprecated
    @Transactional(propagation = Propagation.REQUIRES_NEW,
            rollbackFor = {RegisterSaveException.class, RuntimeException.class})
    public DocumentSubmitResponseDto approveWithRollbackNoException(Long documentId, String author, String comment) {

        log.info("Начало обработки документа c Id: {}, author: {}, comment: {}", documentId, author, comment);

        return transactionTemplate.execute(status -> {

            log.info("Начало транзакции для обработки документа c Id: {}, author: {}, comment: {}",
                    documentId, author, comment);


            boolean isDocumentExist = documentRepository.existsById(documentId);

            if (!isDocumentExist) {
                log.info("Документ c Id: {} не найден", documentId);
                return new DocumentSubmitResponseDto(documentId, OperationStatus.NOT_FOUND);
            }


            /** Обновление статуса документа: */
            int updatedDocumentStatusCount = documentRepository.updateStatusIfExpected(
                    documentId,
                    Status.SUBMITTED,
                    Status.APPROVED
            );

            if (updatedDocumentStatusCount == 0) {
                Status newStatus = documentRepository.findStatusById(documentId).orElse(Status.SUBMITTED);
                log.info("Конфликт при обновлении статуса документа {}: {}", documentId, newStatus);
                status.setRollbackOnly();
                return new DocumentSubmitResponseDto(documentId, OperationStatus.CONFLICT);
            }


            /** Запись в историю: */
            Document document = documentRepository.getById(documentId);


            History history = History.builder()
                    .document(document)
                    .action(Action.APPROVE)
                    .author(author != null ? author : "SYSTEM")
                    .comment(comment)
                    .date(document.getUpdateDate())
                    .build();

            History savedHistory = historyRepository.save(history);

            if (savedHistory == null || savedHistory.getId() == null) {
                log.info("Не удалось создать запись в Истории для documentId: {}, savedHistory: {}",
                        documentId, savedHistory);
                status.setRollbackOnly();
                return new DocumentSubmitResponseDto(documentId, OperationStatus.CONFLICT);
            }


            /** Запись в реестр: */
            try {

                int registerInserted = registerRepository.insertIfNotExists(/*Long.valueOf(-1) */ documentId );

                if (registerInserted == 0) {
                    log.info("Не удалось создать запись в реестре для documentId: {}, registerInserted: {}",            /** Запись уже существует */
                            documentId, registerInserted);
                    status.setRollbackOnly();
                    return new DocumentSubmitResponseDto(documentId, OperationStatus.CONFLICT);
                }

            } catch (DataAccessException e) {
                log.error("Ошибка БД при сохранении в реестр для документа {}: {}", documentId, e.getMessage());        /** ERROR БД, в т.ч. по FK */
                status.setRollbackOnly();
                return new DocumentSubmitResponseDto(documentId, OperationStatus.REGISTER_ERROR);
            }


            log.info("Конец транзакции для документа с Id: {}, документ успешно утверждён", documentId);
            return new DocumentSubmitResponseDto(documentId, OperationStatus.SUCCESS);
        });

    }


    /** Approve одного документа в отдельной транзакции */
    @Transactional(propagation = Propagation.REQUIRES_NEW,
            rollbackFor = {RegisterSaveException.class, RuntimeException.class})
    public DocumentSubmitResponseDto approveWithRollback(Long documentId, String author, String comment) {

        log.info("Начало обработки документа c Id: {}, author: {}, comment: {}", documentId, author, comment);

        return transactionTemplate.execute(status -> {

            log.info("Начало транзакции для обработки документа c Id: {}, author: {}, comment: {}",
                    documentId, author, comment);


            boolean isDocumentExist = documentRepository.existsById(documentId);

            if (!isDocumentExist) {
                log.info("Документ c Id: {} не найден", documentId);
                return new DocumentSubmitResponseDto(documentId, OperationStatus.NOT_FOUND);
            }


            /** Обновление статуса документа: */
            int updatedDocumentStatusCount = documentRepository.updateStatusIfExpected(
                    documentId,
                    Status.SUBMITTED,
                    Status.APPROVED
            );

            if (updatedDocumentStatusCount == 0) {
                Status newStatus = documentRepository.findStatusById(documentId).orElse(Status.SUBMITTED);
                log.info("Конфликт при обновлении статуса документа {}: {}", documentId, newStatus);
                status.setRollbackOnly();
//                return new DocumentSubmitResponseDto(documentId, OperationStatus.CONFLICT);
                throw new RegisterSaveException("Ошибка сохранения записи в реестр для документа с id: "
                        .concat(documentId.toString()), documentId);
            }


            /** Запись в историю: */
            Document document = documentRepository.getById(documentId);


            History history = History.builder()
                    .document(document)
                    .action(Action.APPROVE)
                    .author(author != null ? author : "SYSTEM")
                    .comment(comment)
                    .date(document.getUpdateDate())
                    .build();

            History savedHistory = historyRepository.save(history);

            if (savedHistory == null || savedHistory.getId() == null) {
                log.info("Не удалось создать запись в Истории для documentId: {}, savedHistory: {}",
                        documentId, savedHistory);
                status.setRollbackOnly();
//                return new DocumentSubmitResponseDto(documentId, OperationStatus.CONFLICT);
                throw new RegisterSaveException("Ошибка сохранения записи в реестр для документа с id: "
                        .concat(documentId.toString()), documentId);
            }


            /** Запись в реестр: */
            try {

                int registerInserted = registerRepository.insertIfNotExists(/*Long.valueOf(-1) */ documentId );

                if (registerInserted == 0) {
                    log.info("Не удалось создать запись в реестре для documentId: {}, registerInserted: {}",            /** Запись уже существует */
                            documentId, registerInserted);
                    status.setRollbackOnly();
//                    return new DocumentSubmitResponseDto(documentId, OperationStatus.CONFLICT);
                    throw new RegisterSaveException("Ошибка сохранения записи в реестр для документа с id: "
                            .concat(documentId.toString()), documentId);
                }

            } catch (DataAccessException e) {
                log.error("Ошибка БД при сохранении в реестр для документа {}: {}", documentId, e.getMessage());        /** ERROR БД, в т.ч. по FK */
                status.setRollbackOnly();
//                return new DocumentSubmitResponseDto(documentId, OperationStatus.REGISTER_ERROR);
                throw new RegisterSaveException("Ошибка сохранения записи в реестр для документа с id: "
                        .concat(documentId.toString()), documentId);
            }


            log.info("Конец транзакции для документа с Id: {}, документ успешно утверждён", documentId);
            return new DocumentSubmitResponseDto(documentId, OperationStatus.SUCCESS);
        });

    }


    /** Approve одного документа в отдельной транзакции */
    @Deprecated
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public DocumentSubmitResponseDto approveOld(Long documentId, String author, String comment) {
        log.info("Начало обработки документа c Id: {}, author: {}, comment: {}", documentId, author, comment);

        try {

            boolean isDocumentExist = documentRepository.existsById(documentId);

            if (!isDocumentExist) {
                log.info("Документ c Id: {} не найден", documentId);
                return new DocumentSubmitResponseDto(documentId, OperationStatus.NOT_FOUND);
            }


            /** Атомарное обновление статуса: */
            int updatedCount = documentRepository.updateStatusIfExpected(
                    documentId,
                    Status.SUBMITTED,
                    Status.APPROVED
            );


            if (updatedCount == 0) {

                Status newStatus = documentRepository.findStatusById(documentId).orElse(Status.SUBMITTED);

                log.warn("Конфликт при обновлении документа {}: {}", documentId, newStatus == Status.APPROVED ?
                        "Документ уже в статусе APPROVED (конкурирующее обновление)"
                        : String.format("Статус документа изменился на %s", newStatus));

                return new DocumentSubmitResponseDto(documentId, OperationStatus.CONFLICT);

            } else {

                Document documentProxy = documentRepository.getReferenceById(documentId);

                /** Запись в историю: */
                History history = History.builder()
                        .document(documentProxy)
                        .action(Action.APPROVE)
                        .author(author != null ? author : "SYSTEM")
                        .comment(comment)
                        .date(ZonedDateTime.now())
                        .build();

                historyRepository.save(history);


                /** Запись в Реестр: */
                Register register = Register.builder()
                        .document(documentProxy)
                        .id(documentId)
                        .build();

                Register saveRegister = registerRepository.save(register);


                if(saveRegister == null && saveRegister.getId() == null && saveRegister.getId() != documentId) {
                    throw new RuntimeException("Ошибка сохранения в Реестре для документа с id: "
                            .concat(String.valueOf(documentId)));
                }

                log.info("Документ с Id: {} успешно обработан, статус изменен с SUBMITTED на APPROVED", documentId);
                return new DocumentSubmitResponseDto(documentId, OperationStatus.SUCCESS);

            }


        } catch (DataAccessException e) {
            log.error("Ошибка базы данных при обработке документа {}: {}", documentId, e.getMessage());
            return new DocumentSubmitResponseDto(documentId, OperationStatus.REGISTER_ERROR);
        } catch (Exception e) {
            log.error("Неожиданная ошибка при обработке документа {}: {}", documentId, e.getMessage(), e);
            return new DocumentSubmitResponseDto(documentId, OperationStatus.REGISTER_ERROR);
        }

    }


    public List<Long> getNotExistingIds(List<Long> idList) {
        log.info("startMethod, idList: {}", idList);

        List<Long> existingIdInList = documentRepository.findAllExistingIds(idList);
        Set<Long> existingIdSet = new HashSet<>(existingIdInList);

        List<Long> notExistingIdInList = idList.stream()
                .filter(id -> !existingIdSet.contains(id))
                .collect(Collectors.toList());

        log.info("endMethod, notExistingIdInList: {}", notExistingIdInList);
        return notExistingIdInList;
    }


    public List<Document> getByStatus(Status status, PageRequest pageRequest) {
        log.info("startMethod, status: {}", status);

        List<Document> documents = documentRepository.findDocumentsByStatus(status, pageRequest);

        log.info("endMethod, к возврату documents: {} для status : {}", documents, status);
        return documents;
    }


    public static UUID generateInnerId(String author, String name, ZonedDateTime date) {
        log.info("startMethod, author: {}, name: {}, date: {}", author, name, date);

        DateTimeFormatter formatter = DateTimeFormatter.ISO_INSTANT;
        String formattedDate = date.format(formatter);

        String source = String.format("%s|%s|%s",
                author != null ? author : "",
                name != null ? name : "",
                formattedDate
        );

        log.info("endMethod");
        return UUID.nameUUIDFromBytes(source.getBytes(StandardCharsets.UTF_8));
    }

}
