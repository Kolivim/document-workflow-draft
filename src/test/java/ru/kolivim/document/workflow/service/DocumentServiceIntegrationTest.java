package ru.kolivim.document.workflow.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import ru.kolivim.document.workflow.dto.DocumentDto;
import ru.kolivim.document.workflow.dto.request.SearchDocumentDto;
import ru.kolivim.document.workflow.entity.enums.Status;
import ru.kolivim.document.workflow.exception.ResourceNotFoundException;
import ru.kolivim.document.workflow.service.impl.DocumentServiceImpl;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.Statement;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.junit.jupiter.api.Assertions.assertTrue;

@Slf4j
@Testcontainers
@SpringBootTest
@AutoConfigureMockMvc
@RequiredArgsConstructor
public class DocumentServiceIntegrationTest {

    static {System.setProperty("liquibase.duplicateFileMode", "WARN");}

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private DocumentServiceImpl documentService;

    private static final String AUTHOR_DRAFT = "integrationTestDraft author";
    private static final String AUTHOR_SUBMIT = "integrationTestSubmit author";
    private static final String AUTHOR_APPROVE = "integrationTestApprove author";
    private static final String COMMENT_SUBMIT = "integrationTestSubmit comment";
    private static final String COMMENT_APPROVE = "integrationTestApprove comment";
    private static final String DOCUMENT_NAME = "integrationTestNDocumentName";

    private static final String INNER_ID_PREFIX = "integrationTest-";

    @Container
    @ServiceConnection
    static PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:17.6");


    @DynamicPropertySource
    static void configureProperties(DynamicPropertyRegistry dynamicPropertyRegistry) {
        dynamicPropertyRegistry.add("spring.datasource.url", postgres::getJdbcUrl);
        dynamicPropertyRegistry.add("spring.datasource.username", postgres::getUsername);
        dynamicPropertyRegistry.add("spring.datasource.password", postgres::getPassword);
    }


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

            log.info("Database создана роль 'docWf'");

        } catch (Exception e) {
            log.error("Exception при создании роли 'docWf', e: {}", e.getMessage());
        }

    }


    @Test
    @Transactional
    @DisplayName("Проверка получения документа по несуществующему Id")
    public void getDocumentById() {
        assertThrows(ResourceNotFoundException.class, () -> documentService.getDocumentById(-1L));
    }


    @Test
    @Transactional
    @DisplayName("Проверка получения списка документов по статусу")
    public void getDocumentsByStatus() {
        log.info("startMethod");

        List<Long> documentIds = createDocuments(1);
        Long docId = documentIds.get(0);

        Pageable pageable = PageRequest.of(0, 20, Sort.by("createDate").descending());
        documentService.submit(pageable, documentIds, AUTHOR_SUBMIT, COMMENT_SUBMIT);

        SearchDocumentDto searchDocumentDto = SearchDocumentDto.builder()
                .status(Status.SUBMITTED)
                .build();


        Page<DocumentDto> findDocumentPage = documentService.getByFilter(searchDocumentDto, pageable);

        assertNotNull(findDocumentPage);
        assertTrue(findDocumentPage.getTotalElements() == 1, "В списке должен быть 1 документ");

        List<DocumentDto> documentDtoList = findDocumentPage.getContent();
        assertTrue(findDocumentPage.getTotalElements() == 1, "В списке должен быть 1 документ");

        DocumentDto documentDto = documentDtoList.get(0);

        assertEquals(docId, documentDto.getId(), "Id документа должен совпадать");
        assertEquals(AUTHOR_DRAFT, documentDto.getAuthor(), "Автор документа должен совпадать");
        assertEquals(DOCUMENT_NAME, documentDto.getName(), "Имя документа должно совпадать");


        log.info("endMethod");
    }


    private List<Long> createDocuments(int count) {
        log.info("startMethod, count: {}", count);

        List<Long> ids = new ArrayList<>();

        for (int i = 0; i < count; i++) {

            DocumentDto request = DocumentDto.builder()
                    .name(DOCUMENT_NAME)
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

}
