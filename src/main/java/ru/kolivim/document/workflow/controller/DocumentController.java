package ru.kolivim.document.workflow.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.data.domain.*;
import org.springframework.data.web.PageableDefault;
import org.springframework.format.annotation.DateTimeFormat;
import ru.kolivim.document.workflow.dto.*;
import ru.kolivim.document.workflow.dto.request.DocumentsRequestDto;
import ru.kolivim.document.workflow.dto.request.SearchDocumentDto;
import ru.kolivim.document.workflow.dto.response.DocumentPage;
import ru.kolivim.document.workflow.dto.response.PageResponseDto;
import ru.kolivim.document.workflow.dto.response.DocumentSubmitResponseDto;
import ru.kolivim.document.workflow.entity.Document;
import ru.kolivim.document.workflow.entity.enums.Status;
import ru.kolivim.document.workflow.service.DocumentService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.ZonedDateTime;
import java.util.List;
import java.util.Optional;

@Tag(name = "Api сервиса Документов",
        description = "Сервис для создания, поиска, получения и согласования по запросу Документов")
@RestController("DocumentController")
@RequestMapping("/api/v1/document")
@RequiredArgsConstructor
public class DocumentController {

    private final DocumentService service;

    private final int pageSize = 200;


    @Operation(summary = "Поиск",
            description = "Получение документов, c фильтрованием согласно переданным полям фильтра")
    @PostMapping(value = "/filter", produces = MediaType.APPLICATION_JSON_VALUE)
    @ResponseBody
    public ResponseEntity<Page<DocumentDto>> getByFilter(@RequestBody SearchDocumentDto searchDocumentDto,
                                                         @PageableDefault(size = pageSize,
                                                                 sort = "createDate",
                                                                 direction = Sort.Direction.DESC) Pageable page
    ) {
        return ResponseEntity.ok(service.getByFilter(searchDocumentDto, page));
    }


    @Operation(summary = "Расширенный поиск",
            description = """
                    Получение документов, c фильтрованием согласно переданным полям фильтра, по расширенному списку 
                    обрабатываемых фильтром полей
                    """)
    @PostMapping(value = "/advancedFilter", produces = MediaType.APPLICATION_JSON_VALUE)
    @ResponseBody
    public ResponseEntity<Page<DocumentDto>> getByAdvancedFilter(@RequestBody SearchDocumentDto searchDocumentDto,
                                                                 @PageableDefault(size = pageSize,
                                                                 sort = "createDate",
                                                                 direction = Sort.Direction.DESC) Pageable page
    ) {
        return ResponseEntity.ok(service.getByAdvancedFilter(searchDocumentDto, page));
    }


    @Operation(summary = "Создание документа", description = "Создает документ согласно полученным параметрам")
    @PostMapping(consumes = MediaType.APPLICATION_JSON_VALUE, produces = MediaType.APPLICATION_JSON_VALUE)
    @ResponseBody
    public ResponseEntity<DocumentDto> create(@RequestBody  @Valid DocumentDto documentDto) {
        return ResponseEntity.ok(service.create(documentDto));
    }


    @Operation(summary = "Поиск документа", description = "Поиск документа по его id")
    @GetMapping(value = "/{id}", produces = MediaType.APPLICATION_JSON_VALUE)
    @ResponseBody
    public ResponseEntity<DocumentDto> getDocumentById(
            @Parameter(description = "Document", required = true) @PathVariable("id") long id) {
        return ResponseEntity.ok(service.getById(id));
    }


