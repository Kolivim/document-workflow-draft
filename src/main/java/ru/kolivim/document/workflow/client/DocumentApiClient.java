package ru.kolivim.document.workflow.client;

import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.web.client.RestTemplateBuilder;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.*;
import org.springframework.stereotype.Component;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestTemplate;
import ru.kolivim.document.workflow.dto.request.DocumentBatchRequest;
import ru.kolivim.document.workflow.dto.request.DocumentsRequestDto;
import ru.kolivim.document.workflow.dto.response.DocumentBatchResponse;
import ru.kolivim.document.workflow.dto.response.DocumentSubmitResponseDto;

import java.time.Duration;
import java.util.List;
import java.util.stream.Collectors;

@Slf4j
@Component
public class DocumentApiClient {


    private final RestTemplate restTemplate;

    @Value("${api.base-url:http://localhost:8080/api/v1/document}")
    private String baseUrl;


    public DocumentApiClient(RestTemplateBuilder restTemplateBuilder) {

//        this.restTemplate = new RestTemplate();

        this.restTemplate =  restTemplateBuilder
                .setConnectTimeout(Duration.ofMillis(5000))   /** 5 секунд на подключение */
                .setReadTimeout(Duration.ofMillis(30000))     /** 30 секунд на чтение ответа */
                .build();

    }


    // TODO : Заменить DocumentBatchRequest на DocumentsRequestDto
    @Deprecated
    public DocumentBatchResponse submitDocumentsOld(DocumentsRequestDto /* DocumentBatchRequest */ request) {
        log.debug("startMethod, request: {}", request);

        try {

            log.info("Отправка {} документов на согласование", request.getIds()/* .getDocumentIds()*/ .size());
            return restTemplate.postForObject(
                    baseUrl + "/submit",
                    request,
                    DocumentBatchResponse.class
            );

        } catch (Exception e) {
            log.error("Ошибка при отправке документов на согласование e: {}", e.getMessage());
            return new DocumentBatchResponse(false, e.getMessage(), null, request.getIds()/* .getDocumentIds()*/);  // TODO: посмотреть подойдёт ли сюда DocumentsResponseDto
        }

    }


    // TODO : Заменить DocumentBatchRequest на DocumentsRequestDto
    @Deprecated
    public DocumentBatchResponse approveDocumentsOld(DocumentsRequestDto /* DocumentBatchRequest */ request) {
        log.debug("startMethod, request: {}", request);

        try {

            log.info("Отправка {} документов на утверждение", request.getIds()/* .getDocumentIds()*/ .size());
            return restTemplate.postForObject(
                    baseUrl + "/approve",
                    request,
                    DocumentBatchResponse.class
            );
        } catch (Exception e) {
            log.error("Ошибка при отправке документов на утверждение e: {}", e.getMessage());
            return new DocumentBatchResponse(false, e.getMessage(), null, request.getIds()/* .getDocumentIds()*/);  // TODO: посмотреть подойдёт ли сюда DocumentsResponseDto
        }

    }

    /**
     * Отправляет документы на согласование через PUT запрос
     * БЕЗ использования HttpEntity
     */
    public DocumentBatchResponse submitDocumentsFail1(DocumentsRequestDto request) {
        log.debug("startMethod submitDocuments, request: {}", request);

        try {
            String url = baseUrl + "/submit";
            log.info("Отправка PUT запроса на {} для {} документов", url, request.getIds().size());

            // Простой вызов exchange без HttpEntity - передаем request напрямую как Object
            ResponseEntity<List<DocumentSubmitResponseDto>> response = restTemplate.exchange(
                    url,
                    HttpMethod.PUT,
                    null,  // Вместо HttpEntity передаем null, но request будет в параметрах?
                    new ParameterizedTypeReference<List<DocumentSubmitResponseDto>>() {},
                    request  // Передаем request как параметр
            );

            log.info("Получен ответ от API: statusCode = {}", response.getStatusCode());

            List<DocumentSubmitResponseDto> responseBody = response.getBody();

            if (responseBody != null) {
                log.info("Документы успешно отправлены на согласование. Обработано: {}", responseBody.size());

                return DocumentBatchResponse.builder()
                        .success(true)
                        .message("Successfully processed " + responseBody.size() + " documents")
                        .processedIds(responseBody.stream()
                                .map(DocumentSubmitResponseDto::getId)
                                .collect(Collectors.toList()))
                        .failedIds(List.of())
                        .build();
            } else {
                log.warn("Получен пустой ответ от API");
                return DocumentBatchResponse.builder()
                        .success(true)
                        .message("No documents processed")
                        .processedIds(List.of())
                        .failedIds(List.of())
                        .build();
            }

        } catch (Exception e) {
            log.error("Ошибка при отправке документов на согласование. URL: {}, Error: {}",
                    baseUrl + "/submit", e.getMessage(), e);

            return DocumentBatchResponse.builder()
                    .success(false)
                    .message("API call failed: " + e.getMessage())
                    .processedIds(null)
                    .failedIds(request.getIds())
                    .build();
        }
    }


