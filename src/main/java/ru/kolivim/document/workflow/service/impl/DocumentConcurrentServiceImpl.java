package ru.kolivim.document.workflow.service.impl;

import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.orm.ObjectOptimisticLockingFailureException;
import ru.kolivim.document.workflow.dto.AttemptDetailDto;
import ru.kolivim.document.workflow.dto.enums.ConcurrentStatus;
import ru.kolivim.document.workflow.dto.request.ConcurrentApproveRequest;
import ru.kolivim.document.workflow.dto.request.DocumentsRequestDto;
import ru.kolivim.document.workflow.dto.response.ConcurrentResponseDto;
import ru.kolivim.document.workflow.dto.response.DocumentSubmitResponseDto;
import ru.kolivim.document.workflow.entity.Document;
import ru.kolivim.document.workflow.entity.enums.OperationStatus;
import ru.kolivim.document.workflow.entity.enums.Status;
import ru.kolivim.document.workflow.service.DocumentConcurrentService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import ru.kolivim.document.workflow.service.DocumentService;
import java.util.concurrent.TimeUnit;

import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;

@Slf4j
@Service
@RequiredArgsConstructor
public class DocumentConcurrentServiceImpl implements DocumentConcurrentService {

    private final DocumentService documentService;


    /** Многопоточное подтверждение документа */
    public ConcurrentResponseDto concurrentApprove(ConcurrentApproveRequest request){
        log.debug("startMethod, request: {}", request);

        AtomicBoolean isSuccessWork = new AtomicBoolean(true);


        Document document = getCorrectDocument(request.getDocumentId());
        if(document == null) return ConcurrentResponseDto.builder().isSuccess(false).build();


        ExecutorService executor = Executors.newFixedThreadPool(request.getThreads());

        CountDownLatch startLatch = new CountDownLatch(1);                                                              /** Все потоки стартуют одновременно */
        CountDownLatch completionLatch = new CountDownLatch(request.getThreads());                                      /** Ждем завершения всех потоков */

        /** Можно и после заполнения перебрать и по статусам в AttemptDetailDto определить successCount / conflictCount / errorCount */
        List<AttemptDetailDto> allResults = Collections.synchronizedList(new ArrayList<>());
        AtomicInteger successCount = new AtomicInteger(0);
        AtomicInteger conflictCount = new AtomicInteger(0);
        AtomicInteger errorCount = new AtomicInteger(0);

        for (int t = 0; t < request.getThreads(); t++) {

            final int threadNumber = t + 1;

            log.debug("Начало создания Thread с threadNumber: {}", threadNumber);

            executor.submit(() -> {

                String realThreadName = Thread.currentThread().getName();

                try {

                    /** Откладываем старт, до получения сигнала - для одновременного старта, после создания всех потоков */
                    startLatch.await();

                    log.debug("Начало выполнения Thread с threadNumber: {}, realThreadName: {}",
                            threadNumber, realThreadName);

                    for (int a = 0; a < request.getAttempts(); a++) {

                        final int attemptNumber = a + 1;

                        log.info("В Thread с threadNumber: {} начало выполнения попытки APPROVE attemptNumber: {}",
                                threadNumber, attemptNumber);

                        try {

                            DocumentsRequestDto requestDto = DocumentsRequestDto.builder()
                                    .ids(Collections.singletonList(request.getDocumentId()))
                                    .author("concurrentApprove threadNumber: ".concat(String.valueOf(threadNumber)))
                                    .comment("попытка номер: ".concat(String.valueOf(attemptNumber)))
                                    .build();

                            Pageable pageable = PageRequest.of(0, 1);

                            List<DocumentSubmitResponseDto> results = documentService.approve(pageable, requestDto);

                            /** Записываем результат */
                            if (results != null && !results.isEmpty()) {

                                log.debug("В результате утверждения в Thread с threadNumber: {}, realThreadName: {} " +
                                                "получен List<DocumentSubmitResponseDto> results: {}",
                                        threadNumber, realThreadName, results);

                                /** Т.к. отправляли только один документ на согласование, то и забираем первый из списка */
                                DocumentSubmitResponseDto result = results.get(0);

                                AttemptDetailDto detail = AttemptDetailDto.builder()
                                        .threadNumber(threadNumber)
                                        .attemptNumber(attemptNumber)
                                        .isSuccess(result.getOperationStatus().equals(OperationStatus.SUCCESS))
                                        .message("В результате выполнения получен статус: ".concat(result.getOperationStatus().toString()))
                                        .id(request.getDocumentId())
                                        .build();

                                allResults.add(detail);


                                if (result.getOperationStatus().equals(OperationStatus.SUCCESS)) {
                                    successCount.incrementAndGet();
                                    log.debug("Thread: {} c attemptNumber: {} SUCCESS - Документ утверждён (APPROVED)",
                                            threadNumber, attemptNumber);
                                } else if (result.getOperationStatus().equals(OperationStatus.CONFLICT)) {
                                    conflictCount.incrementAndGet();
                                    log.debug("Thread: {} c attemptNumber: {} CONFLICT", threadNumber, attemptNumber);
                                } else {
                                    errorCount.incrementAndGet();
                                    log.debug("Thread: {} c attemptNumber: {} ERROR", threadNumber, attemptNumber);
                                }


                            } else {

                                allResults.add(
                                        AttemptDetailDto.builder()
                                                .threadNumber(threadNumber)
                                                .attemptNumber(attemptNumber)
                                                .isSuccess(false)
                                                .message("В результате выполнения ответ от сервиса не был получен")
                                                .id(request.getDocumentId())
                                                .build());
                                errorCount.incrementAndGet();

                                log.debug("Thread: {} c attemptNumber: {} ERROR", threadNumber, attemptNumber);

                            }


                        } catch (ObjectOptimisticLockingFailureException e) {

                            /** Конфликт оптимистичной блокировки */
                            allResults.add(
                                    AttemptDetailDto.builder()
                                            .threadNumber(threadNumber)
                                            .attemptNumber(attemptNumber)
                                            .isSuccess(false)
                                            .message(ConcurrentStatus.CONFLICT.toString().concat("- конфликт оптимистичной блокировки"))
                                            .id(request.getDocumentId())
                                            .build());
                            conflictCount.incrementAndGet();

                            log.debug("Thread: {} c attemptNumber: {} CONFLICT", threadNumber, attemptNumber);

                        } catch (Exception e) {

                            allResults.add(
                                    AttemptDetailDto.builder()
                                            .threadNumber(threadNumber)
                                            .attemptNumber(attemptNumber)
                                            .isSuccess(false)
                                            .message(e.getMessage())
                                            .id(request.getDocumentId())
                                            .build());
                            errorCount.incrementAndGet();

                            log.error("Thread: {} c attemptNumber: {} ERROR, Exception: {}",
                                    threadNumber, attemptNumber, e.getMessage());
                        }

                    }

                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    log.error("Thread {} interrupted, Exception: {}", threadNumber, e.getMessage());
                    isSuccessWork.set(false);
                } finally {
                    completionLatch.countDown();
                }


            });


        }


        /** Сигнал на старт всех потоков одновременно */
        log.info("Начало работы {} шт Threads c {} шт попыток в каждом", request.getThreads(), request.getAttempts());
        startLatch.countDown();


        /** Ожидаем завершения всех потоков (максимум 300 секунд) */
        try {
            if (!completionLatch.await(300, TimeUnit.SECONDS)) {
                log.info("Timeout ожидания выполнения потоков закончился");
            }
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            log.error("CATCH - ошибка выполнения основного потока InterruptedException: {}", e.getMessage());
            isSuccessWork.set(false);
        }


        executor.shutdown();
        try {
            if (!executor.awaitTermination(5, TimeUnit.SECONDS)) {
                executor.shutdownNow();
            }
        } catch (InterruptedException e) {
            executor.shutdownNow();
            log.error("CATCH - InterruptedException: {}", e.getMessage());
            isSuccessWork.set(false);
        }


        /** Проверяем финальный статус документа */
        Status finalStatus = null;
        Optional<Status> finalStatusOptional = documentService.getStatusOptionalById(request.getDocumentId());
        if(finalStatusOptional.isPresent()) finalStatus = finalStatusOptional.get();

        /** Проверяем количество записей в реестре */
        long registryCount = documentService.registerCountByDocumentId(request.getDocumentId());

        boolean isSuccessWorkApprove = isSuccessWorkApprove(finalStatus, registryCount,
                request.getThreads(), request.getAttempts(),
                successCount.get(), conflictCount.get(), errorCount.get(),
                isSuccessWork.get());

        log.info("endMethod, количество записей Success: {}, Conflict: {}, Error: {}, " +
                        "Итоговый статус: {}, количество записей в Реестре: {}, isSuccessWorkApprove: {}",
                successCount.get(), conflictCount.get(), errorCount.get(), finalStatus, registryCount,
                isSuccessWorkApprove);


        return ConcurrentResponseDto.builder()
                .id(request.getDocumentId())
                .status(finalStatus)
                .countSuccessfulApprove(successCount.get())
                .countFailedApprove(conflictCount.get() + errorCount.get())
                .isSuccess(isSuccessWorkApprove)
                .build();

    }


