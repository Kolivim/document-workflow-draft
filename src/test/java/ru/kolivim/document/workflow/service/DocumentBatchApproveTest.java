package ru.kolivim.document.workflow.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.SpyBean;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import ru.kolivim.document.workflow.dto.DocumentDto;
import ru.kolivim.document.workflow.dto.request.DocumentsRequestDto;
import ru.kolivim.document.workflow.dto.response.DocumentSubmitResponseDto;
import ru.kolivim.document.workflow.entity.Document;
import ru.kolivim.document.workflow.entity.History;
import ru.kolivim.document.workflow.entity.enums.Action;
import ru.kolivim.document.workflow.entity.enums.OperationStatus;
import ru.kolivim.document.workflow.entity.enums.Status;
import ru.kolivim.document.workflow.repository.DocumentRepository;
import ru.kolivim.document.workflow.repository.RegisterRepository;
import ru.kolivim.document.workflow.service.impl.DocumentServiceImpl;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.Statement;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.doThrow;

@Slf4j
@Testcontainers
@SpringBootTest
@RequiredArgsConstructor
public class DocumentBatchApproveTest {

    static {System.setProperty("liquibase.duplicateFileMode", "WARN");}

    @Container
    @ServiceConnection
    static PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:17.6");


    @DynamicPropertySource
    static void configureProperties(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", postgres::getJdbcUrl);
        registry.add("spring.datasource.username", postgres::getUsername);
        registry.add("spring.datasource.password", postgres::getPassword);
    }


    @Autowired
    private DocumentServiceImpl documentService;

    @Autowired
    private DocumentRepository documentRepository;

    @SpyBean
    private RegisterRepository registerRepositorySpy;

    private static final String AUTHOR_DRAFT = "batchDraft author";
    private static final String AUTHOR_SUBMIT = "batchSubmit author";
    private static final String AUTHOR_APPROVE = "batchApprove author";
    private static final String COMMENT_SUBMIT = "batchSubmit comment";
    private static final String COMMENT_APPROVE = "batchApprove comment";
    private static final String DOCUMENT_NAME_PREFIX = "batchNameTestApprove-";

    private static final String INNER_ID_PREFIX = "batchTestApprove-";


    @BeforeAll
    static void beforeAll() {
        postgres.start();
        createRole();
    }


    @AfterAll
    static void afterAll() {
        if (postgres != null && postgres.isRunning()) {
            postgres.stop();
        }
    }


    private static void createRole() {

        try (Connection conn = DriverManager.getConnection(
                postgres.getJdbcUrl(),
                postgres.getUsername(),
                postgres.getPassword());
             Statement stmt = conn.createStatement()) {

            stmt.execute("""
                    DO $$
                    BEGIN
                        IF NOT EXISTS (SELECT FROM pg_catalog.pg_roles WHERE rolname = 'docWf') THEN
                            CREATE ROLE "docWf" WITH LOGIN PASSWORD 'docWf';
                        END IF;
                    END
                    $$;
                    """);

            stmt.execute("GRANT CREATE ON SCHEMA public TO \"docWf\";");
            stmt.execute("GRANT ALL PRIVILEGES ON SCHEMA public TO \"docWf\";");

            log.info("Database роль 'docWf' создана");

        } catch (Exception e) {
            log.error("Exception при создании роли 'docWf'", e);
        }

    }


