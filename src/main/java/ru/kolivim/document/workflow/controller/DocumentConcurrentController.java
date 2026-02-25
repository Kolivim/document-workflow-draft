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


    @Operation(summary = "Тестирование конкурентного утверждения документа",
            description = "Используя API запускает несколько параллельных попыток утвердить документ(APPROVED)")
    @PutMapping(value = "/http", produces = MediaType.APPLICATION_JSON_VALUE)
    @ResponseBody
    public ResponseEntity<ConcurrentResponseDto> concurrentHttpApprove(@Valid @RequestBody ConcurrentApproveRequest request) {
        return ResponseEntity.ok(service.concurrentHttpApprove(request));
    }


    /*
    @Operation(summary = "Утверждает документ(переводит в статус APPROVED)",
                description = "Утверждает документ(переводит в статус APPROVED)")
    @PutMapping(value = "/approve", produces = MediaType.APPLICATION_JSON_VALUE)
    @ResponseBody
    public ResponseEntity<List<DocumentSubmitResponseDto>> approve(
            @Parameter(description = "Список идентификаторов документов, которые нужно утвердить", required = true)
            @RequestParam List<Long> idList) {
        return ResponseEntity.ok(service.approve(idList));
    }
    */


    /*
    @Operation(summary = "Запускает несколько параллельных попыток утвердить документ(перевести в статус APPROVED)",
                description = "Запускает несколько параллельных попыток утвердить документ(перевести в статус APPROVED)")
    @PutMapping(value = "/parallelApproveOne", produces = MediaType.APPLICATION_JSON_VALUE)
    @ResponseBody
    public CompletableFuture<List<DocumentSubmitResponseDto>> parallelApproveOne(
            @Parameter(description = "Идентификатор документа для утверждения", required = true)
            @RequestParam Long id,
            @Parameter(description = "Количество потоков, обрабатывающих документ", required = true)
            @RequestParam int threads,
            @Parameter(description = "Количество попыток обработать документ", required = true)
            @RequestParam int attempts) throws InterruptedException {
        return service.parallelApproveOne(id, threads, attempts);
    }
    */


    /*
    @Operation(summary = "Запускает несколько параллельных попыток утвердить документ(перевести в статус APPROVED)",
                description = "Запускает несколько параллельных попыток утвердить документ(перевести в статус APPROVED)")
    @PutMapping(value = "/parallelApproveTwo", produces = MediaType.APPLICATION_JSON_VALUE)
    @ResponseBody
    public CompletableFuture<List<DocumentSubmitResponseDto>> parallelApproveTwo(
            @Parameter(description = "Идентификатор документа, который нужно утвердить", required = true)
            @RequestParam Long id) {
        return service.parallelApproveTwo(id);
    }
    */

}
