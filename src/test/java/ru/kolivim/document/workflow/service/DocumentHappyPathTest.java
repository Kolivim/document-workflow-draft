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
import ru.kolivim.document.workflow.repository.HistoryRepository;
import ru.kolivim.document.workflow.repository.RegisterRepository;
import ru.kolivim.document.workflow.service.impl.DocumentServiceImpl;

import java.sql.*;
import java.time.*;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;


@Slf4j
@Testcontainers
@SpringBootTest
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
@RequiredArgsConstructor
public class DocumentHappyPathTest {

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

    @Autowired
    private HistoryRepository historyRepository;

    @Autowired
    private RegisterRepository registerRepository;

    private static Long createdDocumentId;

    private static ZonedDateTime createDate;

    private static ZonedDateTime submittedUpdateDate;

    private static ZonedDateTime approvedUpdateDate;

    private static final String INNER_ID_TEST = "12345A/17";

    private static final String AUTHOR_TEST = "happyPath author";

    private static final String DOCUMENT_NAME_TEST = "happyPath doc";

    private static final String COMMENT_TEST = "happyPath comment";


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

            log.info("Database создана роль 'docWf'");

        } catch (Exception e) {
            log.error("Exception при создании роли 'docWf', e: {}", e.getMessage());
        }

    }


    @Test
    @Order(1)
    @DisplayName("Этап 1. Создание документа в статусе DRAFT")
    void createDocumentSuccess() {
        log.info("startMethod");

        ZonedDateTime startCreateDateTime = ZonedDateTime.now(ZoneOffset.UTC);
        LocalDate startcreateLocalDate = startCreateDateTime.toLocalDate();


        /** Arrange */
        DocumentDto documentDto = DocumentDto.builder()
                .name(DOCUMENT_NAME_TEST)
                .author(AUTHOR_TEST)
                .innerId(INNER_ID_TEST)
                .build();


        /** Act */
        DocumentDto responseDocumentDto = documentService.create(documentDto);
        this.createdDocumentId = responseDocumentDto.getId();


        /** Assert */
        assertNotNull(this.createdDocumentId, "Id документа не должен быть null");
        assertEquals(Status.DRAFT, responseDocumentDto.getStatus(), "Статус созданного документа должен быть DRAFT");
        assertEquals(AUTHOR_TEST, responseDocumentDto.getAuthor(), "Автор должен совпадать");

        Optional<Document> savedDocumentOptional = documentRepository.findById(createdDocumentId);
        assertTrue(savedDocumentOptional.isPresent(), "Запись с документом должна быть создана в БД");

        Document savedDocument = savedDocumentOptional.get();

        assertEquals(Status.DRAFT, savedDocument.getStatus(), "Статус должен быть DRAFT");

        assertEquals(documentDto.getInnerId(), savedDocument.getInnerId(), "Внутренний номер должен совпадать");

        ZonedDateTime createDate = savedDocument.getCreateDate();
        LocalDate createLocalDate = createDate.toLocalDate();
        this.createDate = createDate;
        assertAll(
                () -> assertNotNull(createDate),
                () -> assertEquals(startcreateLocalDate, createLocalDate, "Дата создания должна совпадать"),
                () -> assertEquals(Year.now().getValue(), createDate.getYear(), "Год создания должна быть текущим")
        );

        ZonedDateTime updateDate = savedDocument.getUpdateDate();
        assertNull(updateDate, "Дата обновления при создании должна быть null");

        log.info("endMethod, создан документ с Id: {}, статус: {}", createdDocumentId, savedDocument.getStatus());
    }


    @Test
    @Order(2)
    @DisplayName("Этап 2. Отправка созданного документа (DRAFT → SUBMITTED)")
    void submitDocumentSuccess() {
        log.info("startMethod, createdDocumentId: {}", createdDocumentId);

        ZonedDateTime submittedDateTime = ZonedDateTime.now(ZoneOffset.UTC);
        LocalDate startLocalDate = submittedDateTime.toLocalDate();

        DocumentsRequestDto documentsRequestDto = DocumentsRequestDto.builder()
                .ids(new ArrayList<Long>() {{add(createdDocumentId);}})
                .author(AUTHOR_TEST)
                .comment(COMMENT_TEST)
                .build();

        Pageable pageable = PageRequest.of(0, 20, Sort.by("create_date").descending());     /** Sort.Direction.DESC, "createDate" */
        List<DocumentSubmitResponseDto> response = documentService.submit(pageable, documentsRequestDto);

        assertNotNull(response);
        assertTrue(response.size() == 1);

        for (DocumentSubmitResponseDto documentSubmitResponseDto : response) {
            assertEquals(createdDocumentId, documentSubmitResponseDto.getId(),
                    "Id созданного и отправленного на согласование документа должны совпадать, т.к. это один документ");
            assertEquals(OperationStatus.SUCCESS, documentSubmitResponseDto.getOperationStatus(),
                    "Статус операции должен быть SUCCESS");
        }

        Optional<Document> documentOptional = documentRepository.findById(createdDocumentId);
        assertTrue(documentOptional.isPresent(), "Запись с документом должна быть создана в БД");
        Document submittedDocument = documentOptional.get();

        assertEquals(Status.SUBMITTED, submittedDocument.getStatus(), "Статус документа должен быть SUBMITTED");

        ZonedDateTime createDate = submittedDocument.getCreateDate();
        assertEquals(createDate, createDate, "Даты создания должны быть равны, она не изменялась");


        submittedUpdateDate = submittedDocument.getUpdateDate();
        LocalDate submittedLocalDate = submittedUpdateDate.toLocalDate();
        assertAll(
                () -> assertNotNull(submittedUpdateDate),
                () -> assertEquals(Year.now().getValue(), submittedUpdateDate.getYear(), "Год обновления должна быть текущим"),
                () -> assertEquals(startLocalDate,submittedLocalDate, "Дата обновления должна совпадать")
        );


        Set<History> historySet = submittedDocument.getHistorySet();
        assertFalse(historySet == null, "Для отправленного на согласование документа должна существовать хотя бы одна запись в истории");
        assertTrue(historySet.size() == 1, "Запись истории для документа со статусом SUBMITTED должна быть одна");

        for (History history : historySet) {

            assertEquals(Action.SUBMIT, history.getAction(), "Действие с документом должно быть SUBMIT");
            assertEquals(documentsRequestDto.getAuthor(), history.getAuthor(), "Авторы должны совпадать");
            assertEquals(documentsRequestDto.getComment(), history.getComment(), "Комментарии должны совпадать");
            assertNotNull(history.getDate());

        }


        log.info("endMethod, отправлен на утверждение (SUBMITTED) документ с Id: {}, статус: {}",
                submittedDocument.getId(), submittedDocument.getStatus());
    }


    @Test
    @Order(3)
    @DisplayName("Этап 3. Утверждение документа (SUBMITTED → APPROVED)")
    void approveDocumentSuccess() {
        log.info("startMethod, createdDocumentId: {}", createdDocumentId);

        ZonedDateTime approveDateTime = ZonedDateTime.now(ZoneOffset.UTC);
        LocalDate startApprovedLocalDate = approveDateTime.toLocalDate();

        DocumentsRequestDto documentsRequestDto = DocumentsRequestDto.builder()
                .ids(new ArrayList<Long>() {{add(createdDocumentId);}})
                .author(AUTHOR_TEST)
                .comment(COMMENT_TEST)
                .build();

        Pageable pageable = PageRequest.of(0, 20, Sort.by("create_date").descending());

        List<DocumentSubmitResponseDto> response = documentService.approve(pageable, documentsRequestDto);

        assertNotNull(response);
        assertTrue(response.size() == 1);

        for (DocumentSubmitResponseDto documentSubmitResponseDto : response) {
            assertEquals(createdDocumentId, documentSubmitResponseDto.getId(),
                    "Id созданного и утверждённого документа должны совпадать, т.к. это один документ");
            assertEquals(OperationStatus.SUCCESS, documentSubmitResponseDto.getOperationStatus(),
                    "Статус операции должен быть SUCCESS");
        }

        Optional<Document> documentOptional = documentRepository.findById(createdDocumentId);
        assertTrue(documentOptional.isPresent(), "Запись с документом должна быть создана в БД");
        Document approvedDocument = documentOptional.get();

        assertEquals(Status.APPROVED, approvedDocument.getStatus(), "Статус документа должен быть APPROVED");

        ZonedDateTime createDate = approvedDocument.getCreateDate();
        assertEquals(this.createDate, createDate, "Дата создания должны быть одинаковой, она не изменялась");


        approvedUpdateDate = approvedDocument.getUpdateDate();
        LocalDate approvedLocalDate = approvedUpdateDate.toLocalDate();
        assertAll(
                () -> assertNotNull(approvedUpdateDate),
                () -> assertEquals(Year.now().getValue(), submittedUpdateDate.getYear(), "Год обновления должна быть текущим"),
                () -> assertEquals(startApprovedLocalDate,approvedLocalDate, "Дата обновления должна совпадать")
        );

        assertTrue(approvedUpdateDate.isAfter(submittedUpdateDate),
                "updateDate APPROVE должен быть позднее updateDate SUBMIT");

        Set<History> historySet = approvedDocument.getHistorySet();
        assertFalse(historySet == null, "Для утверждённого документа должна существовать хотя бы две записи в истории");
        assertTrue(historySet.size() == 2, "Запись истории для документа будет две, " +
                "со статусом SUBMITTED и со статусом Approve");

        List<History> historyList = new ArrayList<>(historySet);

        History historySubmit = historyList.get(0);
        assertEquals(Action.SUBMIT, historySubmit.getAction(), "Первое действие с документом должно быть SUBMIT");

        History historyApprove = historyList.get(1);
        assertEquals(Action.APPROVE, historyApprove.getAction(), "Действие с документом должно быть APPROVE");
        assertEquals(documentsRequestDto.getAuthor(), historyApprove.getAuthor(), "Авторы должны совпадать");
        assertEquals(documentsRequestDto.getComment(), historyApprove.getComment(), "Комментарии должны совпадать");
        assertNotNull(historyApprove.getDate());

        assertEquals(approvedDocument.getUpdateDate(), historyApprove.getDate());

        Optional<Register> registerOptional = registerRepository.findById(createdDocumentId);
        assertTrue(registerOptional.isPresent(), "Запись в реестре должна существовать, для утверждённого документа");
        Register register = registerOptional.get();
        assertNotNull(register);
        assertEquals(createdDocumentId, register.getId());


        log.info("endMethod, утверждён (APPROVE) документ с Id: {}, статус: {}",
                approvedDocument.getId(), approvedDocument.getStatus());

    }

}