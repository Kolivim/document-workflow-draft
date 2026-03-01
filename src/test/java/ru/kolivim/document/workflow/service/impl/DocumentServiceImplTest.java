package ru.kolivim.document.workflow.service.impl;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.transaction.support.TransactionTemplate;
import ru.kolivim.document.workflow.dto.DocumentDto;
import ru.kolivim.document.workflow.dto.request.SearchDocumentDto;
import ru.kolivim.document.workflow.dto.response.DocumentPage;
import ru.kolivim.document.workflow.dto.response.DocumentSubmitResponseDto;
import ru.kolivim.document.workflow.dto.response.PageResponseDto;
import ru.kolivim.document.workflow.entity.Document;
import ru.kolivim.document.workflow.entity.History;
import ru.kolivim.document.workflow.entity.enums.Action;
import ru.kolivim.document.workflow.entity.enums.OperationStatus;
import ru.kolivim.document.workflow.entity.enums.Status;
import ru.kolivim.document.workflow.exception.ResourceNotFoundException;
import ru.kolivim.document.workflow.mapper.DocumentMapper;
import ru.kolivim.document.workflow.repository.DocumentRepository;
import ru.kolivim.document.workflow.repository.HistoryRepository;
import ru.kolivim.document.workflow.repository.RegisterRepository;

