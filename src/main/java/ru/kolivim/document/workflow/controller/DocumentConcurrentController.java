package ru.kolivim.document.workflow.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.ExampleObject;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.validation.annotation.Validated;
import ru.kolivim.document.workflow.dto.request.ConcurrentApproveRequest;
import ru.kolivim.document.workflow.dto.response.ConcurrentResponseDto;
import ru.kolivim.document.workflow.dto.response.DocumentSubmitResponseDto;
import ru.kolivim.document.workflow.dto.response.ErrorResponse;
import ru.kolivim.document.workflow.service.DocumentConcurrentService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.concurrent.CompletableFuture;

@Tag(name = "Тестирование конкурентности", description = "API для тестирования конкурентного утверждения документов")
@Validated
@RestController
@RequestMapping("/api/v1/document/concurrent")
@RequiredArgsConstructor
public class DocumentConcurrentController {

    private final DocumentConcurrentService service;


    @Operation(summary = "Тестирование конкурентного утверждения документа",
            description = """
                    Запускает несколько параллельных попыток утвердить один документ.
                    Ожидаемое поведение: ровно одна попытка должна перевести документ в APPROVED
                    и создать запись в реестре. Остальные попытки должны завершиться с конфликтом""")
    @ApiResponses({
            @ApiResponse(
                    responseCode = "200",
                    description = "Тестирование успешно выполнено",
                    content = @Content(
                            mediaType = MediaType.APPLICATION_JSON_VALUE,
                            schema = @Schema(implementation = ConcurrentResponseDto.class),
                            examples = @ExampleObject(
                                    value = """
                                            {
                                                "id": 1,
                                                "status": "APPROVED",
                                                "countSuccessfulApprove": 1,
                                                "countFailedApprove": 49,
                                                "isSuccess": true
                                            }
                                            """
                            )
                    )
            ),
            @ApiResponse(
                    responseCode = "400",
                    description = "Ошибка валидации входных данных",
                    content = @Content(
                            mediaType = MediaType.APPLICATION_JSON_VALUE,
                            schema = @Schema(implementation = ErrorResponse.class),
                            examples = @ExampleObject(
                                    value = """
                                            {
                                                "code": "VALIDATION_ERROR",
                                                "message": "Ошибка валидации входных данных",
                                                "status": 400,
                                                "timestamp": "2026-03-01T21:52:28.1731223+03:00",
                                                "path": "/api/v1/document/concurrent",
                                                "errors": {
                                                    "threads": "Количество потоков должно быть не менее 1",
                                                    "documentId": "ID документа обязателен"
                                                }
                                            }
                                            """
                            )
                    )
            ),
            @ApiResponse(
                    responseCode = "404",
                    description = "Документ не найден",
                    content = @Content(
                            mediaType = MediaType.APPLICATION_JSON_VALUE,
                            schema = @Schema(implementation = ErrorResponse.class),
                            examples = @ExampleObject(
                                    value = """
                                            {
                                                "code": "NOT_FOUND",
                                                "message": "Документ не найден для id: 10250",
                                                "status": 404,
                                                "timestamp": "2026-03-01T22:03:44.8463934+03:00",
                                                "path": "/api/v1/document/concurrent"
                                            }
                                            """
                            )
                    )
            ),
            @ApiResponse(
                    responseCode = "409",
                    description = "Недопустимый статус документа (должен быть SUBMITTED)",
                    content = @Content(
                            mediaType = MediaType.APPLICATION_JSON_VALUE,
                            schema = @Schema(implementation = ErrorResponse.class),
                            examples = @ExampleObject(
                                    value = """
                                            {
                                                "code": "INVALID_STATUS_TRANSITION",
                                                "message": "Недопустимый переход статуса для документа 1025: APPROVED -> APPROVED",
                                                "status": 409,
                                                "timestamp": "2026-03-01T22:03:32.0283642+03:00",
                                                "path": "/api/v1/document/concurrent"
                                            }
                                            """
                            )
                    )
            )
    })
    @PutMapping(produces = MediaType.APPLICATION_JSON_VALUE)
    @ResponseBody
    public ResponseEntity<ConcurrentResponseDto> concurrentApprove(@Valid @RequestBody ConcurrentApproveRequest request) {
        return ResponseEntity.ok(service.concurrentApprove(request));
    }


}
