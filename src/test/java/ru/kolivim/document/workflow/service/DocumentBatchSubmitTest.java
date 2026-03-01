package ru.kolivim.document.workflow.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
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
import ru.kolivim.document.workflow.entity.Register;
import ru.kolivim.document.workflow.entity.enums.Action;
import ru.kolivim.document.workflow.entity.enums.OperationStatus;
import ru.kolivim.document.workflow.entity.enums.Status;
import ru.kolivim.document.workflow.repository.DocumentRepository;
import ru.kolivim.document.workflow.service.impl.DocumentServiceImpl;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.Statement;
import java.time.*;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

import static org.junit.jupiter.api.Assertions.*;

@Slf4j
@Testcontainers
@SpringBootTest
@RequiredArgsConstructor
public class DocumentBatchSubmitTest {

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

    private static final String AUTHOR_DRAFT = "batchDraft author";
    private static final String AUTHOR_SUBMIT = "batchSubmit author";
    private static final String AUTHOR_APPROVE = "batchApprove author";
    private static final String COMMENT_SUBMIT = "batchSubmit comment";
    private static final String COMMENT_APPROVE = "batchApprove comment";
    private static final String DOCUMENT_NAME_PREFIX = "batchNameTestSubmit-";

//    private static final String DOCUMENT_DESCRIPTION_TEST = "test description ";

    private static final String INNER_ID_PREFIX = "batchTestSubmit-";


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
    @DisplayName("Пакетный Submit - все документы в статусе DRAFT успешно отправлены")
    void batchSubmitAllSucceed() {
        log.info("startMethod");

        int documentCount = 10;
        List<Long> draftIds = createDocuments(documentCount);

        DocumentsRequestDto request = DocumentsRequestDto.builder()
                .ids(draftIds)
                .author(AUTHOR_SUBMIT)
                .comment(COMMENT_SUBMIT)
                .build();

        Pageable pageable = PageRequest.of(0, 20, Sort.by("create_date").descending());

        List<DocumentSubmitResponseDto> results = documentService.submit(pageable, request);

        assertNotNull(results);
        assertEquals(documentCount, results.size(), "Должно вернуться столько же ответов, " +
                "сколько отправлено документов");

        for (int i = 0; i < results.size(); i++) {

            DocumentSubmitResponseDto result = results.get(i);

            assertEquals(draftIds.get(i), result.getId(), "Id документа должен совпадать");

            assertEquals(OperationStatus.SUCCESS, result.getOperationStatus(),
                    String.format("Документ %d должен был быть успешно отправленным", draftIds.get(i)));

        }


        for (Long id : draftIds) {

            Document doc = documentRepository.findById(id).orElse(null);
            assertNotNull(doc, "Документ должен существовать");
            assertEquals(Status.SUBMITTED, doc.getStatus(), String.format("Документ %d должен иметь статус SUBMITTED", id));

            assertEquals(1, doc.getHistorySet().size(), "Должна быть одна запись в истории");

            History history = doc.getHistorySet().iterator().next();
            assertEquals(Action.SUBMIT, history.getAction());
            assertEquals(AUTHOR_SUBMIT, history.getAuthor());
            assertEquals(COMMENT_SUBMIT, history.getComment());

        }

        log.info("endMethod, все {} документов успешно отправлены", documentCount);
    }


