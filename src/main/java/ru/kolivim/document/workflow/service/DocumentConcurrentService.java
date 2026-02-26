package ru.kolivim.document.workflow.service;

import ru.kolivim.document.workflow.dto.request.ConcurrentApproveRequest;
import ru.kolivim.document.workflow.dto.response.ConcurrentResponseDto;
import ru.kolivim.document.workflow.dto.response.DocumentSubmitResponseDto;
import ru.kolivim.document.workflow.entity.Document;
import ru.kolivim.document.workflow.entity.Register;

import java.util.List;
import java.util.concurrent.CompletableFuture;

public interface DocumentConcurrentService {

    ConcurrentResponseDto concurrentApprove(ConcurrentApproveRequest request);

}
