package ru.kolivim.document.workflow.service.impl;

import ru.kolivim.document.workflow.dto.SubmitDocumentDto;
import ru.kolivim.document.workflow.entity.Document;
import ru.kolivim.document.workflow.entity.History;
import ru.kolivim.document.workflow.entity.Register;
import ru.kolivim.document.workflow.entity.enums.Action;
import ru.kolivim.document.workflow.entity.enums.OperationStatus;
import ru.kolivim.document.workflow.entity.enums.Status;
import ru.kolivim.document.workflow.repository.DocumentRepository;
import ru.kolivim.document.workflow.repository.HistoryRepository;
import ru.kolivim.document.workflow.repository.RegisterRepository;
import ru.kolivim.document.workflow.service.RegisterService;
import lombok.RequiredArgsConstructor;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.ZonedDateTime;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

@Service
@RequiredArgsConstructor
public class RegisterServiceImpl implements RegisterService {

    private final RegisterRepository registerRepository;

    private final DocumentRepository documentRepository;

    private final HistoryRepository historyRepository;

    @Override
    @Transactional
    public CompletableFuture<List<SubmitDocumentDto>> parallelApproveOne(Long id, int threads, int attempts) {
        ExecutorService executorService = Executors.newFixedThreadPool(threads);
        List<Long> idList = new ArrayList<>();
        for (int i = 0; i < attempts; i++){
            idList.add(id);
        }
        return CompletableFuture.supplyAsync(() -> approve(idList), executorService);
    }


    @Override
    @Transactional
    public List<SubmitDocumentDto> approve(List<Long> ids) {
        List<SubmitDocumentDto> submitDocumentDtoList = new ArrayList<>();
        for (Long id: ids){
            if (documentRepository.findById(id).isPresent()){
                if (documentRepository.findById(id).get().getStatus() != Status.SUBMITTED){
                    submitDocumentDtoList.add(new SubmitDocumentDto(id, OperationStatus.CONFLICT));
                } else {
                    try {
                        save(documentRepository.findById(id).get());
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

    @Async
    @Transactional
    @Override
    public CompletableFuture<List<SubmitDocumentDto>> parallelApproveTwo(Long id){
        return CompletableFuture.completedFuture(approve(List.of(id)));
    }

    @Override
    @Transactional
    public Register save(Document document) {
        Register register = Register.builder()
                .document(document)
                .status(Status.APPROVED)
                .build();
        registerRepository.save(register);
        History history = History.builder()
                .time(ZonedDateTime.now())
                .action(Action.APPROVE)
                .author(register.getDocument().getAuthor())
                .document(register.getDocument())
                .build();
        historyRepository.save(history);
        document.setStatus(Status.APPROVED);
        Set<History> historySet = new HashSet<>();
        historySet.add(history);
        //document.getHistorySet().clear();
        document.getHistorySet().addAll(historySet);
//        for (History history1 : historySet) {
//            history1.setDocument(document);
//        }
        document.setRegister(register);
        documentRepository.save(document);
        return register;
    }

//    @Transactional
//    protected Set<History> generateHistoryApprove(Document document){
//        Set<History> histories = new HashSet<>();
//        History history = History.builder()
//                .action(Action.APPROVE)
//                .time(ZonedDateTime.now())
//                .author(document.getAuthor())
//                .document(document)
//                .build();
//        histories.add(history);
//        historyRepository.save(history);
//        return histories;
//    }
}