    public boolean isSuccessWorkApprove(Status finalStatus, long registryCount,
                                        int countThreads, int countAttempts,
                                        int successCount, int conflictCount, int errorCount,
                                        boolean isSuccessWork) {
        log.debug("startMethod, finalStatus: {}, registryCount: {}, countThreads: {}, countAttempts: {}, " +
                        "successCount: {}, conflictCount: {}, errorCount: {}, isSuccessWork: {}",
                finalStatus, registryCount, countThreads, countAttempts, successCount, conflictCount, errorCount,
                isSuccessWork);

        if(!isSuccessWork) return false;

        boolean isCorrectStatus = finalStatus != null && finalStatus.equals(Status.APPROVED);
        if(!isCorrectStatus) return false;

        boolean isCorrectRegisterCount = registryCount == 1;
        if(!isCorrectRegisterCount) return false;

        int plannedTotalAttempts = countThreads * countAttempts;
        int realFinalAttempts = successCount + conflictCount + errorCount;
        boolean isCorrectAttemptsCount = plannedTotalAttempts == realFinalAttempts;
        if(!isCorrectAttemptsCount) return false;

        return true;
    }


    private Document getCorrectDocument(Long documentId) {
        log.debug("startMethod, documentId: {}", documentId);

        Optional<Document> documentOptional = documentService.getDocumentOptionalById(documentId);
        if(!documentOptional.isPresent()) {
            log.info("Не найден документ с documentId: {}", documentId);
            return null /** Написать далее реализацию ответа - Документ не найден */ ;
        }

        Document document = documentOptional.get();

        if (document.getStatus() != Status.SUBMITTED.SUBMITTED) {
            log.info("Не корректный статус документа с documentId: {}", documentId);
            return null;
        }

        log.debug("endMethod, к возврату document: {}", document);
        return document;
    }

}