    @Test
    @DisplayName("Пакетный Approve - все документы в статусе SUBMIT успешно Утверждены")
    void batchApproveAllSucceed() {
        log.info("startMethod");

        Pageable pageable = PageRequest.of(0, 20, Sort.by("create_date").descending());

        int documentCount = 10;
        List<Long> docIds = createDocuments(documentCount);

        DocumentsRequestDto submitRequest = DocumentsRequestDto.builder()
                .ids(docIds)
                .author(AUTHOR_SUBMIT)
                .comment(COMMENT_SUBMIT)
                .build();

        List<DocumentSubmitResponseDto> submitResults = documentService.submit(pageable, submitRequest);


        DocumentsRequestDto approveRequest = DocumentsRequestDto.builder()
                .ids(docIds)
                .author(AUTHOR_APPROVE)
                .comment(COMMENT_APPROVE)
                .build();

        List<DocumentSubmitResponseDto> approveResults = documentService.approve(pageable, approveRequest);


        assertNotNull(approveResults);
        assertEquals(documentCount, approveResults.size(), "Должно вернуться столько же ответов, " +
                "сколько отправлено документов");

        for (int i = 0; i < approveResults.size(); i++) {

            DocumentSubmitResponseDto result = approveResults.get(i);

            assertEquals(docIds.get(i), result.getId(), "Id документа должен совпадать");

            assertEquals(OperationStatus.SUCCESS, result.getOperationStatus(),
                    String.format("Документ %d должен был быть успешно Утверждён", docIds.get(i)));

        }


        for (Long id : docIds) {

            Document doc = documentRepository.findById(id).orElse(null);
            assertNotNull(doc, "Документ должен существовать");
            assertEquals(Status.APPROVED, doc.getStatus(), String.format("Документ %d должен иметь статус APPROVED", id));
            assertEquals(AUTHOR_DRAFT, doc.getAuthor(), "Автор документа не должен измениться");

            assertEquals(2, doc.getHistorySet().size(), "Должно быть две записи в истории");

            List<History> historyList = doc.getHistorySet().stream()
                    .sorted(Comparator.comparing(History::getDate))
                    .collect(Collectors.toList());

            History historySubmit = historyList.get(0);
            assertEquals(Action.SUBMIT, historySubmit.getAction(), "Первое действие с документом должно быть SUBMIT");
            assertEquals(AUTHOR_SUBMIT, historySubmit.getAuthor(), "Авторы должны совпадать");
            assertEquals(COMMENT_SUBMIT, historySubmit.getComment(), "Комментарии должны совпадать");
            assertNotNull(historySubmit.getDate(), "Дата обновления в Истории не должна быть NULL");

            History historyApprove = historyList.get(1);
            assertEquals(Action.APPROVE, historyApprove.getAction(), "Действие с документом должно быть APPROVE");
            assertEquals(AUTHOR_APPROVE, historyApprove.getAuthor(), "Авторы должны совпадать");
            assertEquals(COMMENT_APPROVE, historyApprove.getComment(), "Комментарии должны совпадать");
            assertNotNull(historyApprove.getDate(), "Дата обновления в Истории не должна быть NULL");


        }

        log.info("endMethod, все {} документов успешно Утверждены", documentCount);
    }