    /**
     * Отправляет документы на согласование через PUT запрос
     */
    public DocumentBatchResponse submitDocuments(DocumentsRequestDto request) {
        log.info("submitDocuments, request: {}", request);

        try {
            String url = baseUrl + "/submit";
            log.info("Отправка PUT запроса на {} для {} документов", url, request.getIds().size());

            // 1. Создаем заголовки
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);

            // 2. Создаем HttpEntity с телом запроса и заголовками
            HttpEntity<DocumentsRequestDto> requestEntity = new HttpEntity<>(request, headers);

            // 3. Отправляем запрос
            ResponseEntity<List<DocumentSubmitResponseDto>> response = restTemplate.exchange(
                    url,
                    HttpMethod.PUT,
                    requestEntity,  // ← теперь тело запроса передается правильно
                    new ParameterizedTypeReference<List<DocumentSubmitResponseDto>>() {}
            );

            log.info("Получен ответ от API: statusCode = {}", response.getStatusCode());

            List<DocumentSubmitResponseDto> responseBody = response.getBody();

            if (responseBody != null) {
                log.info("Документы успешно отправлены на согласование. Обработано: {}", responseBody.size());

                return DocumentBatchResponse.builder()
                        .success(true)
                        .message("Successfully processed " + responseBody.size() + " documents")
                        .processedIds(responseBody.stream()
                                .map(DocumentSubmitResponseDto::getId)
                                .collect(Collectors.toList()))
                        .failedIds(List.of())
                        .build();
            } else {
                log.warn("Получен пустой ответ от API");
                return DocumentBatchResponse.builder()
                        .success(true)
                        .message("No documents processed")
                        .processedIds(List.of())
                        .failedIds(List.of())
                        .build();
            }

        } catch (Exception e) {
            log.error("Ошибка при отправке документов на согласование. URL: {}, Error: {}",
                    baseUrl + "/submit", e.getMessage(), e);

            // Логируем детали ошибки
            if (e instanceof HttpClientErrorException) {
                HttpClientErrorException httpException = (HttpClientErrorException) e;
                log.error("HTTP Status: {}, Response body: {}",
                        httpException.getStatusCode(),
                        httpException.getResponseBodyAsString());
            }

            return DocumentBatchResponse.builder()
                    .success(false)
                    .message("API call failed: " + e.getMessage())
                    .processedIds(null)
                    .failedIds(request.getIds())
                    .build();
        }
    }


    /**
     * Отправляет документы на согласование через PUT запрос
     * @param request DTO с идентификаторами документов
     * @return ответ от API с результатами обработки
     */
    public DocumentBatchResponse submitDocumentso(DocumentsRequestDto request) {
        log.info("startMethod submitDocuments, request: {}", request);

        try {

            String url = baseUrl + "/submit";
            log.info("Отправка PUT запроса на {} для {} документов", url, request.getIds().size());

            /** Создаем HTTP entity с запросом */
            HttpEntity<DocumentsRequestDto> requestEntity = new HttpEntity<>(request);

            /** Выполняем PUT запрос */
            // Контроллер возвращает ResponseEntity<List<DocumentSubmitResponseDto>>
            // Используем ParameterizedTypeReference для правильного парсинга
            ResponseEntity<List<DocumentSubmitResponseDto>> response = restTemplate.exchange(
                    url,
                    HttpMethod.PUT,
                    requestEntity,
                    new ParameterizedTypeReference<List<DocumentSubmitResponseDto>>() {}
            );

            log.info("Получен ответ от API: statusCode = {}", response.getStatusCode());

            List<DocumentSubmitResponseDto> responseBody = response.getBody();

            if (responseBody != null) {
                log.info("Документы успешно отправлены на согласование. Обработано: {}", responseBody.size());

                // Преобразуем ответ контроллера в DocumentBatchResponse
                return DocumentBatchResponse.builder()
                        .success(true)
                        .message("Successfully processed " + responseBody.size() + " documents")
                        .processedIds(responseBody.stream()
                                .map(DocumentSubmitResponseDto::getId)
                                .collect(Collectors.toList()))
                        .failedIds(List.of())
                        .build();
            } else {
                log.warn("Получен пустой ответ от API");
                return DocumentBatchResponse.builder()
                        .success(true)
                        .message("No documents processed")
                        .processedIds(List.of())
                        .failedIds(List.of())
                        .build();
            }
//            ResponseEntity<DocumentBatchResponse> response = restTemplate.exchange(
//                    url,
//                    HttpMethod.PUT,
//                    requestEntity,
//                    DocumentBatchResponse.class
//            );
//
//            log.info("Получен ответ от API: statusCode = {}", response.getStatusCode());
//
//            DocumentBatchResponse responseBody = response.getBody();
//            if (responseBody != null) {
//                log.info("Документы успешно отправлены на согласование. Успешно: {}, с ошибками: {}",
//                        responseBody.getProcessedIds() != null ? responseBody.getProcessedIds().size() : 0,
//                        responseBody.getFailedIds() != null ? responseBody.getFailedIds().size() : 0);
//            }
//
//            return responseBody;

        } catch (Exception e) {
            log.error("Ошибка при отправке документов на согласование. URL: {}, Error: {}",
                    baseUrl + "/submit", e.getMessage(), e);

            // В случае ошибки возвращаем response с fail статусом
            return DocumentBatchResponse.builder()
                    .success(false)
                    .message("API call failed: " + e.getMessage())
                    .processedIds(null)
                    .failedIds(request.getIds())
                    .build();
        }

    }


    /**
     * Отправляет документы на утверждение через PUT запрос
     * @param request DTO с идентификаторами документов
     * @return ответ от API с результатами обработки
     */
    public DocumentBatchResponse approveDocuments(DocumentsRequestDto request) {
        log.info("startMethod approveDocuments, request: {}", request);

        try {

            String url = baseUrl + "/approve";
            log.info("Отправка PUT запроса на {} для {} документов", url, request.getIds().size());

            /** Создаем HTTP entity с запросом */
            HttpEntity<DocumentsRequestDto> requestEntity = new HttpEntity<>(request);

            /** Выполняем PUT запрос */
            // Контроллер возвращает ResponseEntity<List<DocumentSubmitResponseDto>>
            ResponseEntity<List<DocumentSubmitResponseDto>> response = restTemplate.exchange(
                    url,
                    HttpMethod.PUT,
                    requestEntity,
                    new ParameterizedTypeReference<List<DocumentSubmitResponseDto>>() {}
            );

            log.info("Получен ответ от API: statusCode = {}", response.getStatusCode());

            List<DocumentSubmitResponseDto> responseBody = response.getBody();

            if (responseBody != null) {
                log.info("Документы успешно отправлены на утверждение. Обработано: {}", responseBody.size());

                // Преобразуем ответ контроллера в DocumentBatchResponse
                return DocumentBatchResponse.builder()
                        .success(true)
                        .message("Successfully processed " + responseBody.size() + " documents")
                        .processedIds(responseBody.stream()
                                .map(DocumentSubmitResponseDto::getId)
                                .collect(Collectors.toList()))
                        .failedIds(List.of())
                        .build();
            } else {
                log.warn("Получен пустой ответ от API");
                return DocumentBatchResponse.builder()
                        .success(true)
                        .message("No documents processed")
                        .processedIds(List.of())
                        .failedIds(List.of())
                        .build();
            }
//            ResponseEntity<DocumentBatchResponse> response = restTemplate.exchange(
//                    url,
//                    HttpMethod.PUT,
//                    requestEntity,
//                    DocumentBatchResponse.class
//            );
//
//            log.info("Получен ответ от API: statusCode={}", response.getStatusCode());
//
//            DocumentBatchResponse responseBody = response.getBody();
//            if (responseBody != null) {
//                log.info("Документы успешно отправлены на утверждение. Успешно: {}, с ошибками: {}",
//                        responseBody.getProcessedIds() != null ? responseBody.getProcessedIds().size() : 0,
//                        responseBody.getFailedIds() != null ? responseBody.getFailedIds().size() : 0);
//            }
//
//            return responseBody;

        } catch (Exception e) {
            log.error("Ошибка при отправке документов на утверждение. URL: {}, Error: {}",
                    baseUrl + "/approve", e.getMessage(), e);

            // В случае ошибки возвращаем response с fail статусом
            return DocumentBatchResponse.builder()
                    .success(false)
                    .message("API call failed: " + e.getMessage())
                    .processedIds(null)
                    .failedIds(request.getIds())
                    .build();
        }

    }

}