import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class DocumentServiceImplTest {

    @Mock
    private DocumentRepository documentRepository;

    @Mock
    private HistoryRepository historyRepository;

    @Mock
    private RegisterRepository registerRepository;

    @Mock
    private TransactionTemplate transactionTemplate;

    @Mock
    private DocumentMapper documentMapper;

    @InjectMocks
    private DocumentServiceImpl documentService;

    @Captor
    private ArgumentCaptor<Document> documentCaptor;

    @Captor
    private ArgumentCaptor<History> historyCaptor;

    @Captor
    private ArgumentCaptor<Specification<Document>> specificationCaptor;

    private Document testDocument;
    private DocumentDto testDocumentDto;
    private final Long TEST_DOCUMENT_ID = 1L;
    private final String TEST_AUTHOR = "testAuthor";
    private final String TEST_COMMENT = "testComment";
    private final String TEST_DOCUMENT_NAME = "Test Document";


    @BeforeEach
    void setUp() {

        ZonedDateTime now = ZonedDateTime.now(ZoneId.of("Europe/Moscow"));

        testDocument = new Document();
        testDocument.setId(TEST_DOCUMENT_ID);
        testDocument.setStatus(Status.DRAFT);
        testDocument.setAuthor(TEST_AUTHOR);
        testDocument.setName(TEST_DOCUMENT_NAME);
        testDocument.setCreateDate(now);
        testDocument.setUpdateDate(now);

        testDocumentDto = new DocumentDto();
        testDocumentDto.setId(TEST_DOCUMENT_ID);
        testDocumentDto.setStatus(Status.DRAFT);
        testDocumentDto.setAuthor(TEST_AUTHOR);
        testDocumentDto.setName(TEST_DOCUMENT_NAME);
        testDocumentDto.setCreateDate(now);
        testDocumentDto.setUpdateDate(now);

    }


    @Test
    @DisplayName("Успешное создание документа")
    void createShouldReturnDocumentDtoWhenDocumentIsCreated() {

        when(documentMapper.dtoToNewEntity(testDocumentDto)).thenReturn(testDocument);
        when(documentRepository.save(any(Document.class))).thenReturn(testDocument);
        when(documentMapper.entityToDto(testDocument)).thenReturn(testDocumentDto);

        DocumentDto result = documentService.create(testDocumentDto);

        assertThat(result).isNotNull();
        assertThat(result.getId()).isEqualTo(TEST_DOCUMENT_ID);
        assertThat(result.getStatus()).isEqualTo(Status.DRAFT);
        assertThat(result.getAuthor()).isEqualTo(TEST_AUTHOR);

        verify(documentRepository).save(documentCaptor.capture());
        Document savedDocument = documentCaptor.getValue();
        assertThat(savedDocument.getStatus()).isEqualTo(Status.DRAFT);
        assertThat(savedDocument.getAuthor()).isEqualTo(TEST_AUTHOR);

    }


    @Test
    @DisplayName("Успешное получение документа по ID")
    void getByIdShouldReturnDocumentDtoWhenDocumentExists() {

        when(documentRepository.findById(TEST_DOCUMENT_ID)).thenReturn(Optional.of(testDocument));
        when(documentMapper.entityToDto(testDocument)).thenReturn(testDocumentDto);

        DocumentDto result = documentService.getById(TEST_DOCUMENT_ID);

        assertThat(result).isNotNull();
        assertThat(result.getId()).isEqualTo(TEST_DOCUMENT_ID);
        assertThat(result.getStatus()).isEqualTo(Status.DRAFT);

    }


    @Test
    @DisplayName("Получение документа по ID - документ не найден")
    void getByIdWithThrowExceptionWhenDocumentNotFound() {

        when(documentRepository.findById(TEST_DOCUMENT_ID)).thenReturn(Optional.empty());


        assertThatThrownBy(() -> documentService.getById(TEST_DOCUMENT_ID))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessage("Документ не найден для id: ".concat(TEST_DOCUMENT_ID.toString()));

    }


    @Test
    @DisplayName("Успешное получение статуса документа по ID")
    void getStatusOptionalByIdShouldReturnStatusWhenDocumentExists() {

        when(documentRepository.findStatusById(TEST_DOCUMENT_ID)).thenReturn(Optional.of(Status.DRAFT));

        Optional<Status> result = documentService.getStatusOptionalById(TEST_DOCUMENT_ID);

        assertThat(result).isPresent();
        assertThat(result.get()).isEqualTo(Status.DRAFT);

    }


    @Test
    @DisplayName("Успешный поиск документов по фильтру")
    void getByFilterShouldReturnPageOfDocuments() {

        SearchDocumentDto searchDto = new SearchDocumentDto();
        searchDto.setAuthor(TEST_AUTHOR);
        searchDto.setStatus(Status.DRAFT);

        Pageable pageable = PageRequest.of(0, 10);
        List<Document> documents = List.of(testDocument);
        Page<Document> documentPage = new PageImpl<>(documents, pageable, documents.size());

        when(documentRepository.findAll(any(Specification.class), eq(pageable))).thenReturn(documentPage);
        when(documentMapper.entityToDto(testDocument)).thenReturn(testDocumentDto);

        Page<DocumentDto> result = documentService.getByFilter(searchDto, pageable);

        assertThat(result).isNotNull();
        assertThat(result.getContent()).hasSize(1);
        assertThat(result.getContent().get(0).getId()).isEqualTo(TEST_DOCUMENT_ID);
        verify(documentRepository).findAll(any(Specification.class), eq(pageable));

    }


    @Test
    @DisplayName("Успешное получение документов по списку ID")
    void getByIdListShouldReturnPageOfDocuments() {

        List<Long> idList = List.of(TEST_DOCUMENT_ID, 2L, 3L);
        Pageable pageable = PageRequest.of(0, 10);
        List<Document> documents = List.of(testDocument);
        Page<Document> documentPage = new PageImpl<>(documents, pageable, documents.size());

        when(documentRepository.findAll(any(Specification.class), eq(pageable))).thenReturn(documentPage);
        when(documentMapper.entityToDto(testDocument)).thenReturn(testDocumentDto);

        Page<DocumentDto> result = documentService.getByIdList(pageable, idList);

        assertThat(result).isNotNull();
        assertThat(result.getContent()).hasSize(1);
        verify(documentRepository).findAll(any(Specification.class), eq(pageable));

    }


    @Test
    @DisplayName("Успешное получение расширенной страницы документов по списку ID")
    void getByIdListWithExtendedPageShouldReturnDocumentPage() {

        List<Long> idList = List.of(TEST_DOCUMENT_ID, 2L, 3L);
        Pageable pageable = PageRequest.of(0, 10);
        List<Document> documents = List.of(testDocument);
        Page<Document> documentPage = new PageImpl<>(documents, pageable, documents.size());

        when(documentRepository.findAll(any(Specification.class), eq(pageable))).thenReturn(documentPage);
        when(documentRepository.findAllExistingIds(idList)).thenReturn(List.of(TEST_DOCUMENT_ID));
        when(documentMapper.entityToDto(testDocument)).thenReturn(testDocumentDto);

        DocumentPage<DocumentDto> result = documentService.getByIdListWithExtendedPage(pageable, idList);

        assertThat(result).isNotNull();
        assertThat(result.getContent()).hasSize(1);
        assertThat(result.getNotFoundIds()).hasSize(2);
        assertThat(result.getNotFoundCount()).isEqualTo(2);
        assertThat(result.getTotalCount()).isEqualTo(3);
    }


    @Test
    @DisplayName("Успешная отправка документа на утверждение (submit)")
    void submitShouldReturnSuccessWhenDocumentIsSubmitted() {

        when(documentRepository.existsById(TEST_DOCUMENT_ID)).thenReturn(true);
        when(documentRepository.updateStatusIfExpected(TEST_DOCUMENT_ID, Status.DRAFT, Status.SUBMITTED))
                .thenReturn(1);
        when(documentRepository.findById(TEST_DOCUMENT_ID)).thenReturn(Optional.of(testDocument));
        when(historyRepository.save(any(History.class))).thenReturn(new History());

        DocumentSubmitResponseDto result = documentService.submit(TEST_DOCUMENT_ID, TEST_AUTHOR, TEST_COMMENT);

        assertThat(result).isNotNull();
        assertThat(result.getId()).isEqualTo(TEST_DOCUMENT_ID);
        assertThat(result.getOperationStatus()).isEqualTo(OperationStatus.SUCCESS);

        verify(historyRepository).save(historyCaptor.capture());
        History savedHistory = historyCaptor.getValue();
        assertThat(savedHistory.getAction()).isEqualTo(Action.SUBMIT);
        assertThat(savedHistory.getAuthor()).isEqualTo(TEST_AUTHOR);
        assertThat(savedHistory.getComment()).isEqualTo(TEST_COMMENT);
        assertThat(savedHistory.getDocument()).isEqualTo(testDocument);

    }


    @Test
    @DisplayName("Успешное утверждение документа (approve)")
    void approveShouldReturnSuccessWhenDocumentIsApproved() {

        testDocument.setStatus(Status.SUBMITTED);

        when(documentRepository.existsById(TEST_DOCUMENT_ID)).thenReturn(true);
        when(documentRepository.existsByIdAndStatus(TEST_DOCUMENT_ID, Status.SUBMITTED)).thenReturn(true);
        when(registerRepository.insertIfNotExists(TEST_DOCUMENT_ID)).thenReturn(1);
        when(documentRepository.updateStatusIfExpected(TEST_DOCUMENT_ID, Status.SUBMITTED, Status.APPROVED))
                .thenReturn(1);
        when(documentRepository.getById(TEST_DOCUMENT_ID)).thenReturn(testDocument);
        when(historyRepository.save(any(History.class))).thenReturn(new History());

        DocumentSubmitResponseDto result = documentService.approve(TEST_DOCUMENT_ID, TEST_AUTHOR, TEST_COMMENT);

        assertThat(result).isNotNull();
        assertThat(result.getId()).isEqualTo(TEST_DOCUMENT_ID);
        assertThat(result.getOperationStatus()).isEqualTo(OperationStatus.SUCCESS);

        verify(registerRepository).insertIfNotExists(TEST_DOCUMENT_ID);
        verify(documentRepository).updateStatusIfExpected(TEST_DOCUMENT_ID, Status.SUBMITTED, Status.APPROVED);
        verify(historyRepository).save(historyCaptor.capture());

        History savedHistory = historyCaptor.getValue();
        assertThat(savedHistory.getAction()).isEqualTo(Action.APPROVE);
        assertThat(savedHistory.getAuthor()).isEqualTo(TEST_AUTHOR);
        assertThat(savedHistory.getComment()).isEqualTo(TEST_COMMENT);

    }

    @Test
    @DisplayName("Успешное получение списка несуществующих ID")
    void getNotExistingIdsShouldReturnListOfNotFoundIds() {

        List<Long> idList = List.of(1L, 2L, 3L, 4L, 5L);
        when(documentRepository.findAllExistingIds(idList)).thenReturn(List.of(1L, 3L, 5L));


        List<Long> result = documentService.getNotExistingIds(idList);


        assertThat(result).isNotNull();
        assertThat(result).hasSize(2);
        assertThat(result).containsExactly(2L, 4L);

    }


    @Test
    @DisplayName("Успешное получение документов по статусу")
    void getByStatusShouldReturnListOfDocuments() {

        PageRequest pageRequest = PageRequest.of(0, 10);
        List<Document> documents = List.of(testDocument);
        when(documentRepository.findDocumentsByStatus(Status.DRAFT, pageRequest)).thenReturn(documents);

        List<Document> result = documentService.getByStatus(Status.DRAFT, pageRequest);

        assertThat(result).isNotNull();
        assertThat(result).hasSize(1);
        assertThat(result.get(0).getStatus()).isEqualTo(Status.DRAFT);

    }

}