    @Test
    @DisplayName("Пакетный Approve - различные результаты в пределах одного пакета (успех, конфликт, не найдено)")
    void batchApproveDifferentDocumentStage() {
        log.info("startMethod");

        /** Подготавливаем документы, во всех имеющихся статусах */
        List<Long> documentIds = createDocuments(30);
        List<Long> draftIds = documentIds.subList(0, 10);
        List<Long> submitIds = documentIds.subList(10, 30);
        List<Long> approveIds = documentIds.subList(20, 30);
        List<Long> notExistIds = List.of(-1L, 999999L, -1L, 999999L, -1L);

        Pageable pageable = PageRequest.of(0, 20, Sort.by("create_date").descending());
        documentService.submit(pageable, submitIds, AUTHOR_SUBMIT, COMMENT_SUBMIT);
        documentService.approve(pageable, approveIds, AUTHOR_APPROVE, COMMENT_APPROVE);
        /** с 20го по 30ый утвердили, исключаем их из списка отправленных на утверждение */
        submitIds = submitIds.subList(0, 10);

        List<Long> mixedIds = new ArrayList<>();
        mixedIds.addAll(draftIds);
        mixedIds.addAll(submitIds);
        mixedIds.addAll(approveIds);
        mixedIds.addAll(notExistIds);

        DocumentsRequestDto request = DocumentsRequestDto.builder()
                .ids(mixedIds)
                .author(AUTHOR_APPROVE)
                .comment(COMMENT_APPROVE)
                .build();


        List<DocumentSubmitResponseDto> results = documentService.approve(pageable, request);


        assertNotNull(results);
        assertEquals(mixedIds.size(), results.size(), "Должно вернуться столько же результатов, " +
                "сколько было отправлено Id документов");


        long successCount = results.stream().filter(r -> r.getOperationStatus() == OperationStatus.SUCCESS).count();
        long conflictCount = results.stream().filter(r -> r.getOperationStatus() == OperationStatus.CONFLICT).count();
        long notFoundCount = results.stream().filter(r -> r.getOperationStatus() == OperationStatus.NOT_FOUND).count();

        assertAll(
                () -> assertEquals(10, successCount, "Должно быть 10 успешных отправок"),
                () -> assertEquals(20, conflictCount, "Должно быть 20 конфликта (SUBMITTED и APPROVED)"),
                () -> assertEquals(5, notFoundCount, "Должен быть 5 NOT_FOUND")
        );


        for (int i = 0; i < draftIds.size(); i++) {

            Document doc = documentRepository.findById(draftIds.get(i)).get();
            assertEquals(Status.DRAFT, doc.getStatus(),
                    String.format("Документ исходного статуса Draft с Id: %d должен остаться DRAFT", draftIds.get(i)));
            assertEquals(AUTHOR_DRAFT, doc.getAuthor(), "Автор документа не должен измениться");
            assertEquals(draftIds.get(i), results.get(i).getId(), "Id документа должен совпадать");
            assertNull(doc.getUpdateDate(), "Дата обновления должна быть NULL");
            assertNull(doc.getRegister(), "Запись в реестре должна быть NULL");

            assertEquals(0, doc.getHistorySet().size(), "Не должно быть записей в истории");

            assertEquals(OperationStatus.CONFLICT, results.get(i).getOperationStatus(),
                    String.format("Операция с Документом %d должна завершится CONFLICT", draftIds.get(i)));

        }


        for (int i = 0; i < submitIds.size(); i++) {

            Long id = submitIds.get(i);
            DocumentSubmitResponseDto approveResponse = results.get(i + 10);

            Document doc = documentRepository.findById(id).get();
            assertEquals(Status.APPROVED, doc.getStatus(),
                String.format("Документ исходного статуса SUBMITTED с Id: %d должен быть успешно Утверждён", id));
            assertEquals(AUTHOR_DRAFT, doc.getAuthor(), "Автор документа не должен измениться");
            assertNotNull(doc.getUpdateDate(), "Дата обновления не должна быть NULL");
            assertNotNull(doc.getRegister(), "Запись в реестре не может быть NULL");

            assertEquals(id, approveResponse.getId(), "Id документа должен совпадать");

            assertEquals(OperationStatus.SUCCESS, approveResponse.getOperationStatus(),
                    String.format("Операция с Документом %d должна быть успешно выполнена", submitIds.get(i)));

            assertEquals(2, doc.getHistorySet().size(), "Должно быть две записи в истории");

            List<History> historyList = doc.getHistorySet().stream()
                    .sorted(Comparator.comparing(History::getDate))
                    .collect(Collectors.toList());

            History historySubmit = historyList.get(0);
            assertEquals(Action.SUBMIT, historySubmit.getAction(), "Первое действие с документом должно быть SUBMIT");
            assertEquals(AUTHOR_SUBMIT, historySubmit.getAuthor(), "Авторы должны совпадать");
            assertEquals(COMMENT_SUBMIT, historySubmit.getComment(), "Комментарии должны совпадать");
            assertNotNull(historySubmit.getDate(), "Дата обновления в Истории не должна быть NULL");

            History historyApprove = historyList.get(1);
            assertEquals(Action.APPROVE, historyApprove.getAction(), "Действие с документом должно быть APPROVE");
            assertEquals(AUTHOR_APPROVE, historyApprove.getAuthor(), "Авторы должны совпадать");
            assertEquals(COMMENT_APPROVE, historyApprove.getComment(), "Комментарии должны совпадать");
            assertNotNull(historyApprove.getDate(), "Дата обновления в Истории не должна быть NULL");
            assertEquals(historyApprove.getDate(), doc.getUpdateDate(), "Даты должны совпадать");

            assertNotEquals(historySubmit.getDate(), historyApprove.getDate(), "Даты не должны совпадать");

        }


        for (int i = 0; i < approveIds.size(); i++) {

            Long id = approveIds.get(i);
            DocumentSubmitResponseDto response = results.get(i + 20);

            Document doc = documentRepository.findById(id).get();
            assertEquals(Status.APPROVED, doc.getStatus(),
                    String.format("Документ исходного APPROVED SUBMITTED с Id: %d должен остаться APPROVED", id));
            assertEquals(AUTHOR_DRAFT, doc.getAuthor(), "Автор документа не должен измениться");
            assertEquals(id, response.getId(), "Id документа должен совпадать");
            assertNotNull(doc.getUpdateDate(), "Дата обновления не должна быть NULL");
            assertNotNull(doc.getRegister(), "Запись в реестре не может быть NULL");

            assertEquals(2, doc.getHistorySet().size(), "Должно быть две записи в истории");

            List<History> historyList = doc.getHistorySet().stream()
                    .sorted(Comparator.comparing(History::getDate))
                    .collect(Collectors.toList());

            History historySubmit = historyList.get(0);
            assertEquals(Action.SUBMIT, historySubmit.getAction(), "Первое действие с документом должно быть SUBMIT");
            assertEquals(AUTHOR_SUBMIT, historySubmit.getAuthor(), "Авторы должны совпадать");
            assertEquals(COMMENT_SUBMIT, historySubmit.getComment(), "Комментарии должны совпадать");
            assertNotNull(historySubmit.getDate(), "Дата обновления в Истории не должна быть NULL");

            History historyApprove = historyList.get(1);
            assertEquals(Action.APPROVE, historyApprove.getAction(), "Действие с документом должно быть APPROVE");
            assertEquals(AUTHOR_APPROVE, historyApprove.getAuthor(), "Авторы должны совпадать");
            assertEquals(COMMENT_APPROVE, historyApprove.getComment(), "Комментарии должны совпадать");
            assertNotNull(historyApprove.getDate(), "Дата обновления в Истории не должна быть NULL");
            assertEquals(historyApprove.getDate(), doc.getUpdateDate(), "Даты должны совпадать");

            assertEquals(OperationStatus.CONFLICT, results.get(i).getOperationStatus(),
                    String.format("Операция с Документом %d должна завершится CONFLICT", submitIds.get(i)));

        }


        for (int i = 0; i < notExistIds.size(); i++) {

            Long id = notExistIds.get(i);
            DocumentSubmitResponseDto response = results.get(i + 30);

            Optional<Document> optionalDoc = documentRepository.findById(id);
            assertFalse(optionalDoc.isPresent(), String.format("Документ с Id: %d не существует", id));

            assertEquals(OperationStatus.NOT_FOUND, response.getOperationStatus(),
                    String.format("Операция с Документом %d должна завершится NOT_FOUND", submitIds.get(i)));

        }


        log.info("endMethod, пакетный Approve, количество документов, со статусами операций, успех: {}, конфликт: {}, " +
                        "не найдено: {}", successCount, conflictCount, notFoundCount);
    }


