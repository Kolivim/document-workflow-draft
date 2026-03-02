package ru.kolivim.document.workflow.controller.api;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.headers.Header;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.ExampleObject;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;
import ru.kolivim.document.workflow.dto.ApiResponsePageDocumentDto;
import ru.kolivim.document.workflow.dto.DocumentDto;
import ru.kolivim.document.workflow.dto.request.DocumentsRequestDto;
import ru.kolivim.document.workflow.dto.request.SearchDocumentDto;
import ru.kolivim.document.workflow.dto.response.DocumentSubmitResponseDto;
import ru.kolivim.document.workflow.dto.response.ErrorResponse;
import ru.kolivim.document.workflow.dto.response.PageResponseDto;

import java.time.ZonedDateTime;
import java.util.List;

@Tag(name = "Api сервиса Документов",
        description = "Сервис для создания, поиска, получения и согласования по запросу Документов")
@RequestMapping("/api/v1/document")
public interface DocumentApi {

    int PAGE_SIZE = 30;


    @Operation(summary = "Поиск документов с фильтрацией",
            description = "Выполняет поиск документов по заданным критериям фильтрации")
    @ApiResponses({
            @ApiResponse(
                    responseCode = "200",
                    description = "Документы успешно найдены",
                    content = @Content(
                            mediaType = MediaType.APPLICATION_JSON_VALUE,
                            schema = @Schema(
                                    implementation = Page.class,
                                    description = "Страница с документами, удовлетворяющими критериям поиска"
                            ),
                            examples = {
                                    @ExampleObject(
                                            name = "Документы найдены",
                                            summary = "Найдены документы по заданным критериям",
                                            value = """
                                                    {
                                                        "content": [
                                                            {
                                                                "id": 1,
                                                                "innerId": "DOC-001",
                                                                "author": "Иванов И.И.",
                                                                "name": "Договор поставки",
                                                                "status": "APPROVED",
                                                                "createDate": "2026-03-01T10:00:00+03:00",
                                                                "updateDate": "2026-03-02T15:30:00+03:00",
                                                                "historySet": [],
                                                                "register": null
                                                            },
                                                            {
                                                                "id": 3,
                                                                "innerId": "DOC-003",
                                                                "author": "Иванов И.И.",
                                                                "name": "Дополнительное соглашение",
                                                                "status": "SUBMITTED",
                                                                "createDate": "2026-03-01T14:00:00+03:00",
                                                                "updateDate": "2026-03-02T16:00:00+03:00",
                                                                "historySet": [],
                                                                "register": null
                                                            }
                                                        ],
                                                        "pageable": {
                                                            "pageNumber": 0,
                                                            "pageSize": 200,
                                                            "sort": {
                                                                "sorted": true,
                                                                "unsorted": false,
                                                                "empty": false
                                                            },
                                                            "offset": 0,
                                                            "paged": true,
                                                            "unpaged": false
                                                        },
                                                        "totalPages": 1,
                                                        "totalElements": 2,
                                                        "last": true,
                                                        "size": 200,
                                                        "number": 0,
                                                        "sort": {
                                                            "sorted": true,
                                                            "unsorted": false,
                                                            "empty": false
                                                        },
                                                        "numberOfElements": 2,
                                                        "first": true,
                                                        "empty": false
                                                    }
                                                    """
                                    ),
                                    @ExampleObject(
                                            name = "Ничего не найдено",
                                            summary = "Документы по заданным критериям отсутствуют",
                                            value = """
                                                    {
                                                        "content": [],
                                                        "pageable": {
                                                            "pageNumber": 0,
                                                            "pageSize": 200,
                                                            "sort": {
                                                                "sorted": true,
                                                                "unsorted": false,
                                                                "empty": false
                                                            },
                                                            "offset": 0,
                                                            "paged": true,
                                                            "unpaged": false
                                                        },
                                                        "totalPages": 0,
                                                        "totalElements": 0,
                                                        "last": true,
                                                        "size": 200,
                                                        "number": 0,
                                                        "sort": {
                                                            "sorted": true,
                                                            "unsorted": false,
                                                            "empty": false
                                                        },
                                                        "numberOfElements": 0,
                                                        "first": true,
                                                        "empty": true
                                                    }
                                                    """
                                    )
                            }
                    )
            ),
            @ApiResponse(
                    responseCode = "400",
                    description = "Ошибка валидации параметров запроса",
                    content = @Content(
                            mediaType = MediaType.APPLICATION_JSON_VALUE,
                            schema = @Schema(implementation = ErrorResponse.class),
                            examples = {
                                    @ExampleObject(
                                            name = "Неверный статус",
                                            description = "Указано недопустимое значение статуса",
                                            value = """
                                                    {
                                                          "code": "INVALID_REQUEST_FORMAT",
                                                          "message": "Недопустимое значение статуса. Допустимые значения: DRAFT, SUBMITTED, APPROVED",
                                                          "status": 400,
                                                          "timestamp": "2026-03-02T02:18:27.8019124+03:00",
                                                          "path": "/api/v1/document/filter"
                                                    }
                                                    """
                                    ),
                                    @ExampleObject(
                                            name = "Неверный формат даты",
                                            description = "Дата указна с опечатками / не полностью",
                                            value = """
                                                    {
                                                          "code": "INVALID_REQUEST_FORMAT",
                                                          "message": "Неверный формат данных в запросе",
                                                          "status": 400,
                                                          "timestamp": "2026-03-02T02:19:53.9796907+03:00",
                                                          "path": "/api/v1/document/filter"
                                                    }
                                                    """
                                    )
                            }
                    )
            ),
            @ApiResponse(
                    responseCode = "500",
                    description = "Внутренняя ошибка сервера",
                    content = @Content(
                            mediaType = MediaType.APPLICATION_JSON_VALUE,
                            schema = @Schema(implementation = ErrorResponse.class),
                            examples = @ExampleObject(
                                    value = """
                                            {
                                                "code": "INTERNAL_SERVER_ERROR",
                                                "message": "Внутренняя ошибка сервера",
                                                "status": 500,
                                                "timestamp": "2026-03-02T10:00:00+03:00",
                                                "path": "/api/v1/document/filter"
                                            }
                                            """
                            )
                    )
            )
    })
    @PostMapping(value = "/filter", produces = MediaType.APPLICATION_JSON_VALUE)
    ResponseEntity<Page<DocumentDto>> getByFilter(
            @Validated(DocumentDto.Search.class) @RequestBody SearchDocumentDto searchDocumentDto,
            @PageableDefault(size = PAGE_SIZE, sort = "createDate", direction = Sort.Direction.DESC) Pageable page);


