package ru.kolivim.document.workflow.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import ru.kolivim.document.workflow.dto.response.DocumentSubmitResponseDto;
import ru.kolivim.document.workflow.service.RegisterService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.concurrent.CompletableFuture;

@Tag(name = "Register", description = "Переводы документов в статус APPROVED")
@RestController("RegisterController")
@RequestMapping("/api/v1/register")
@RequiredArgsConstructor
public class RegisterController {

    private final RegisterService service;


    @Operation(summary = "Утверждает документ(переводит в статус APPROVED)",
                description = "Утверждает документ(переводит в статус APPROVED)")
    @PutMapping(value = "/approve", produces = MediaType.APPLICATION_JSON_VALUE)
    @ResponseBody
    public ResponseEntity<List<DocumentSubmitResponseDto>> approve(
            @Parameter(description = "Список идентификаторов документов, которые нужно утвердить", required = true)
            @RequestParam List<Long> idList) {
        return ResponseEntity.ok(service.approve(idList));
    }


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


    @Operation(summary = "Запускает несколько параллельных попыток утвердить документ(перевести в статус APPROVED)",
                description = "Запускает несколько параллельных попыток утвердить документ(перевести в статус APPROVED)")
    @PutMapping(value = "/parallelApproveTwo", produces = MediaType.APPLICATION_JSON_VALUE)
    @ResponseBody
    public CompletableFuture<List<DocumentSubmitResponseDto>> parallelApproveTwo(
            @Parameter(description = "Идентификатор документа, который нужно утвердить", required = true)
            @RequestParam Long id) {
        return service.parallelApproveTwo(id);
    }

}