    @Test
    @DisplayName("Пакетный Approve - с переданным пустым списком Id должен вернуться пустой результат")
    void batchApproveEmptyDocumentIdList() {
        log.info("startMethod");

        DocumentsRequestDto request = DocumentsRequestDto.builder()
                .ids(new ArrayList<>())
                .author(AUTHOR_APPROVE)
                .comment(COMMENT_APPROVE)
                .build();

        Pageable pageable = PageRequest.of(0, 20, Sort.by("create_date").descending());

        List<DocumentSubmitResponseDto> results = documentService.approve(pageable, request);

        assertNotNull(results);
        assertTrue(results.isEmpty(), "Результат должен быть пустым списком");

        log.info("Пакетный Approve: пустой список обработан корректно");
    }


    @Test
    @DisplayName("Пакетный Approve - при отсутствии списка должен вернуться пустой результат")
    void batchApproveNullDocumentIdList() {
        log.info("startMethod");

        DocumentsRequestDto request = DocumentsRequestDto.builder()
                .author(AUTHOR_APPROVE)
                .comment(COMMENT_APPROVE)
                .build();

        Pageable pageable = PageRequest.of(0, 20, Sort.by("create_date").descending());

        List<DocumentSubmitResponseDto> results = documentService.approve(pageable, request);

        assertNotNull(results);
        assertTrue(results.isEmpty(), "Результат должен быть пустым списком");

        log.info("Пакетный Approve: пустой список обработан корректно");
    }


