package ru.kolivim.document.workflow.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import ru.kolivim.document.workflow.exception.ResourceNotFoundException;
import ru.kolivim.document.workflow.service.impl.DocumentServiceImpl;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.Statement;
import java.time.ZonedDateTime;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertThrows;

@Slf4j
@Deprecated
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

//    @Autowired
//    CommentService commentService;
//    @Autowired
//    CommentRepository commentRepository;
//    @Autowired
//    EventNotificationRepository eventNotificationRepository;
//    @Autowired
//    SettingsRepository settingsRepository;
//    @Autowired
//    NotificationService notificationService;
//    @Autowired
//    JwtEncoder jwtEncoder;
//    @Autowired
//    TechnicalUserConfig technicalUser;
//    @Autowired
//    JwtEncoder accessTokenEncoder;
//    @Autowired
//    NotificationsMapper notificationsMapper;
//    @Autowired
//    CommentMapperImpl commentMapper;                  //    private static FactoryTest factoryTest;


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
        postgres.start();                                                                                               //        factoryTest = new FactoryTest();
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


    // Разные тесты сервиса:
    // - Поиск по ID
    // - Поиск по параметрам
    // - Обновление документа
    // - Удаление
    // - Негативные сценарии


    @Test
    @Transactional
    @DisplayName("Проверка получения документа по несуществующему Id")
    public void getDocumentById() {
        assertThrows(ResourceNotFoundException.class, () -> documentService.getDocumentById(-1L));
    }


    @Test
    @Transactional
    @DisplayName("Test getDocumentById with negative ID throws ResourceNotFoundException")
    public void createException() {



    }


}
