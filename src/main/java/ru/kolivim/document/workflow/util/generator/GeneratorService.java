package ru.kolivim.document.workflow.util.generator;

import jakarta.annotation.PostConstruct;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.web.client.RestTemplateBuilder;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;
import ru.kolivim.document.workflow.dto.DocumentDto;

import java.io.IOException;
import java.time.Duration;
import java.time.ZonedDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

@Slf4j
@Service
//@RequiredArgsConstructor
public class GeneratorService {

    private final RestTemplate restTemplate;

    private final GeneratorProperties properties;

    private final AtomicInteger successCount = new AtomicInteger(0);

    private final AtomicInteger failureCount = new AtomicInteger(0);

    private final List<Long> createdIds = new ArrayList<>();

    private long startTime;

    private long lastLogTime;

    /** Общее количество документов для создания (по умолчанию количество создаваемых пачек документов = 1 и, соответственно, totalDocuments == number) */
    private int totalDocuments;


    public GeneratorService(RestTemplateBuilder builder, GeneratorProperties properties) {

        this.restTemplate = builder
                .setConnectTimeout(Duration.ofSeconds(10))
                .setReadTimeout(Duration.ofSeconds(30))
                .build();

        this.properties = properties;

    }


    @PostConstruct
    public void init() throws IOException {

        /** Загружает параметры по умолчанию из конфигурационного файла */
        properties.loadFromDefaultFile();

        this.totalDocuments = properties.getTotalDocuments();

        log.info("Получено для создания из конфигурационного файла следующее количество документов, : {}", totalDocuments);

    }


    /** Запускает процесс создания документов */
    public void generate() {
        log.info("startMethod, получено для создания следующее количество документов: {}", totalDocuments);

        startTime = System.currentTimeMillis();
        lastLogTime = startTime;

        printLoadedConfigParameters();


        /** Создаём указанное количество пачек */
        for (int batchNum = 1; batchNum <= properties.getCount(); batchNum++) {
            generateBatch(batchNum);
        }


        printSummary();

        log.info("endMethod");
    }


    /** Генерирует одну пачку документов */
    private void generateBatch(int batchNum) {
        log.info("startMethod, пачка {} из {} (для создания {} документов)",
                batchNum, properties.getCount(), properties.getNumber());

        long batchStartTime = System.currentTimeMillis();


        for (int i = 1; i <= properties.getNumber(); i++) {

            int documentIndex = (batchNum - 1) * properties.getNumber() + i;

            createSingleDocument(documentIndex, totalDocuments);

        }


        log.info("endMethod, создание Документов пачки {} из {} завершено за {} мс, создано Документов: {}",
                batchNum, properties.getCount(), System.currentTimeMillis() - batchStartTime, properties.getNumber());
    }


    /** Создает один документ через API */
    private void createSingleDocument(int index, int total) {

        long docStartTime = System.currentTimeMillis();

        log.info("startMethod, подготовка создания документа {} из {}", index, total);


        try {

            DocumentDto document = buildDocumentDto(index);


            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            HttpEntity<DocumentDto> requestEntity = new HttpEntity<>(document, headers);

            ResponseEntity<DocumentDto> response = restTemplate.exchange(
                    properties.getApiUrl(),
                    HttpMethod.POST,
                    requestEntity,
                    DocumentDto.class
            );


            if (response.getBody() != null && response.getBody().getId() != null) {

                createdIds.add(response.getBody().getId());
                successCount.incrementAndGet();

                log.info("Создан Документ {}/{} - с Id: {} за {} мс", index, total, response.getBody().getId(),
                        System.currentTimeMillis() - docStartTime);

                if (index % 10 == 0 || index == total) {

                    long now = System.currentTimeMillis();
                    long elapsed = now - startTime;
                    double avgTimePerDoc = (double) elapsed / index;
                    long remainingTime = (long) (avgTimePerDoc * (total - index));

                    int percent = (index * 100) / total;
                    log.info("Прогресс: для документа {} из {} составляет {}% | Прошло: {} мс | " +
                                    "Прогнозируемое оставшееся время ~ {} мс",
                            index, total, percent, elapsed, remainingTime);

                }

            }

        } catch (RestClientException e) {
            log.error("Ошибка создания документа {} RestClientException: {}", index, e.getMessage());
            failureCount.incrementAndGet();
        }


        log.info("endMethod, отправлен запрос на создание документа {} из {}", index, total);
    }


    public void updateTotalDocuments(int total) {
        this.totalDocuments = total;
        log.info("Обновлено общее количество документов для создания: {}", totalDocuments);
    }


    /** Создает DTO документа */
    private DocumentDto buildDocumentDto(int index) {

        return DocumentDto.builder()
                .innerId(System.currentTimeMillis() + "_" + index)
                .name("Документ №" + index)
                .author(properties.getAuthor())
                .status(properties.getStatus())
                .createDate(ZonedDateTime.now())
                .updateDate(null)
                .build();
    }


    /** Выводит итоговую статистику */
    private void printSummary() {
        log.info("startMethod");


        long duration = System.currentTimeMillis() - startTime;

        log.info("╔══════════════════════════════════════════════════════════════╗");
        log.info("║                     ГЕНЕРАЦИЯ ЗАВЕРШЕНА                      ║");
        log.info("╚══════════════════════════════════════════════════════════════╝");
        log.info("⏱️  Время выполнения: {} мс ({} сек)", duration, duration / 1000);
        log.info("✅ Успешно создано: {}", successCount.get());
        log.info("❌ Ошибок: {}\n", failureCount.get());

        int total = successCount.get() + failureCount.get();
        if (total > 0) {
            double percent = (successCount.get() * 100.0) / total;
            log.info("Процент успеха: {} %", percent);
        }

        log.info("");
        log.info("📋 Созданные ID документов (первые 20):");

        StringBuilder ids = new StringBuilder("   ");
        createdIds.stream().limit(20).forEach(id -> ids.append(id).append(", "));

        if (ids.length() > 3) {
            ids.setLength(ids.length() - 2);
            log.info(ids.toString());
        }

        if (createdIds.size() > 20) {
            log.info("   ... и еще {} документов", createdIds.size() - 20);
        }

        log.info("");
        log.info("Всего создано документов: {}", createdIds.size());
        log.info("");

        if (successCount.get() > 0) {
            double avgTime = (double) duration / successCount.get();
            log.info("Среднее время на документ: {} мс", Math.round(avgTime));
        }

        log.info("endMethod");
    }


    private void printLoadedConfigParameters() {

        log.info("");
        log.info("╔══════════════════════════════════════════════════════════════╗");
        log.info("║         УТИЛИТА МАССОВОГО СОЗДАНИЯ ДОКУМЕНТОВ                ║");
        log.info("╚══════════════════════════════════════════════════════════════╝");
        log.info("\tПолучены следующие параметры генерации Документов:");
        log.info("\t📊 Количество пачек для создания: {} (по умолчанию = 1)", properties.getCount());
        log.info("\t📦 Количество документов для создания, в 1 пачке: {}", properties.getNumber());
        log.info("\t👤 Автор: {}", properties.getAuthor());
        log.info("\t📌 Статус: {}", properties.getStatus());
        log.info("\t🌐 API URL: {}\n", properties.getApiUrl());

    }

}