    private List<Long> createDocuments(int count) {
        log.info("startMethod, count: {}", count);

        List<Long> ids = new ArrayList<>();

        for (int i = 0; i < count; i++) {

            DocumentDto request = DocumentDto.builder()
                    .name(DOCUMENT_NAME_PREFIX + i)
                    .author(AUTHOR_DRAFT)
                    .innerId(INNER_ID_PREFIX + i + "_" + Instant.now())
                    .build();

            DocumentDto response = documentService.create(request);
            ids.add(response.getId());

            log.debug("Создан документ со статусом DRAFT, id: {}", response.getId());

        }

        log.info("endMethod, к возврату ids: {}", ids);
        return ids;
    }


    @Test
    @DisplayName("Откат Approve при ошибке записи в регистр из-за выброса Exception'а")
    void approveRegisterRollbackWithThrow() {
        log.info("startMethod");

        /** Подменяем insertIfNotExists, чтобы он выбрасывал исключение */
        doThrow(new DataIntegrityViolationException("Нарушение внешнего ключа"))
                .when(registerRepositorySpy).insertIfNotExists(anyLong());


        List<Long> documentIds = createDocuments(1);
        Long docId = documentIds.get(0);

        Pageable pageable = PageRequest.of(0, 20, Sort.by("create_date").descending());
        documentService.submit(pageable, documentIds, AUTHOR_SUBMIT, COMMENT_SUBMIT);

        DocumentsRequestDto documentsRequestDto = DocumentsRequestDto.builder()
                .ids(documentIds)
                .author(AUTHOR_APPROVE)
                .comment(COMMENT_APPROVE)
                .build();


        List<DocumentSubmitResponseDto> response = documentService.approve(pageable, documentsRequestDto);


        assertNotNull(response);
        assertFalse(response.isEmpty());
        assertTrue(response.size() == 1, "Должен быть один документ в ответе");
        assertEquals(OperationStatus.CONFLICT, response.get(0).getOperationStatus(),
                String.format("Операция с Документом %d должна завершится CONFLICT", response.get(0).getId()));
        assertEquals(documentIds.get(0), response.get(0).getId(), "Id документа должен совпадать");


        Optional<Document> optionalDocument = documentRepository.findById(docId);
        assertTrue(optionalDocument.isPresent(),"Документ должен существовать");
        Document document = optionalDocument.get();
        assertEquals(Status.SUBMITTED, document.getStatus(),
                "Статус документа при ошибке регистрации не должен измениться с SUBMITTED");

        assertNull(document.getRegister(), "Запись в реестре должна быть NULL");

        assertEquals(1, document.getHistorySet().size(), "Должна быть одна запись в истории");

        List<History> historyList = new ArrayList<>(document.getHistorySet());
        History historySubmit = historyList.get(0);
        assertEquals(Action.SUBMIT, historySubmit.getAction(), "Первое действие с документом должно быть SUBMIT");
        assertEquals(AUTHOR_SUBMIT, historySubmit.getAuthor(), "Авторы должны совпадать");
        assertEquals(COMMENT_SUBMIT, historySubmit.getComment(), "Комментарии должны совпадать");
        assertNotNull(historySubmit.getDate(), "Дата обновления в Истории не должна быть NULL");

        assertEquals(historySubmit.getDate(), document.getUpdateDate(), "Даты должны совпадать");

        log.info("endMethod");
    }


