package ru.kolivim.document.workflow.service;

import org.springframework.data.domain.PageRequest;
import ru.kolivim.document.workflow.dto.DocumentDto;
import ru.kolivim.document.workflow.dto.SearchDocumentDto;
import ru.kolivim.document.workflow.dto.request.DocumentsRequestDto;
import ru.kolivim.document.workflow.dto.response.DocumentSubmitResponseDto;
import ru.kolivim.document.workflow.dto.response.DocumentPage;
import ru.kolivim.document.workflow.dto.response.PageResponseDto;
import ru.kolivim.document.workflow.entity.Document;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import ru.kolivim.document.workflow.entity.enums.Status;

import java.time.ZonedDateTime;
import java.util.List;
import java.util.Optional;

public interface DocumentService {

    Page<DocumentDto> getByFilter(SearchDocumentDto searchDocumentDto, Pageable pageable);

    Page<DocumentDto> getByAdvancedFilter(SearchDocumentDto searchDocumentDto, Pageable pageable);

    DocumentDto create(DocumentDto DocumentDto);

    DocumentDto getById(Long id);

    Page<DocumentDto> getByIdList(Pageable pageable, List<Long> idList);

    /* ru.kolivim.document.workflow.dto.response.ApiResponse<Page<DocumentDto>> */ PageResponseDto getByIdListWithNoFound(Pageable pageable, List<Long> idList);

    /* Page<DocumentDto> */ DocumentPage getByIdListWithExtendedPage(Pageable pageable, List<Long> idList);

    List<DocumentSubmitResponseDto> submit(Pageable pageable, DocumentsRequestDto documentsRequestDto);

    List<DocumentSubmitResponseDto> approve(Pageable pageable, DocumentsRequestDto documentsRequestDto);

    List<Document> getByStatus(Status status, PageRequest pageRequest);


    /** Устаревшие реализации далее */
    /******************************************************************************************************************/


    DocumentDto entityToDto(Document document);

    List<DocumentDto> entitiesToDtos(List<Document> documents);

    @Deprecated
    List<Document> findByStatusAuthorDate(Status status, Optional<String> author,
                                          Optional<ZonedDateTime> startDate, Optional<ZonedDateTime> endDate);

    Document update(Document document);

}
