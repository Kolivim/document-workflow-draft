package ru.kolivim.document.workflow.service;

import ru.kolivim.document.workflow.dto.SubmitDocumentDto;
import ru.kolivim.document.workflow.entity.Document;
import ru.kolivim.document.workflow.entity.Register;

import java.util.List;
import java.util.concurrent.CompletableFuture;

public interface RegisterService {

    List<SubmitDocumentDto> approve(List<Long> ids);

    CompletableFuture<List<SubmitDocumentDto>> parallelApproveOne(Long id, int threads, int attempts) throws InterruptedException;

    CompletableFuture<List<SubmitDocumentDto>> parallelApproveTwo(Long id);

    Register save(Document document);
}