    @Operation(summary = "Поиск документов",
            description = "Возвращает список документов, по переданному в запросе списку id документов",
            parameters = @Parameter(name = "author", description = "Отбираются только документы с указанными id"))
    @Deprecated
    @PostMapping(value = "/documents", consumes = MediaType.APPLICATION_JSON_VALUE, produces = MediaType.APPLICATION_JSON_VALUE)
    @ResponseBody
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Документы успешно получены")
//            @ApiResponse(responseCode = "400", description = "Некорректный запрос"),
//            @ApiResponse(responseCode = "404", description = "Документы не найдены")
//            @ApiResponse(responseCode = "500", description = "Внутренняя ошибка сервера")
    })
    public ResponseEntity<Page<DocumentDto>> getDocumentByIdList(
            @PageableDefault(size = pageSize, sort = "createDate", direction = Sort.Direction.DESC) Pageable pageable,
            @RequestBody DocumentsRequestDto documentsRequestDto) {

        Page<DocumentDto> pageResult = service.getByIdList(pageable, documentsRequestDto.getIds());

        return ResponseEntity.ok(pageResult);
    }


    @Operation(summary = "Поиск документов",
            description = "Возвращает список документов, по переданному в запросе списку id документов",
            parameters = @Parameter(name = "author", description = "Отбираются только документы с указанными id"))
    @Deprecated
    @PostMapping(value = "/documents/message", consumes = MediaType.APPLICATION_JSON_VALUE, produces = MediaType.APPLICATION_JSON_VALUE)
    @ResponseBody
    @ApiResponses(value = {@ApiResponse(responseCode = "200", description = "Документы успешно получены")})
    public ResponseEntity<ru.kolivim.document.workflow.dto.response.ApiResponse<Page<DocumentDto>>> getDocumentByIdListWithMessage(
            @PageableDefault(size = pageSize, sort = "createDate", direction = Sort.Direction.DESC) Pageable pageable,
            @RequestBody DocumentsRequestDto documentsRequestDto) {

        PageResponseDto pageResponseDto = service.getByIdListWithNoFound(pageable, documentsRequestDto.getIds());

        ru.kolivim.document.workflow.dto.response.ApiResponse response =
                ru.kolivim.document.workflow.dto.response.ApiResponse.success(pageResponseDto.getPage(),
                        "Не найдено документов: ".concat(String.valueOf(pageResponseDto.getNotFoundCount())));

        return ResponseEntity.ok(response);
    }


    @Operation(summary = "Поиск документов",
            description = "Возвращает список документов, по переданному в запросе списку id документов",
            parameters = @Parameter(name = "author", description = "Отбираются только документы с указанными id"))
    @Deprecated
    @PostMapping(value = "/documents/noFoundIds", consumes = MediaType.APPLICATION_JSON_VALUE,
            produces = MediaType.APPLICATION_JSON_VALUE)
    @ResponseBody
    @ApiResponses(value = {@ApiResponse(responseCode = "200", description = "Документы успешно получены")})
    public ResponseEntity<PageResponseDto> getDocumentByIdListWithNoFoundIds(
            @PageableDefault(size = pageSize, sort = "createDate", direction = Sort.Direction.DESC) Pageable pageable,
            @RequestBody DocumentsRequestDto documentsRequestDto) {

        PageResponseDto pageResponseDto = service.getByIdListWithNoFound(pageable, documentsRequestDto.getIds());

        return ResponseEntity.ok(pageResponseDto);
    }


    @Operation(summary = "Поиск документов",
            description = "Возвращает список документов, по переданному в запросе списку id документов",
            parameters = @Parameter(name = "author", description = "Отбираются только документы с указанными id"))
    @Deprecated
    @PostMapping(value = "/documents/extendedPage", consumes = MediaType.APPLICATION_JSON_VALUE, produces = MediaType.APPLICATION_JSON_VALUE)
    @ResponseBody
    @ApiResponses(value = {@ApiResponse(responseCode = "200", description = "Документы успешно получены")})
    public ResponseEntity<Page<DocumentDto>> getDocumentByIdListWithExtendedPage(
            @PageableDefault(size = pageSize, sort = "createDate", direction = Sort.Direction.DESC) Pageable pageable,
            @RequestBody DocumentsRequestDto documentsRequestDto) {

        DocumentPage pageResult = service.getByIdListWithExtendedPage(pageable, documentsRequestDto.getIds());

        return ResponseEntity.ok()
                .header("X-Total-Requested", String.valueOf(pageResult.getTotalCount()))
                .header("X-Not-Found-Count", String.valueOf(pageResult.getNotFoundCount()))
                .body(pageResult);
    }


    @Operation(summary = "Поиск документов",
            description = "Возвращает список документов, по переданному в запросе списку id документов",
            parameters = @Parameter(name = "author", description = "Отбираются только документы с указанными id"))
    @Deprecated
    @GetMapping(produces = MediaType.APPLICATION_JSON_VALUE)
    @ResponseBody
    public ResponseEntity<Page<DocumentDto>> getDocumentByIdListOld(Pageable pageable, @RequestParam List<Long> idList) {
        return ResponseEntity.ok(service.getByIdList(pageable, idList));
    }


    @Operation(summary = "Поиск", description = "Получение документов, c фильтрованием согласно переданным полям фильтра")
    @Deprecated
    @PostMapping(value = "/filterWithParameters", produces = MediaType.APPLICATION_JSON_VALUE)
    @ResponseBody
    public ResponseEntity<Page<DocumentDto>> getByFilterWithParameters(
            @RequestBody DocumentDto documentDto,
            @PageableDefault(size = pageSize, sort = "createDate", direction = Sort.Direction.DESC) Pageable page ,

            /** Формат: ISO 8601 с часовым поясом, например: 2024-01-15T10:30:00+03:00 */
            @Parameter(description = "Дата создания документа. Будут отобраны только документы, созданные после указанной даты")
            @RequestParam(required = false)
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME)
            ZonedDateTime startDate,
            @Parameter(description = "Дата создания документа. Будут отобраны только документы, созданные до указанной даты")
            @RequestParam(required = false)
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME)
            ZonedDateTime endDate
    ) {
        SearchDocumentDto searchDocumentDto = new SearchDocumentDto(documentDto, startDate, endDate);
        return ResponseEntity.ok(service.getByFilter(searchDocumentDto, page));
    }


    @Operation(summary = "Отправляет список документов на согласование",
            description = "При согласовании документ изменяет статус на SUBMITTED")
    @PutMapping(value = "/submit", produces = MediaType.APPLICATION_JSON_VALUE)
    @ResponseBody
    public ResponseEntity<List<DocumentSubmitResponseDto>> submit(
            @PageableDefault(size = pageSize, sort = "createDate", direction = Sort.Direction.DESC) Pageable pageable,
            @RequestBody DocumentsRequestDto documentsRequestDto
    ) {
        return ResponseEntity.ok(service.submit(pageable, documentsRequestDto));
    }


    @Operation(summary = "Отправляет список документов на согласование",
            description = "При согласовании документ изменяет статус на SUBMITTED")
    @PutMapping(value = "/approve", produces = MediaType.APPLICATION_JSON_VALUE)
    @ResponseBody
    public ResponseEntity<List<DocumentSubmitResponseDto>> approve(
            @PageableDefault(size = pageSize, sort = "createDate", direction = Sort.Direction.DESC) Pageable pageable,
            @RequestBody DocumentsRequestDto documentsRequestDto
    ) {
        return ResponseEntity.ok(service.approve(pageable, documentsRequestDto));
    }

}
