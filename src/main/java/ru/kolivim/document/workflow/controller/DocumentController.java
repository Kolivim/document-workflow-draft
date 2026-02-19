package ru.kolivim.document.workflow.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import ru.kolivim.document.workflow.dto.DocumentDto;
import ru.kolivim.document.workflow.dto.SubmitDocumentDto;
import ru.kolivim.document.workflow.entity.Document;
import ru.kolivim.document.workflow.entity.enums.Status;
import ru.kolivim.document.workflow.service.DocumentService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.ZonedDateTime;
import java.util.List;
import java.util.Optional;

@RestController("DocumentController")
@RequestMapping("/api/v1/document")
@Tag(name = "Document", description = "Документы")
@RequiredArgsConstructor
public class DocumentController {

    private final DocumentService service;

    @PostMapping(consumes = MediaType.APPLICATION_JSON_VALUE, produces = MediaType.APPLICATION_JSON_VALUE)
    @ResponseBody
    @Operation(summary = "Создает документ.", description = "Создает документ.")
    public ResponseEntity<DocumentDto> createDocument(@RequestBody DocumentDto documentDto) {
        final Document document = service.create();
        return ResponseEntity.ok(service.entityToDto(document));
    }

    @GetMapping(value = "/{id}", produces = MediaType.APPLICATION_JSON_VALUE)
    @ResponseBody
    @Operation(summary = "Поиск документа по его id.", description = "Поиск документа по его id.")
    public ResponseEntity<DocumentDto> findDocumentById(
            @Parameter(description = "Document", required = true) @PathVariable("id") long id
    ) {
        final Document document = service.findById(id);
        return ResponseEntity.ok(service.entityToDto(document));
    }

    @GetMapping(produces = MediaType.APPLICATION_JSON_VALUE)
    @ResponseBody
    @Operation(summary = "Возвращает список документов.",
            description = "Возвращает список документов.",
            parameters = @Parameter(name = "author", description = "Имя автора. Отбираются только документы указанного автора."))
    public ResponseEntity<List<DocumentDto>> findAllDocument(
            Pageable pageable,
            @RequestParam List<Long> idList) {
        Pageable paging = PageRequest.of(pageable.getPageNumber(), pageable.getPageSize());
        Page<Document> pageResult = service.findAll(paging, idList);
        List<Document> batchedData = pageResult.getContent();
        return ResponseEntity.ok(service.entitiesToDtos(batchedData));
    }

    @PutMapping(produces = MediaType.APPLICATION_JSON_VALUE)
    @ResponseBody
    @Operation(summary = "Отправляет документ на согласование(переводит в статус SUBMITTED).", description = "Отправляет документ на согласование(переводит в статус SUBMITTED).")
    public ResponseEntity<List<SubmitDocumentDto>> submit(@RequestParam List<Long> idList) {
        return ResponseEntity.ok(service.submit(idList));
    }

    @GetMapping(value = "/byStatusAuthorDate", produces = MediaType.APPLICATION_JSON_VALUE)
    @ResponseBody
    @Operation(summary = "Поиск настройки аналитики типу и коду устройства.",
            description = "Поиск настройки аналитики типу и коду устройства.")
    public ResponseEntity<List<DocumentDto>> findByStatusAuthorDate(
            @Parameter(description = """
            Статус документа.
            Будут отобраны только документы с указанным статусом""")
            @RequestParam
            Status status,
            @Parameter(description = """
            Автор документа.
            Будут отобраны только документы с указанным автором""")
            @RequestParam(required = false)
            String author,
            @Parameter(description = """
            Дата создания документа.
            Будут отобраны только документы после указанной даты""")
            @RequestParam(required = false)
            ZonedDateTime startDate,
            @Parameter(description = """
            Дата создания документа.
            Будут отобраны только документы до указанной даты""")
            @RequestParam(required = false)
            ZonedDateTime endDate

    ) {
        final List<Document> documents = service.findByStatusAuthorDate(status, Optional.ofNullable(author), Optional.ofNullable(startDate), Optional.ofNullable(endDate));
        return ResponseEntity.ok(service.entitiesToDtos(documents));
    }
}