    @Operation(summary = "Расширенный поиск",
            description = """
                    Получение документов, c фильтрованием согласно переданным полям фильтра, по расширенному списку 
                    обрабатываемых фильтром полей
                    """)
    @ApiResponses({
            @ApiResponse(
                    responseCode = "200",
                    description = "Документы успешно найдены",
                    content = @Content(
                            mediaType = MediaType.APPLICATION_JSON_VALUE,
                            schema = @Schema(
                                    implementation = Page.class,
                                    description = "Страница с документами, удовлетворяющими критериям поиска")
                    )),
            @ApiResponse(
                    responseCode = "400",
                    description = "Ошибка валидации параметров запроса",
                    content = @Content(
                            mediaType = MediaType.APPLICATION_JSON_VALUE,
                            schema = @Schema(implementation = ErrorResponse.class)
                    )),
            @ApiResponse(
                    responseCode = "500",
                    description = "Внутренняя ошибка сервера",
                    content = @Content(
                            mediaType = MediaType.APPLICATION_JSON_VALUE,
                            schema = @Schema(implementation = ErrorResponse.class)
                    ))
    })
    @PostMapping(value = "/advancedFilter", produces = MediaType.APPLICATION_JSON_VALUE)
    ResponseEntity<Page<DocumentDto>> getByAdvancedFilter(
            @Validated(DocumentDto.Search.class) @RequestBody SearchDocumentDto searchDocumentDto,
            @PageableDefault(size = PAGE_SIZE, sort = "createDate", direction = Sort.Direction.DESC) Pageable page);


