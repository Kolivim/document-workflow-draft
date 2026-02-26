package ru.kolivim.document.workflow.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import ru.kolivim.document.workflow.dto.request.ConcurrentApproveRequest;
import ru.kolivim.document.workflow.dto.response.ConcurrentResponseDto;
import ru.kolivim.document.workflow.dto.response.DocumentSubmitResponseDto;
import ru.kolivim.document.workflow.service.DocumentConcurrentService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.concurrent.CompletableFuture;

@Tag(name = "Тестирование конкурентности", description = "API для тестирования конкурентного утверждения документов")
@RestController("DocumentConcurrentController")
@RequestMapping("/api/v1/document/concurrent")
@RequiredArgsConstructor
public class DocumentConcurrentController {

    private final DocumentConcurrentService service;


    @Operation(summary = "Тестирование конкурентного утверждения документа",
            description = "Запускает несколько параллельных попыток утвердить документ(APPROVED)")
    @PutMapping(produces = MediaType.APPLICATION_JSON_VALUE)
    @ResponseBody
    public ResponseEntity<ConcurrentResponseDto> concurrentApprove(@Valid @RequestBody ConcurrentApproveRequest request) {
        return ResponseEntity.ok(service.concurrentApprove(request));
    }


}