    @Test
    @DisplayName("Пакетный submit - различные результаты в пределах одного пакета (успех, конфликт, не найдено)")
    void batchSubmitDifferentDocumentStage() {
        log.info("startMethod");

//        ZonedDateTime submittedDateTime = ZonedDateTime.now(ZoneOffset.UTC);
//        LocalDate startLocalDate = submittedDateTime.toLocalDate();


        /** Подготавливаем документы, во всех имеющихся статусах */
        List<Long> documentIds = createDocuments(30);
        List<Long> draftIds = documentIds.subList(0, 10);
        List<Long> submitIds = documentIds.subList(10, 30);
        List<Long> approveIds = documentIds.subList(20, 30);
        List<Long> notExistIds = List.of(-1L, 999999L, -1L, 999999L, -1L);

        Pageable pageable = PageRequest.of(0, 20, Sort.by("create_date").descending());
        documentService.submit(pageable, submitIds, AUTHOR_SUBMIT, COMMENT_SUBMIT);
        documentService.approve(pageable, approveIds, AUTHOR_APPROVE, COMMENT_APPROVE);
        /** c 20го по 30ый утвердили, исключаем их из списка отправленных на утверждение */
        submitIds = submitIds.subList(0, 10);

        List<Long> mixedIds = new ArrayList<>();
        mixedIds.addAll(draftIds);
        mixedIds.addAll(submitIds);
        mixedIds.addAll(approveIds);
        mixedIds.addAll(notExistIds);

        DocumentsRequestDto request = DocumentsRequestDto.builder()
                .ids(mixedIds)
                .author(AUTHOR_SUBMIT)
                .comment(COMMENT_SUBMIT)
                .build();


        /** Выполняем проверяемый метод */
        List<DocumentSubmitResponseDto> results = documentService.submit(pageable, request);


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


        for (Long id : draftIds) {

            Document doc = documentRepository.findById(id).get();
            assertEquals(Status.SUBMITTED, doc.getStatus(),
                    String.format("Документ исходного статуса Draft с Id: %d должен стать SUBMITTED", id));
            assertEquals(AUTHOR_DRAFT, doc.getAuthor(), "Автор документа не должен измениться");

            assertEquals(1, doc.getHistorySet().size(), "Должна быть одна запись в истории");

            History history = doc.getHistorySet().iterator().next();
            assertEquals(Action.SUBMIT, history.getAction());
            assertEquals(request.getAuthor(), history.getAuthor(), "Авторы должны совпадать");
            assertEquals(request.getComment(), history.getComment(), "Комментарии должны совпадать");
            assertNotNull(history.getDate(), "Дата обновления в Истории не должна быть NULL");
            assertEquals(doc.getUpdateDate() , history.getDate(), "Дата истории и дата обновления документа должны совпадать");

        }


        for (Long id : submitIds) {

            Document doc = documentRepository.findById(id).get();
            assertEquals(Status.SUBMITTED, doc.getStatus(),
                String.format("Документ исходного статуса SUBMITTED с Id: %d должен остаться в том же статусе SUBMITTED", id));
            assertEquals(AUTHOR_DRAFT, doc.getAuthor(), "Автор документа не должен измениться");

            assertEquals(1, doc.getHistorySet().size(), "Должна быть одна запись в истории");

            History history = doc.getHistorySet().iterator().next();
            assertEquals(Action.SUBMIT, history.getAction());
            assertEquals(AUTHOR_SUBMIT, history.getAuthor(), "Авторы должны совпадать");
            assertEquals(COMMENT_SUBMIT, history.getComment(), "Комментарии должны совпадать");
            assertNotNull(history.getDate(), "Дата обновления в Истории не должна быть NULL");
            assertEquals(doc.getUpdateDate() , history.getDate(), "Дата истории и дата обновления документа должны совпадать");

        }


        for (Long id : approveIds) {

            Document doc = documentRepository.findById(id).get();
            assertEquals(Status.APPROVED, doc.getStatus(),
                    String.format("Документ исходного статуса APPROVED с Id: %d должен остаться в том же статусе APPROVED", id));
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

            assertNotEquals(historySubmit.getDate(), historyApprove.getDate(), "Даты не должны совпадать");


//            ZonedDateTime submittedUpdateDate = doc.getUpdateDate();
//            LocalDate submittedLocalDate = submittedUpdateDate.toLocalDate();
//            assertAll(
//                    () -> assertNotNull(submittedUpdateDate),
//                    () -> assertEquals(Year.now().getValue(), submittedUpdateDate.getYear(), "Год обновления должна быть текущим"),
//                    () -> assertEquals(startLocalDate,submittedLocalDate, "Дата обновления должна совпадать")
//            );

        }


        for (int i = 0; i < notExistIds.size(); i++) {

            Long id = notExistIds.get(i);
            DocumentSubmitResponseDto response = results.get(i + 30);

            Optional<Document> optionalDoc = documentRepository.findById(id);
            assertFalse(optionalDoc.isPresent(), String.format("Документ с Id: %d не существует", id));

            assertEquals(OperationStatus.NOT_FOUND, response.getOperationStatus(),
                    String.format("Операция с Документом %d должна завершится NOT_FOUND", submitIds.get(i)));

        }


        log.info("✓ Пакетный submit: результаты - успех: {}, конфликт: {}, не найдено: {}",
                successCount, conflictCount, notFoundCount);
    }


    @Test
    @DisplayName("Пакетный submit - с переданным пустым списком Id должен вернуться пустой результат")
    void batchSubmitEmptyDocumentIdList() {
        log.info("startMethod");

        DocumentsRequestDto request = DocumentsRequestDto.builder()
                .ids(new ArrayList<>())
                .author(AUTHOR_SUBMIT)
                .comment(COMMENT_SUBMIT)
                .build();

        Pageable pageable = PageRequest.of(0, 20, Sort.by("create_date").descending());

        List<DocumentSubmitResponseDto> results = documentService.submit(pageable, request);

        assertNotNull(results);
        assertTrue(results.isEmpty(), "Результат должен быть пустым списком");

        log.info("Пакетный Submit, пустой список обработан корректно");
    }


    @Test
    @DisplayName("Пакетный submit - при отсутствии списка должен вернуться пустой результат")
    void batchSubmitNullDocumentIdList() {
        log.info("startMethod");

        DocumentsRequestDto request = DocumentsRequestDto.builder()
                .author(AUTHOR_SUBMIT)
                .comment(COMMENT_SUBMIT)
                .build();

        Pageable pageable = PageRequest.of(0, 20, Sort.by("create_date").descending());

        List<DocumentSubmitResponseDto> results = documentService.submit(pageable, request);

        assertNotNull(results);
        assertTrue(results.isEmpty(), "Результат должен быть пустым списком");

        log.info("Пакетный Submit, пустой список обработан корректно");
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


    /** Принимает список id в статусе DRAFT */
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


    /** Принимает список id в статусе SUBMITTED */
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