    @Test
    @DisplayName("Откат Approve при ошибке записи в регистр из-за того, что запись уже есть")
    void approveRegisterRollbackInsertReturnsZero() {
        log.info("startMethod");

        /** Cимулирует ситуацию, когда запись уже существует */
        doReturn(0).when(registerRepositorySpy).insertIfNotExists(anyLong());


        List<Long> documentIds = createDocuments(1);
        Long docId = documentIds.get(0);

        Pageable pageable = PageRequest.of(0, 20, Sort.by("create_date").descending());
        documentService.submit(pageable, documentIds, AUTHOR_SUBMIT, COMMENT_SUBMIT);

        DocumentsRequestDto documentsRequestDto = DocumentsRequestDto.builder()
                    .ids(documentIds)
                    .author(AUTHOR_APPROVE)
                    .comment(COMMENT_APPROVE)
                    .build();


        List<DocumentSubmitResponseDto> response = documentService.approve(pageable, documentsRequestDto);


        assertNotNull(response);
        assertFalse(response.isEmpty());
        assertTrue(response.size() == 1, "Должен быть один документ в ответе");
        assertEquals(OperationStatus.CONFLICT, response.get(0).getOperationStatus(),
                String.format("Операция с Документом %d должна завершится CONFLICT", response.get(0).getId()));
        assertEquals(documentIds.get(0), response.get(0).getId(), "Id документа должен совпадать");


        Optional<Document> optionalDocument = documentRepository.findById(docId);
        assertTrue(optionalDocument.isPresent(),"Документ должен существовать");
        Document document = optionalDocument.get();
        assertEquals(Status.SUBMITTED, document.getStatus(),
                "Статус документа при ошибке регистрации не должен измениться с SUBMITTED");

        assertNull(document.getRegister(), "Запись в реестре должна быть NULL");

        log.info("endMethod");
    }


    /** @param ids список id документов в статусе DRAFT */
    private void submitDocuments(List<Long> ids) {
        log.info("startMethod, ids: {}", ids);

        for (Long id : ids) {

            DocumentsRequestDto request = DocumentsRequestDto.builder()
                    .ids(ids)
                    .author(AUTHOR_SUBMIT)
                    .comment(COMMENT_SUBMIT)
                    .build();

            Pageable pageable = PageRequest.of(0, 20, Sort.by("create_date").descending());

            List<DocumentSubmitResponseDto> results = documentService.submit(pageable, request);

        }

        log.info("endMethod");
    }


    /** @param ids список id документов в статусе SUBMITTED */
    private void approveDocuments(List<Long> ids) {
        log.info("startMethod, ids: {}", ids);

        for (Long id : ids) {

            DocumentsRequestDto request = DocumentsRequestDto.builder()
                    .ids(ids)
                    .author(AUTHOR_APPROVE)
                    .comment(COMMENT_APPROVE)
                    .build();

            Pageable pageable = PageRequest.of(0, 20, Sort.by("create_date").descending());

            List<DocumentSubmitResponseDto> results = documentService.approve(pageable, request);

        }

        log.info("endMethod");
    }

}