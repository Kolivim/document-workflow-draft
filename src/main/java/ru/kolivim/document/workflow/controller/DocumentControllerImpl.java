package ru.kolivim.document.workflow.controller;

import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;
import ru.kolivim.document.workflow.controller.api.DocumentApi;
import ru.kolivim.document.workflow.dto.DocumentDto;
import ru.kolivim.document.workflow.dto.request.DocumentsRequestDto;
import ru.kolivim.document.workflow.dto.request.SearchDocumentDto;
import ru.kolivim.document.workflow.dto.response.DocumentPage;
import ru.kolivim.document.workflow.dto.response.DocumentSubmitResponseDto;
import ru.kolivim.document.workflow.dto.response.PageResponseDto;
import ru.kolivim.document.workflow.service.DocumentService;

import java.time.ZonedDateTime;
import java.util.List;

@Validated
@RestController
@RequiredArgsConstructor
public class DocumentControllerImpl implements DocumentApi {

    private final DocumentService service;


    @Override
    public ResponseEntity<Page<DocumentDto>> getByFilter(
            @Validated(DocumentDto.Search.class) @RequestBody SearchDocumentDto searchDocumentDto,
            @PageableDefault(size = PAGE_SIZE, sort = "createDate", direction = Sort.Direction.DESC) Pageable page) {
        return ResponseEntity.ok(service.getByFilter(searchDocumentDto, page));
    }


    @Override
    public ResponseEntity<Page<DocumentDto>> getByAdvancedFilter(
            @Validated(DocumentDto.Search.class) @RequestBody SearchDocumentDto searchDocumentDto,
            @PageableDefault(size = PAGE_SIZE, sort = "createDate", direction = Sort.Direction.DESC) Pageable page) {
        return ResponseEntity.ok(service.getByAdvancedFilter(searchDocumentDto, page));
    }


    @Override
    public ResponseEntity<DocumentDto> create(
            @Validated(DocumentDto.Create.class) @RequestBody DocumentDto documentDto) {
        return ResponseEntity.ok(service.create(documentDto));
    }


    @Override
    public ResponseEntity<DocumentDto> getDocumentById(@PathVariable("id") long id) {
        return ResponseEntity.ok(service.getById(id));
    }


    @Override
    public ResponseEntity<Page<DocumentDto>> getDocumentByIdList(
            @PageableDefault(size = PAGE_SIZE, sort = "createDate", direction = Sort.Direction.DESC) Pageable pageable,
            @RequestBody DocumentsRequestDto documentsRequestDto) {
        Page<DocumentDto> pageResult = service.getByIdList(pageable, documentsRequestDto.getIds());
        return ResponseEntity.ok(pageResult);
    }


    @Override
    public ResponseEntity<ru.kolivim.document.workflow.dto.response.ApiResponse<Page<DocumentDto>>> getDocumentByIdListWithMessage(
            @PageableDefault(size = PAGE_SIZE, sort = "createDate", direction = Sort.Direction.DESC) Pageable pageable,
            @RequestBody DocumentsRequestDto documentsRequestDto) {

        PageResponseDto pageResponseDto = service.getByIdListWithNoFound(pageable, documentsRequestDto.getIds());

        ru.kolivim.document.workflow.dto.response.ApiResponse response =
                ru.kolivim.document.workflow.dto.response.ApiResponse.success(
                        pageResponseDto.getPage(),
                        "Не найдено документов: " + pageResponseDto.getNotFoundCount()
                );

        return ResponseEntity.ok(response);
    }


    @Override
    public ResponseEntity<PageResponseDto> getDocumentByIdListWithNoFoundIds(
            @PageableDefault(size = PAGE_SIZE, sort = "createDate", direction = Sort.Direction.DESC) Pageable pageable,
            @RequestBody DocumentsRequestDto documentsRequestDto) {

        PageResponseDto pageResponseDto = service.getByIdListWithNoFound(pageable, documentsRequestDto.getIds());
        return ResponseEntity.ok(pageResponseDto);
    }


    @Override
    public ResponseEntity<Page<DocumentDto>> getDocumentByIdListWithExtendedPage(
            @PageableDefault(size = PAGE_SIZE, sort = "createDate", direction = Sort.Direction.DESC) Pageable pageable,
            @RequestBody DocumentsRequestDto documentsRequestDto) {

        DocumentPage pageResult = service.getByIdListWithExtendedPage(pageable, documentsRequestDto.getIds());

        return ResponseEntity.ok()
                .header("X-Total-Requested", String.valueOf(pageResult.getTotalCount()))
                .header("X-Not-Found-Count", String.valueOf(pageResult.getNotFoundCount()))
                .body(pageResult);
    }


    @Override
    public ResponseEntity<List<DocumentSubmitResponseDto>> submit(
            @PageableDefault(size = PAGE_SIZE, sort = "createDate", direction = Sort.Direction.DESC) Pageable pageable,
            @Validated(DocumentsRequestDto.Submit.class) @RequestBody DocumentsRequestDto documentsRequestDto) {
        return ResponseEntity.ok(service.submit(pageable, documentsRequestDto));
    }


    @Override
    public ResponseEntity<List<DocumentSubmitResponseDto>> approve(
            @PageableDefault(size = PAGE_SIZE, sort = "createDate", direction = Sort.Direction.DESC) Pageable pageable,
            @Validated(DocumentsRequestDto.Approve.class) @RequestBody DocumentsRequestDto documentsRequestDto) {
        return ResponseEntity.ok(service.approve(pageable, documentsRequestDto));
    }


    @Override
    @Deprecated
    public ResponseEntity<Page<DocumentDto>> getDocumentByIdListOld(
            Pageable pageable,
            @RequestParam List<Long> idList) {
        return ResponseEntity.ok(service.getByIdList(pageable, idList));
    }


    @Override
    @Deprecated
    public ResponseEntity<Page<DocumentDto>> getByFilterWithParameters(
            @RequestBody DocumentDto documentDto,
            @PageableDefault(size = PAGE_SIZE, sort = "createDate", direction = Sort.Direction.DESC) Pageable page,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) ZonedDateTime startDate,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) ZonedDateTime endDate) {

        SearchDocumentDto searchDocumentDto = new SearchDocumentDto(documentDto, startDate, endDate);
        return ResponseEntity.ok(service.getByFilter(searchDocumentDto, page));
    }

}
