package ru.kolivim.document.workflow.service.impl;

import ru.kolivim.document.workflow.dto.DocumentDto;
import ru.kolivim.document.workflow.dto.SubmitDocumentDto;
import ru.kolivim.document.workflow.entity.Document;
import ru.kolivim.document.workflow.entity.History;
import ru.kolivim.document.workflow.entity.enums.Action;
import ru.kolivim.document.workflow.entity.enums.OperationStatus;
import ru.kolivim.document.workflow.entity.enums.Status;
import ru.kolivim.document.workflow.exception.ResourceNotFoundException;
import ru.kolivim.document.workflow.mapper.DocumentMapper;
import ru.kolivim.document.workflow.repository.DocumentRepository;
import ru.kolivim.document.workflow.repository.HistoryRepository;
import ru.kolivim.document.workflow.service.DocumentService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.time.ZonedDateTime;
import java.util.*;

@Service
@RequiredArgsConstructor
public class DocumentServiceImpl implements DocumentService {

    private final DocumentRepository documentRepository;

    private final HistoryRepository historyRepository;

    private final DocumentMapper documentMapper;

    @Override
    @Transactional
    public Document create() {
        Document document = Document.builder()
                .author(generateAuthor())
                .createDate(ZonedDateTime.now())
                .innerId(String.valueOf(Math.random()))
                .status(Status.DRAFT)
                .name(generateTitle()).build();
        return documentRepository.save(document);
    }

    @Override
    public DocumentDto entityToDto(Document document) {
        return documentMapper.entityToDto(document);
    }

    @Override
    public Document findById(Long id) {
        return documentRepository.findById(id).orElseThrow(() -> new ResourceNotFoundException("Document not found"));
    }

    @Override
    public Page<Document> findAll(Pageable pageable, List<Long> idList) {
        List<Document> documents = new ArrayList<>();
        for (Long id: idList){
            Document probe = new Document();
            probe.setId(id);
            ExampleMatcher matcher = ExampleMatcher.matching()
                    .withIgnorePaths("innerId", "author", "title", "status", "createTime", "updateTime", "historySet", "register")
                    .withMatcher("id", ExampleMatcher.GenericPropertyMatcher::startsWith);

            Example<Document> documentExample = Example.of(probe, matcher);
            Page<Document> documentPage = documentRepository.findAll(documentExample, pageable);
            List<Document> documentList = documentPage.getContent();
            documents.addAll(documentList);
        }
        Page<Document> resultPage;
        if (!documents.isEmpty()) {
            int start = (int) pageable.getOffset();
            int end = Math.min((start + pageable.getPageSize()), documents.size());
            resultPage = new PageImpl<>(documents.subList(start, end), pageable, documents.size());
        } else {
            resultPage = new PageImpl<>(documents, pageable, 0);
        }
        return resultPage;
    }

    @Override
    public List<DocumentDto> entitiesToDtos(List<Document> documents) {
        return documentMapper.entitiesToDtos(documents);
    }

    @Override
    @Transactional
    public List<SubmitDocumentDto> submit(List<Long> ids) {
        List<SubmitDocumentDto> submitDocumentDtoList = new ArrayList<>();
        for (Long id: ids){
            if (documentRepository.findById(id).isPresent()){
                if (documentRepository.findById(id).get().getStatus() != Status.DRAFT){
                    submitDocumentDtoList.add(new SubmitDocumentDto(id, OperationStatus.CONFLICT));
                } else {
                    try {
                        update(documentRepository.findById(id).get());
                        submitDocumentDtoList.add(new SubmitDocumentDto(id, OperationStatus.SUCCESS));
                    } catch (Exception e){
                        submitDocumentDtoList.add(new SubmitDocumentDto(id, OperationStatus.REGISTER_MISTAKE));
                    }
                }
            } else {
                submitDocumentDtoList.add(new SubmitDocumentDto(id, OperationStatus.NOT_FOUND));
            }
        }
        return submitDocumentDtoList;
    }


    @Override
    public List<Document> findByStatusAuthorDate(Status status, Optional<String> author, Optional<ZonedDateTime> startDate, Optional<ZonedDateTime> endDate) {

        List<Document> documents = new ArrayList<>(documentRepository.findByStatus(status));
        author.ifPresent(s -> documents.retainAll(documentRepository.findByAuthor(s)));
        startDate.ifPresent(zonedDateTime -> documents.retainAll(documentRepository.findByCreateDateAfter(zonedDateTime)));
        endDate.ifPresent(zonedDateTime -> documents.retainAll(documentRepository.findByCreateDateBefore(zonedDateTime)));

        return documents;
    }

    @Override
    @Transactional
    public Document update(Document document) {

        Document newDocument = documentRepository.findById(document.getId()).orElseThrow();

        if (document.getCreateDate() != null) {
            newDocument.setCreateDate(document.getCreateDate());
        }

        if (document.getAuthor() != null) {
            newDocument.setAuthor(document.getAuthor());
        }

        newDocument.setStatus(Status.SUBMITTED);
        if (document.getName() != null) {
            newDocument.setName(document.getName());
        }

        Set<History> historySet = generateHistorySubmit(document);
        //document.getHistorySet().clear();

        newDocument.getHistorySet().addAll(historySet);
        //historyRepository.saveAll(historySet);
//        for (History history1 : historySet) {
//            history1.setDocument(document);
//        }

        if (document.getInnerId() != null) {
            newDocument.setInnerId(document.getInnerId());
        }

//        newDocument.setUpdateTime(ZonedDateTime.now());

        documentRepository.save(newDocument);
        return newDocument;
    }

    private String generateAuthor(){
        List<String> authors = List.of("Steven Spielberg", "Martin Scorsese", "Christopher Nolan", "Alfred Hitchcock", "Stanley Kubrick");
        Random rand = new Random();
        int n = rand.nextInt(authors.size());
        return authors.get(n);
    }

    private String generateTitle(){
        List<String> titles = List.of("Citizen Kane", "Casablanca", "The Godfather", "Gone with the Wind", "Lawrence of Arabia", "The Wizard of Oz");
        Random rand = new Random();
        int n = rand.nextInt(titles.size());
        return titles.get(n);
    }

    @Transactional(propagation = Propagation.MANDATORY)
    Set<History> generateHistorySubmit(Document document){
        Set<History> histories = new HashSet<>();
        History history = History.builder()
                .action(Action.SUBMIT)
                .date(ZonedDateTime.now())
                .author(document.getAuthor())
                .document(document)
                .build();
        histories.add(history);
        historyRepository.saveAll(histories);
        return histories;
    }

}
