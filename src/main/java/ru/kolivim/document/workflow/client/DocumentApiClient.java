package ru.kolivim.document.workflow.client;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.web.client.RestTemplateBuilder;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.stereotype.Component;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestTemplate;
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

        this.restTemplate =  restTemplateBuilder
                .setConnectTimeout(Duration.ofMillis(10000))                                                            /** 10 секунд на подключение */
                .setReadTimeout(Duration.ofMillis(90000))                                                               /** 90 секунд на чтение ответа */
                .build();

    }


    /**
     * Отправляет документы (SUBMITTED) на согласование
     * @param request DTO с идентификаторами документов для отправки (SUBMITTED)
     * @return DTO с результатами обработки через API
     */
    public DocumentBatchResponse submitDocuments(DocumentsRequestDto request) {
        log.info("startMethod, к отправке (SUBMITTED) получены request: {}", request);

        try {

            String url = baseUrl + "/submit";
            log.info("Отправка запроса (SUBMITTED) на URL: {} для {} документов", url, request.getIds().size());


            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);


            HttpEntity<DocumentsRequestDto> requestEntity = new HttpEntity<>(request, headers);

            ResponseEntity<List<DocumentSubmitResponseDto>> response = restTemplate.exchange(
                    url,
                    HttpMethod.PUT,
                    requestEntity,
                    new ParameterizedTypeReference<List<DocumentSubmitResponseDto>>() {}
            );


            log.info("Получен ответ от /submit API: statusCode = {}", response.getStatusCode());

            List<DocumentSubmitResponseDto> responseBody = response.getBody();

            if (responseBody != null) {

                log.info("Документы успешно отправлены на согласование (SUBMITTED), обработано: {}", responseBody.size());

                return DocumentBatchResponse.builder()
                        .success(true)
                        .message("Успешно отправлено (SUBMITTED) " + responseBody.size() + " документов")
                        .processedIds(responseBody.stream()
                                .map(DocumentSubmitResponseDto::getId)
                                .collect(Collectors.toList()))
                        .failedIds(List.of())
                        .build();

            } else {

                log.info("Получен пустой ответ от API");

                return DocumentBatchResponse.builder()
                        .success(true)
                        .message("Нет обрабатываемых документов")
                        .processedIds(List.of())
                        .failedIds(List.of())
                        .build();

            }

        } catch (Exception e) {

            log.error("Ошибка при отправке документов на согласование. URL: {}, Exception: {}", baseUrl + "/submit",
                    e.getMessage(), e);


            if (e instanceof HttpClientErrorException) {

                HttpClientErrorException httpException = (HttpClientErrorException) e;
                log.error("HTTP Status: {}, Response body: {}", httpException.getStatusCode(),
                        httpException.getResponseBodyAsString());

            }


            return DocumentBatchResponse.builder()
                    .success(false)
                    .message("Ошибка вызова API: ".concat(e.getMessage()))
                    .processedIds(null)
                    .failedIds(request.getIds())
                    .build();

        }

    }


    /**
     * Отправляет документы на утверждение (APPROVE)
     * @param request DTO с идентификаторами документов для утверждения (APPROVE)
     * @return DTO с результатами обработки через API
     */
    public DocumentBatchResponse approveDocuments(DocumentsRequestDto request) {
        log.info("startMethod, к утверждению (APPROVE)  request: {}", request);

        try {

            String url = baseUrl + "/approve";
            log.info("Отправка запроса (APPROVED) на URL: {} для {} документов", url, request.getIds().size());


            HttpEntity<DocumentsRequestDto> requestEntity = new HttpEntity<>(request);

            ResponseEntity<List<DocumentSubmitResponseDto>> response = restTemplate.exchange(
                    url,
                    HttpMethod.PUT,
                    requestEntity,
                    new ParameterizedTypeReference<List<DocumentSubmitResponseDto>>() {}
            );


            log.info("Получен ответ от /approve API : statusCode = {}", response.getStatusCode());

            List<DocumentSubmitResponseDto> responseBody = response.getBody();

            if (responseBody != null) {

                log.info("Документы успешно отправлены на утверждение (Approve) в количестве {}", responseBody.size());

                return DocumentBatchResponse.builder()
                        .success(true)
                        .message("Успешно обработано " + responseBody.size() + " документов")
                        .processedIds(responseBody.stream()
                                .map(DocumentSubmitResponseDto::getId)
                                .collect(Collectors.toList()))
                        .failedIds(List.of())
                        .build();
            } else {

                log.info("Получен пустой ответ от API");
                return DocumentBatchResponse.builder()
                        .success(true)
                        .message("Нет обработанных документов")
                        .processedIds(List.of())
                        .failedIds(List.of())
                        .build();

            }

        } catch (Exception e) {

            log.error("Ошибка при отправке документов на утверждение (APPROVE) на URL: {}, Exception: {}",
                    baseUrl + "/approve", e.getMessage(), e);

            return DocumentBatchResponse.builder()
                    .success(false)
                    .message("Ошибка вызова API: " + e.getMessage())
                    .processedIds(null)
                    .failedIds(request.getIds())
                    .build();

        }

    }


}