    @Operation(summary = "Создание документа", description = "Создает новый документ согласно полученным параметрам")
    @ApiResponses({
            @ApiResponse(
                    responseCode = "200",
                    description = "Документ успешно создан",
                    content = @Content(
                            mediaType = MediaType.APPLICATION_JSON_VALUE,
                            schema = @Schema(implementation = DocumentDto.class),
                            examples = @ExampleObject(
                                    name = "Созданный документ",
                                    summary = "Пример ответа с созданным документом",
                                    value = """
                                            {
                                                  "id": 1231,
                                                  "innerId": "DOC 2026-01-01 Спец",
                                                  "author": "I'm author 02_03_26 1",
                                                  "name": "Спецификация",
                                                  "status": "DRAFT",
                                                  "createDate": "2026-03-02T02:33:32.2431894+03:00",
                                                  "updateDate": null,
                                                  "historySet": null,
                                                  "register": null
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
                            examples = {
                                    @ExampleObject(
                                            name = "Пустой innerId",
                                            description = "Внутренний ID не может быть пустым",
                                            value = """
                                                    {
                                                        "code": "VALIDATION_ERROR",
                                                        "message": "Ошибка валидации входных данных",
                                                        "status": 400,
                                                        "timestamp": "2026-03-02T15:30:45.123+03:00",
                                                        "path": "/api/v1/document",
                                                        "errors": {
                                                            "innerId": "Внутренний ID не может быть пустым"
                                                        }
                                                    }
                                                    """
                                    ),
                                    @ExampleObject(
                                            name = "Пустой автор",
                                            description = "Автор не может быть пустым",
                                            value = """
                                                    {
                                                        "code": "VALIDATION_ERROR",
                                                        "message": "Ошибка валидации входных данных",
                                                        "status": 400,
                                                        "timestamp": "2026-03-02T15:30:45.123+03:00",
                                                        "path": "/api/v1/document",
                                                        "errors": {
                                                            "author": "Автор не может быть пустым"
                                                        }
                                                    }
                                                    """
                                    ),
                                    @ExampleObject(
                                            name = "Пустое название",
                                            description = "Название документа не может быть пустым",
                                            value = """
                                                    {
                                                         "code": "VALIDATION_ERROR",
                                                         "message": "Ошибка валидации входных данных",
                                                         "status": 400,
                                                         "timestamp": "2026-03-02T02:32:18.8023943+03:00",
                                                         "path": "/api/v1/document",
                                                         "errors": {
                                                           "name": "не должно быть пустым"
                                                         }
                                                    }
                                                    """
                                    )
                            }
                    )
            ),
            @ApiResponse(
                    responseCode = "409",
                    description = "Конфликт - документ с таким innerId уже существует",
                    content = @Content(
                            mediaType = MediaType.APPLICATION_JSON_VALUE,
                            schema = @Schema(implementation = ErrorResponse.class),
                            examples = @ExampleObject(
                                    name = "Дубликат innerId",
                                    description = "Попытка создать документ с существующим внутренним ID",
                                    value = """
                                            {
                                                 "code": "DUPLICATE_INNER_ID",
                                                 "message": "Документ с внутренним ID '11142-f1c6-44ae-a05a-afa4o9014115' уже существует",
                                                 "status": 409,
                                                 "timestamp": "2026-03-02T02:31:07.544905+03:00",
                                                 "path": "/api/v1/document"
                                            }
                                            """
                            )
                    )
            ),
            @ApiResponse(
                    responseCode = "500",
                    description = "Внутренняя ошибка сервера",
                    content = @Content(
                            mediaType = MediaType.APPLICATION_JSON_VALUE,
                            schema = @Schema(implementation = ErrorResponse.class),
                            examples = @ExampleObject(
                                    value = """
                                            {
                                                "code": "INTERNAL_SERVER_ERROR",
                                                "message": "Внутренняя ошибка сервера",
                                                "status": 500,
                                                "timestamp": "2026-03-02T15:30:45.123+03:00",
                                                "path": "/api/v1/document"
                                            }
                                            """
                            )
                    )
            )
    })
    @PostMapping(consumes = MediaType.APPLICATION_JSON_VALUE, produces = MediaType.APPLICATION_JSON_VALUE)
    ResponseEntity<DocumentDto> create(@Validated(DocumentDto.Create.class) @RequestBody DocumentDto documentDto);


    @Operation(summary = "Поиск документа по id",
            description = "Возвращает полную информацию о документе по его уникальному идентификатору")
    @ApiResponses({
            @ApiResponse(
                    responseCode = "200",
                    description = "Документ найден и успешно возвращен",
                    content = @Content(
                            mediaType = MediaType.APPLICATION_JSON_VALUE,
                            schema = @Schema(implementation = DocumentDto.class),
                            examples = {
                                    @ExampleObject(
                                            name = "Документ в статусе DRAFT",
                                            summary = "Черновик документа (без истории, без реестра)",
                                            value = """
                                                    {
                                                        "id": 1,
                                                        "innerId": "DOC-2026-001",
                                                        "author": "Иванов И.И.",
                                                        "name": "Договор поставки №123",
                                                        "status": "DRAFT",
                                                        "createDate": "2026-03-01T10:00:00+03:00",
                                                        "updateDate": "2026-03-01T10:00:00+03:00",
                                                        "historySet": [],
                                                        "register": null
                                                    }
                                                    """
                                    ),
                                    @ExampleObject(
                                            name = "Документ в статусе SUBMITTED",
                                            summary = "Документ отправлен на согласование (есть история отправки)",
                                            value = """
                                                    {
                                                        "id": 2,
                                                        "innerId": "DOC-2026-002",
                                                        "author": "Петров П.П.",
                                                        "name": "Счет-фактура №456",
                                                        "status": "SUBMITTED",
                                                        "createDate": "2026-03-01T11:00:00+03:00",
                                                        "updateDate": "2026-03-02T09:30:00+03:00",
                                                        "historySet": [
                                                            {
                                                                "id": 101,
                                                                "author": "Петров П.П.",
                                                                "time": "2026-03-02T09:30:00+03:00",
                                                                "action": "SUBMIT",
                                                                "comment": "Отправка на согласование"
                                                            }
                                                        ],
                                                        "register": null
                                                    }
                                                    """
                                    ),
                                    @ExampleObject(
                                            name = "Утвержденный документ",
                                            summary = "Документ утвержден (полная история + запись в реестре)",
                                            value = """
                                                    {
                                                        "id": 3,
                                                        "innerId": "DOC-2026-003",
                                                        "author": "Сидоров С.С.",
                                                        "name": "Акт выполненных работ №789",
                                                        "status": "APPROVED",
                                                        "createDate": "2026-03-01T12:00:00+03:00",
                                                        "updateDate": "2026-03-02T14:00:00+03:00",
                                                        "historySet": [
                                                            {
                                                                "id": 102,
                                                                "author": "Сидоров С.С.",
                                                                "time": "2026-03-02T13:00:00+03:00",
                                                                "action": "SUBMIT",
                                                                "comment": "Отправка на согласование"
                                                            },
                                                            {
                                                                "id": 103,
                                                                "author": "Иванов И.И.",
                                                                "time": "2026-03-02T14:00:00+03:00",
                                                                "action": "APPROVE",
                                                                "comment": "Утверждено"
                                                            }
                                                        ],
                                                        "register": {
                                                            "id": 3,
                                                            "status": "APPROVED"
                                                        }
                                                    }
                                                    """
                                    )
                            }
                    )
            ),
            @ApiResponse(
                    responseCode = "400",
                    description = "Неверный формат идентификатора",
                    content = @Content(
                            mediaType = MediaType.APPLICATION_JSON_VALUE,
                            schema = @Schema(implementation = ErrorResponse.class),
                            examples = {
                                    @ExampleObject(
                                            name = "Нечисловой ID",
                                            summary = "Ошибка преобразования типа",
                                            description = "ID должен быть целым числом (long)",
                                            value = """
                                                    {
                                                         "code": "TYPE_MISMATCH",
                                                         "message": "Неверный формат параметра 'id'. Ожидается тип: long",
                                                         "status": 400,
                                                         "timestamp": "2026-03-02T02:42:23.4174107+03:00",
                                                         "path": "/api/v1/document/g"
                                                    }
                                                    """
                                    )
                            }
                    )
            ),
            @ApiResponse(
                    responseCode = "404",
                    description = "Документ с указанным ID не найден",
                    content = @Content(
                            mediaType = MediaType.APPLICATION_JSON_VALUE,
                            schema = @Schema(implementation = ErrorResponse.class),
                            examples = @ExampleObject(
                                    name = "Документ отсутствует",
                                    description = "В БД нет документа с таким идентификатором",
                                    value = """
                                            {
                                                  "code": "NOT_FOUND",
                                                  "message": "Документ не найден для id: 7530",
                                                  "status": 404,
                                                  "timestamp": "2026-03-02T02:41:18.8710393+03:00",
                                                  "path": "/api/v1/document/7530"
                                            }
                                            """
                            )
                    )),
            @ApiResponse(
                    responseCode = "500",
                    description = "Внутренняя ошибка сервера",
                    content = @Content(
                            mediaType = MediaType.APPLICATION_JSON_VALUE,
                            schema = @Schema(implementation = ErrorResponse.class)
                    ))
    })
    @GetMapping(value = "/{id}", produces = MediaType.APPLICATION_JSON_VALUE)
    ResponseEntity<DocumentDto> getDocumentById(
            @Parameter(description = "Id документа", example = "1", required = true)
            @PathVariable("id") long id);


    @Operation(summary = "Поиск документов по списку Id",
            description = "Возвращает страницу с документами по переданному списку id документов")
    @PostMapping(value = "/documents", consumes = MediaType.APPLICATION_JSON_VALUE, produces = MediaType.APPLICATION_JSON_VALUE)
    @ApiResponses({
            @ApiResponse(
                    responseCode = "200",
                    description = "Документы успешно получены (даже если некоторые не найдены, может быть пустая страница)",
                    content = @Content(
                            mediaType = MediaType.APPLICATION_JSON_VALUE,
                            schema = @Schema(implementation = Page.class),
                            examples = {
                                    @ExampleObject(
                                            name = "Документы найдены",
                                            summary = "Запрошенные документы существуют",
                                            value = """
                                                    {
                                                        "content": [
                                                            {
                                                                "id": 1,
                                                                "innerId": "DOC-001",
                                                                "author": "Иванов И.И.",
                                                                "name": "Документ 1",
                                                                "status": "DRAFT",
                                                                "createDate": "2026-03-01T10:00:00+03:00",
                                                                "updateDate": "2026-03-01T10:00:00+03:00"
                                                            },
                                                            {
                                                                "id": 2,
                                                                "innerId": "DOC-002",
                                                                "author": "Петров П.П.",
                                                                "name": "Документ 2",
                                                                "status": "APPROVED",
                                                                "createDate": "2026-03-01T11:00:00+03:00",
                                                                "updateDate": "2026-03-02T09:30:00+03:00"
                                                            }
                                                        ],
                                                        "pageable": {
                                                            "pageNumber": 0,
                                                            "pageSize": 200,
                                                            "sort": {
                                                                "sorted": true,
                                                                "unsorted": false,
                                                                "empty": false
                                                            }
                                                        },
                                                        "totalPages": 1,
                                                        "totalElements": 2,
                                                        "last": true,
                                                        "size": 200,
                                                        "number": 0,
                                                        "numberOfElements": 2,
                                                        "first": true,
                                                        "empty": false
                                                    }
                                                    """
                                    ),
                                    @ExampleObject(
                                            name = "Пустой результат",
                                            summary = "Ни один из запрошенных документов не найден",
                                            value = """
                                                    {
                                                        "content": [],
                                                        "pageable": {
                                                            "pageNumber": 0,
                                                            "pageSize": 200,
                                                            "sort": {
                                                                "sorted": true,
                                                                "unsorted": false,
                                                                "empty": false
                                                            }
                                                        },
                                                        "totalPages": 0,
                                                        "totalElements": 0,
                                                        "last": true,
                                                        "size": 200,
                                                        "number": 0,
                                                        "numberOfElements": 0,
                                                        "first": true,
                                                        "empty": true
                                                    }
                                                    """
                                    )
                            }
                    )
            ),
            @ApiResponse(
                    responseCode = "500",
                    description = "Внутренняя ошибка сервера",
                    content = @Content(
                            mediaType = MediaType.APPLICATION_JSON_VALUE,
                            schema = @Schema(implementation = ErrorResponse.class)
                    )
            )
    })
    ResponseEntity<Page<DocumentDto>> getDocumentByIdList(
            @PageableDefault(size = PAGE_SIZE, sort = "createDate", direction = Sort.Direction.DESC) Pageable pageable,
            @RequestBody DocumentsRequestDto documentsRequestDto);


    @Operation(summary = "Поиск документов",
            description = "Возвращает список документов, по переданному в запросе списку id документов")
    @PostMapping(value = "/documents/message", consumes = MediaType.APPLICATION_JSON_VALUE, produces = MediaType.APPLICATION_JSON_VALUE)
    @ApiResponse(responseCode = "200",
            description = "Документы успешно получены (даже если некоторые не найдены)",
            content = @Content(
                    mediaType = MediaType.APPLICATION_JSON_VALUE,
                    schema = @Schema(implementation = ApiResponsePageDocumentDto.class),
                    examples = @ExampleObject(
                            value = """
                                    {
                                        "success": true,
                                        "message": "Не найдено документов: 2",
                                        "data": {
                                            "content": [
                                                {
                                                    "id": 1,
                                                    "innerId": "DOC-001",
                                                    "author": "Иванов И.И.",
                                                    "name": "Документ 1",
                                                    "status": "DRAFT"
                                                }
                                            ],
                                            "totalElements": 1,
                                            "totalPages": 1
                                        }
                                    }
                                    """
                    )
            ))
    ResponseEntity<ru.kolivim.document.workflow.dto.response.ApiResponse<Page<DocumentDto>>> getDocumentByIdListWithMessage(
            @PageableDefault(size = PAGE_SIZE, sort = "createDate", direction = Sort.Direction.DESC) Pageable pageable,
            @RequestBody DocumentsRequestDto documentsRequestDto);


    @Operation(summary = "Поиск документов",
            description = "Возвращает список документов, по переданному в запросе списку id документов")
    @PostMapping(value = "/documents/noFoundIds", consumes = MediaType.APPLICATION_JSON_VALUE, produces = MediaType.APPLICATION_JSON_VALUE)
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200",
                    description = "Документы успешно получены (даже если некоторые не найдены)",
                    content = @Content(
                            mediaType = MediaType.APPLICATION_JSON_VALUE,
                            schema = @Schema(implementation = PageResponseDto.class),
                            examples = {
                                    @ExampleObject(
                                            name = "Частично найденные документы",
                                            summary = "Часть документов найдена, часть нет",
                                            value = """
                                                    {
                                                        "page": {
                                                            "content": [
                                                                {
                                                                    "id": 1,
                                                                    "innerId": "DOC-001",
                                                                    "author": "Иванов И.И.",
                                                                    "name": "Договор поставки",
                                                                    "status": "APPROVED",
                                                                    "createDate": "2026-03-01T10:00:00+03:00",
                                                                    "updateDate": "2026-03-02T15:30:00+03:00"
                                                                },
                                                                {
                                                                    "id": 3,
                                                                    "innerId": "DOC-003",
                                                                    "author": "Петров П.П.",
                                                                    "name": "Счет-фактура",
                                                                    "status": "SUBMITTED",
                                                                    "createDate": "2026-03-01T11:00:00+03:00",
                                                                    "updateDate": "2026-03-02T16:00:00+03:00"
                                                                }
                                                            ],
                                                            "totalElements": 2,
                                                            "totalPages": 1,
                                                            "size": 20,
                                                            "number": 0,
                                                            "first": true,
                                                            "last": true,
                                                            "empty": false
                                                        },
                                                        "notFoundIds": [2, 5],
                                                        "notFoundCount": 2,
                                                        "totalRequested": 4
                                                    }
                                                    """
                                    ),
                                    @ExampleObject(
                                            name = "Все документы найдены",
                                            summary = "Все запрошенные документы существуют",
                                            value = """
                                                    {
                                                        "page": {
                                                            "content": [
                                                                {
                                                                    "id": 1,
                                                                    "innerId": "DOC-001",
                                                                    "author": "Иванов И.И.",
                                                                    "name": "Договор поставки",
                                                                    "status": "APPROVED"
                                                                },
                                                                {
                                                                    "id": 2,
                                                                    "innerId": "DOC-002",
                                                                    "author": "Сидоров С.С.",
                                                                    "name": "Акт выполненных работ",
                                                                    "status": "SUBMITTED"
                                                                }
                                                            ],
                                                            "totalElements": 2,
                                                            "totalPages": 1
                                                        },
                                                        "notFoundIds": [],
                                                        "notFoundCount": 0,
                                                        "totalRequested": 2
                                                    }
                                                    """
                                    ),
                                    @ExampleObject(
                                            name = "Ни одного документа не найдено",
                                            summary = "Ни один из запрошенных документов не существует",
                                            value = """
                                                    {
                                                        "page": {
                                                            "content": [],
                                                            "totalElements": 0,
                                                            "totalPages": 0
                                                        },
                                                        "notFoundIds": [100, 101, 102],
                                                        "notFoundCount": 3,
                                                        "totalRequested": 3
                                                    }
                                                    """
                                    )
                            }
                    ))
    })
    ResponseEntity<PageResponseDto> getDocumentByIdListWithNoFoundIds(
            @PageableDefault(size = PAGE_SIZE, sort = "createDate", direction = Sort.Direction.DESC) Pageable pageable,
            @RequestBody DocumentsRequestDto documentsRequestDto);


    @Operation(summary = "Поиск документов",
            description = "Возвращает список документов, по переданному в запросе списку id документов")
    @PostMapping(value = "/documents/extendedPage", consumes = MediaType.APPLICATION_JSON_VALUE, produces = MediaType.APPLICATION_JSON_VALUE)
    @ApiResponses(
            value = {
                    @ApiResponse(responseCode = "200",
                            description = "Документы успешно получены. Информация о запросе в заголовках: X-Total-Requested, X-Not-Found-Count",
                            headers = {
                                    @Header(name = "X-Total-Requested",
                                            description = "Общее количество ID, переданных в запросе",
                                            schema = @Schema(type = "integer", example = "5")),
                                    @Header(name = "X-Not-Found-Count",
                                            description = "Количество ID, не найденных в БД",
                                            schema = @Schema(type = "integer", example = "2"))
                            },
                            content = @Content(
                                    mediaType = MediaType.APPLICATION_JSON_VALUE,
                                    schema = @Schema(type = "object",
                                            subTypes = {DocumentDto.class},
                                            description = "Страница с документами")
                            ))
            })
    ResponseEntity<Page<DocumentDto>> getDocumentByIdListWithExtendedPage(
            @PageableDefault(size = PAGE_SIZE, sort = "createDate", direction = Sort.Direction.DESC) Pageable pageable,
            @RequestBody DocumentsRequestDto documentsRequestDto);


    @Deprecated
    @Operation(summary = "Поиск документов (устаревший метод)",
            description = "**Внимание! Этот метод устарел** " +
                    "Используйте POST /api/v1/document/documents вместо него. " +
                    "Возвращает список документов по переданному списку id (параметр запроса)")
    @GetMapping(produces = MediaType.APPLICATION_JSON_VALUE)
    @ApiResponse(responseCode = "200",
            description = "Документы успешно получены",
            content = @Content(
                    mediaType = MediaType.APPLICATION_JSON_VALUE,
                    schema = @Schema(
                            implementation = Page.class,
                            description = "Страница с документами"
                    ),
                    examples = {
                            @ExampleObject(
                                    name = "Документы найдены",
                                    summary = "Запрошенные документы существуют",
                                    value = """
                                            {
                                                "content": [
                                                    {
                                                        "id": 1,
                                                        "innerId": "DOC-001",
                                                        "author": "Иванов И.И.",
                                                        "name": "Договор поставки",
                                                        "status": "APPROVED",
                                                        "createDate": "2026-03-01T10:00:00+03:00",
                                                        "updateDate": "2026-03-02T15:30:00+03:00"
                                                    },
                                                    {
                                                        "id": 2,
                                                        "innerId": "DOC-002",
                                                        "author": "Петров П.П.",
                                                        "name": "Счет-фактура",
                                                        "status": "SUBMITTED",
                                                        "createDate": "2026-03-01T11:00:00+03:00",
                                                        "updateDate": "2026-03-02T16:00:00+03:00"
                                                    }
                                                ],
                                                "totalElements": 2,
                                                "totalPages": 1,
                                                "size": 20,
                                                "number": 0,
                                                "first": true,
                                                "last": true,
                                                "empty": false
                                            }
                                            """
                            ),
                            @ExampleObject(
                                    name = "Документы не найдены",
                                    summary = "Ни один из запрошенных ID не существует в БД",
                                    value = """
                                            {
                                                "content": [],
                                                "totalElements": 0,
                                                "totalPages": 0,
                                                "size": 20,
                                                "number": 0,
                                                "first": true,
                                                "last": true,
                                                "empty": true
                                            }
                                            """
                            ),
                            @ExampleObject(
                                    name = "Пустой список ID",
                                    summary = "Передан пустой список idList (без обращения к БД)",
                                    value = """
                                            {
                                                "content": [],
                                                "totalElements": 0,
                                                "totalPages": 0,
                                                "size": 20,
                                                "number": 0,
                                                "first": true,
                                                "last": true,
                                                "empty": true
                                            }
                                            """
                            )
                    }
            ))
    ResponseEntity<Page<DocumentDto>> getDocumentByIdListOld(Pageable pageable, @RequestParam List<Long> idList);


    @Deprecated
    @Operation(summary = "Поиск документов с фильтрацией (устаревший метод)",
            description = """
                    **Внимание! Этот метод устарел**
                    Используйте POST /api/v1/document/filter вместо него.
                    Выполняет поиск документов с фильтрацией по полям документа и диапазону дат создания""")
    @ApiResponse(responseCode = "200",
            description = "Документы успешно найдены",
            content = @Content(
                    mediaType = MediaType.APPLICATION_JSON_VALUE,
                    schema = @Schema(
                            implementation = Page.class,
                            description = "Страница с документами, удовлетворяющих критериям поиска"
                    )
            ))
    @PostMapping(value = "/filterWithParameters", produces = MediaType.APPLICATION_JSON_VALUE)
    ResponseEntity<Page<DocumentDto>> getByFilterWithParameters(
            @RequestBody DocumentDto documentDto,
            @PageableDefault(size = PAGE_SIZE, sort = "createDate", direction = Sort.Direction.DESC) Pageable page,
            @Parameter(description = "Дата создания документа. Будут отобраны только документы, созданные после указанной даты")
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) ZonedDateTime startDate,
            @Parameter(description = "Дата создания документа. Будут отобраны только документы, созданные до указанной даты")
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) ZonedDateTime endDate);


    @Operation(summary = "Отправка на согласование",
            description = "Выполняет пакетную отправку документов на согласование")
    @ApiResponses({
            @ApiResponse(
                    responseCode = "200",
                    description = "Обработка завершена. В ответе содержится результат по каждому ID",
                    content = @Content(
                            mediaType = MediaType.APPLICATION_JSON_VALUE,
                            schema = @Schema(
                                    implementation = List.class,
                                    description = "Список результатов обработки по каждому документу"),
                            examples = {
                                    @ExampleObject(
                                            name = "Смешанные результаты",
                                            summary = "Часть документов обработана, часть отклонена, часть не найдена",
                                            value = """
                                                    [
                                                        {
                                                            "id": 1,
                                                            "operationStatus": "SUCCESS"
                                                        },
                                                        {
                                                            "id": 2,
                                                            "operationStatus": "CONFLICT"
                                                        },
                                                        {
                                                            "id": 3,
                                                            "operationStatus": "NOT_FOUND"
                                                        }
                                                    ]
                                                    """
                                    )
                            }
                    )
            ),
            @ApiResponse(
                    responseCode = "400",
                    description = "Ошибка валидации входных данных",
                    content = @Content(
                            mediaType = MediaType.APPLICATION_JSON_VALUE,
                            schema = @Schema(implementation = ErrorResponse.class),
                            examples = {
                                    @ExampleObject(
                                            name = "Пустой список ID",
                                            description = "Список идентификаторов не может быть пустым",
                                            value = """
                                                    {
                                                        "code": "VALIDATION_ERROR",
                                                        "message": "Ошибка валидации входных данных",
                                                        "status": 400,
                                                        "errors": {
                                                            "ids": "Список ID документов не может быть пустым"
                                                        }
                                                    }
                                                    """
                                    ),
                                    @ExampleObject(
                                            name = "Превышен лимит ID",
                                            description = "Список содержит более 1000 элементов",
                                            value = """
                                                    {
                                                        "code": "VALIDATION_ERROR",
                                                        "message": "Ошибка валидации входных данных",
                                                        "status": 400,
                                                        "errors": {
                                                            "ids": "Список должен содержать от 1 до 1000 Id"
                                                        }
                                                    }
                                                    """
                                    ),
                                    @ExampleObject(
                                            name = "Отсутствует инициатор",
                                            description = "Поле author обязательно для этой операции",
                                            value = """
                                                    {
                                                        "code": "VALIDATION_ERROR",
                                                        "message": "Ошибка валидации входных данных",
                                                        "status": 400,
                                                        "timestamp": "2026-03-02T10:00:00+03:00",
                                                        "path": "/api/v1/document/approve",
                                                        "errors": {
                                                            "author": "Инициатор не может быть пустым"
                                                        }
                                                    }
                                                    """
                                    )
                            }
                    )
            ),
            @ApiResponse(
                    responseCode = "500",
                    description = "Внутренняя ошибка сервера (проблемы с БД, системные сбои)",
                    content = @Content(
                            mediaType = MediaType.APPLICATION_JSON_VALUE,
                            schema = @Schema(implementation = ErrorResponse.class),
                            examples = {
                                    @ExampleObject(
                                            name = "Ошибка БД",
                                            value = """
                                                    {
                                                        "code": "INTERNAL_SERVER_ERROR",
                                                        "message": "could not execute statement; SQL [n/a]; nested exception is org.hibernate.exception.JDBCConnectionException: Unable to acquire JDBC Connection",
                                                        "status": 500,
                                                        "timestamp": "2026-03-02T10:00:00+03:00",
                                                        "path": "/api/v1/document/submit"
                                                    }
                                                    """
                                    ),
                                    @ExampleObject(
                                            name = "Некорректный JSON",
                                            value = """
                                                    {
                                                        "code": "INTERNAL_SERVER_ERROR",
                                                        "message": "JSON parse error: Unexpected character",
                                                        "status": 500,
                                                        "timestamp": "2026-03-02T10:00:00+03:00",
                                                        "path": "/api/v1/document/submit"
                                                    }
                                                    """
                                    )
                            }
                    )
            )
    })
    @PutMapping(value = "/submit", produces = MediaType.APPLICATION_JSON_VALUE)
    ResponseEntity<List<DocumentSubmitResponseDto>> submit(
            @PageableDefault(size = PAGE_SIZE, sort = "createDate", direction = Sort.Direction.DESC) Pageable pageable,
            @Validated(DocumentsRequestDto.Submit.class) @RequestBody DocumentsRequestDto documentsRequestDto);


    @Operation(summary = "Отправка документов на утверждение",
            description = "Выполняет пакетное утверждение документов (перевод из SUBMITTED в APPROVED)")
    @ApiResponses({
            @ApiResponse(
                    responseCode = "200",
                    description = "Обработка завершена. В ответе содержится результат по каждому ID",
                    content = @Content(
                            mediaType = MediaType.APPLICATION_JSON_VALUE,
                            schema = @Schema(
                                    implementation = List.class,
                                    description = "Список результатов обработки по каждому документу"
                            ),
                            examples = {
                                    @ExampleObject(
                                            name = "Смешанные результаты",
                                            summary = "Различные статусы для разных документов",
                                            value = """
                                                    [
                                                        {
                                                            "id": 1,
                                                            "operationStatus": "SUCCESS"
                                                        },
                                                        {
                                                            "id": 2,
                                                            "operationStatus": "CONFLICT"
                                                        },
                                                        {
                                                            "id": 3,
                                                            "operationStatus": "NOT_FOUND"
                                                        },
                                                        {
                                                            "id": 4,
                                                            "operationStatus": "REGISTER_ERROR"
                                                        }
                                                    ]
                                                    """
                                    )
                            }
                    )
            ),
            @ApiResponse(
                    responseCode = "400",
                    description = "Ошибка валидации входных данных",
                    content = @Content(
                            mediaType = MediaType.APPLICATION_JSON_VALUE,
                            schema = @Schema(implementation = ErrorResponse.class),
                            examples = {
                                    @ExampleObject(
                                            name = "Пустой список ID",
                                            description = "Список идентификаторов не может быть пустым",
                                            value = """
                                                    {
                                                        "code": "VALIDATION_ERROR",
                                                        "message": "Ошибка валидации входных данных",
                                                        "status": 400,
                                                        "errors": {
                                                            "ids": "Список ID документов не может быть пустым"
                                                        }
                                                    }
                                                    """
                                    ),
                                    @ExampleObject(
                                            name = "Превышен лимит ID",
                                            description = "Список содержит более 1000 элементов",
                                            value = """
                                                    {
                                                        "code": "VALIDATION_ERROR",
                                                        "message": "Ошибка валидации входных данных",
                                                        "status": 400,
                                                        "errors": {
                                                            "ids": "Список должен содержать от 1 до 1000 Id"
                                                        }
                                                    }
                                                    """
                                    ),
                                    @ExampleObject(
                                            name = "Отсутствует инициатор",
                                            description = "Поле author обязательно для этой операции",
                                            value = """
                                                    {
                                                        "code": "VALIDATION_ERROR",
                                                        "message": "Ошибка валидации входных данных",
                                                        "status": 400,
                                                        "timestamp": "2026-03-02T10:00:00+03:00",
                                                        "path": "/api/v1/document/approve",
                                                        "errors": {
                                                            "author": "Инициатор не может быть пустым"
                                                        }
                                                    }
                                                    """
                                    )
                            }
                    )
            ),
            @ApiResponse(
                    responseCode = "500",
                    description = "Внутренняя ошибка сервера (проблемы с БД, системные сбои)",
                    content = @Content(
                            mediaType = MediaType.APPLICATION_JSON_VALUE,
                            schema = @Schema(implementation = ErrorResponse.class))
            )
    })
    @PutMapping(value = "/approve", produces = MediaType.APPLICATION_JSON_VALUE)
    ResponseEntity<List<DocumentSubmitResponseDto>> approve(
            @PageableDefault(size = PAGE_SIZE, sort = "createDate", direction = Sort.Direction.DESC) Pageable pageable,
            @Validated(DocumentsRequestDto.Approve.class) @RequestBody DocumentsRequestDto documentsRequestDto);

}