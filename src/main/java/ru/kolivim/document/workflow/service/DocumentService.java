package ru.kolivim.document.workflow.service;

import org.springframework.data.domain.PageRequest;
import org.springframework.data.repository.query.Param;
import ru.kolivim.document.workflow.dto.DocumentDto;
import ru.kolivim.document.workflow.dto.request.SearchDocumentDto;
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

    Document getDocumentById(Long id);

    Optional<Document> getDocumentOptionalById(Long id);

    Optional<Status> getStatusOptionalById(Long id);

    DocumentDto getById(Long id);

    long registerCountByDocumentId(Long documentId);

    Page<DocumentDto> getByIdList(Pageable pageable, List<Long> idList);

    PageResponseDto getByIdListWithNoFound(Pageable pageable, List<Long> idList);

    DocumentPage getByIdListWithExtendedPage(Pageable pageable, List<Long> idList);

    List<DocumentSubmitResponseDto> submit(Pageable pageable, DocumentsRequestDto documentsRequestDto);

    List<DocumentSubmitResponseDto> approve(Pageable pageable, DocumentsRequestDto documentsRequestDto);

    List<Document> getByStatus(Status status, PageRequest pageRequest);

}
