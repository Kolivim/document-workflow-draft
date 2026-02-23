package ru.kolivim.document.workflow.service;

import ru.kolivim.document.workflow.dto.response.DocumentSubmitResponseDto;
import ru.kolivim.document.workflow.entity.Document;
import ru.kolivim.document.workflow.entity.Register;

import java.util.List;
import java.util.concurrent.CompletableFuture;

public interface RegisterService {

    List<DocumentSubmitResponseDto> approve(List<Long> ids);

    CompletableFuture<List<DocumentSubmitResponseDto>> parallelApproveOne(Long id, int threads, int attempts) throws InterruptedException;

    CompletableFuture<List<DocumentSubmitResponseDto>> parallelApproveTwo(Long id);

    Register save(Document document);
}
