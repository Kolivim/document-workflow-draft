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

//    private final RegisterRepository registerRepository;

//    private final RestTemplate restTemplate;
//    private final String baseUrl = "http://localhost:8080/api";                                                         /** Базовый URL API (должен быть настроен в конфигурации) */


    /** Многопоточное подтверждение документа через Http */
    public ConcurrentResponseDto concurrentHttpApprove(ConcurrentApproveRequest request){
        log.debug("startMethod, request: {}", request);


        /*

        Document document = getCorrectDocument(request.getDocumentId());
        //  1 :
//        Optional<Document> documentOptional = documentRepository.findById(request.getDocumentId());
//        if(!documentOptional.isPresent()) return null - Написать реализацию ответа - Документ не найден;
//        Document document = documentOptional.get();
//
//
//        if (document.getStatus() != Status.SUBMITTED.SUBMITTED) {
//            return null - Написать реализацию ответа - Статус документа не соответствует требуемому (SUBMITTED);
//        }
        //  !1


        /** Создаем пул потоков
        ExecutorService executorService = Executors.newFixedThreadPool(threads);


        /** Счетчики результатов
        AtomicInteger successCount = new AtomicInteger(0);
        AtomicInteger conflictCount = new AtomicInteger(0);
        AtomicInteger errorCount = new AtomicInteger(0);


        /** Детали попыток
        List<ConcurrentTestResult.AttemptDetail> attemptDetails = Collections.synchronizedList(new ArrayList<>());


        /** Счетчик для ожидания завершения
        CountDownLatch latch = new CountDownLatch(threads * attempts);


        /** Запускаем параллельные попытки
        for (int t = 0; t < threads; t++) {
            final int threadNumber = t + 1;

            for (int a = 0; a < attempts; a++) {

                final int attemptNumber = a + 1;

                executorService.submit(() -> {

                    try {

                        /** ИСПОЛЬЗУЕМ СУЩЕСТВУЮЩИЙ API УТВЕРЖДЕНИЯ
                        String url = baseUrl + "/documents/approve?ids=" + documentId
                                + "&initiator=concurrent-test&comment=Concurrent test (thread="
                                + threadNumber + ",attempt=" + attemptNumber + ")";

                        log.debug("Thread {}/{}: Calling approve API", threadNumber, attemptNumber);

                        // Вызываем существующий endpoint
                        ResponseEntity<ApproveResult[]> response = restTemplate.exchange(
                                url,
                                HttpMethod.POST,
                                null,
                                ApproveResult[].class
                        );


                        /** Анализируем результат
                        ApproveResult[] results = response.getBody();
                        if (results != null && results.length > 0) {
                            ApproveResult result = results[0];

                            ConcurrentTestResult.AttemptDetail detail =
                                    ConcurrentTestResult.AttemptDetail.builder()
                                            .threadNumber(threadNumber)
                                            .attemptNumber(attemptNumber)
                                            .status(mapResultStatus(result.getStatus()))
                                            .message(result.getMessage())
                                            .build();

                            attemptDetails.add(detail);

                            // Обновляем счетчики
                            switch (result.getStatus()) {
                                case "SUCCESS":
                                    successCount.incrementAndGet();
                                    log.info("Thread {}/{}: SUCCESS - Document approved",
                                            threadNumber, attemptNumber);
                                    break;
                                case "CONFLICT":
                                    conflictCount.incrementAndGet();
                                    log.debug("Thread {}/{}: CONFLICT - {}",
                                            threadNumber, attemptNumber, result.getMessage());
                                    break;
                                default:
                                    errorCount.incrementAndGet();
                                    log.warn("Thread {}/{}: ERROR - {}",
                                            threadNumber, attemptNumber, result.getMessage());
                            }
                        }

                    } catch (Exception e) {
                        log.error("Thread {}/{}: EXCEPTION - {}",
                                threadNumber, attemptNumber, e.getMessage());

                        errorCount.incrementAndGet();
                        attemptDetails.add(ConcurrentTestResult.AttemptDetail.builder()
                                .threadNumber(threadNumber)
                                .attemptNumber(attemptNumber)
                                .status("ERROR")
                                .message("API call failed: " + e.getMessage())
                                .build());
                    } finally {
                        latch.countDown();
                    }
                });
            }
        }


        /** Ожидаем завершения всех попыток
        try {
            latch.await(60, TimeUnit.SECONDS);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            log.error("Test interrupted", e);
        }


        /** Завершаем пул потоков
        executorService.shutdown();


        /** Получаем финальный статус
        Document finalDocument = documentRepository.findById(documentId).orElse(null);
        String finalStatus = finalDocument != null ? finalDocument.getStatus().name() : "UNKNOWN";


        /** Проверяем записи в реестре
        long registryCount = registryRepository.countByDocumentId(documentId);


        /** Формируем результат
        ConcurrentTestResult result = ConcurrentTestResult.builder()
                .documentId(documentId)
                .finalStatus(finalStatus)
                .successfulApprovals(successCount.get())
                .conflicts(conflictCount.get())
                .errors(errorCount.get())
                .totalAttempts(threads * attempts)
                .registryEntriesCount(registryCount)
                .attemptDetails(attemptDetails)
                .build();


        log.info("=== CONCURRENT TEST COMPLETED ===");
        log.info("Final document status: {}", finalStatus);
        log.info("Registry entries created: {}", registryCount);
        log.info("Successful approvals: {}", result.getSuccessfulApprovals());
        log.info("Conflicts: {}", result.getConflicts());
        log.info("Errors: {}", result.getErrors());
        log.info("Total attempts: {}", result.getTotalAttempts());
        log.info("==================================");


//        return result;
      */

        return null;

    }


    /** Многопоточное подтверждение документа через DocumentService */
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


                                //
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
                                //


                            } else {

                                /** Ошибка */
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

                            /** Ошибка */
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


                        /** Небольшая задержка между попытками в одном потоке */
                        /* Thread.sleep(5); */

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


        //
        /** Проверяем финальный статус документа */
        Status finalStatus = null;
        Optional<Status> finalStatusOptional = documentService.getStatusOptionalById(request.getDocumentId());
        if(finalStatusOptional.isPresent()) finalStatus = finalStatusOptional.get();
//        boolean isCorrectStatus = finalStatus != null && finalStatus.equals(Status.APPROVED);

        /** Проверяем количество записей в реестре */
        long registryCount = documentService.registerCountByDocumentId(request.getDocumentId());                        // registerRepository.countByDocumentId(request.getDocumentId());
//        boolean isCorrectRegisterCount = registryCount == 1;
        //

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
                .isSuccess( isSuccessWorkApprove /* isCorrectStatus && isCorrectRegisterCount && isSuccessWork.get() */ )
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




    /** Многопоточное подтверждение документа через DocumentService */
    public ConcurrentResponseDto concurrentApproveV1(ConcurrentApproveRequest request){
        log.debug("startMethod, request: {}", request);

        /*
        Document document = getCorrectDocument(request.getDocumentId());
        //  1:
//        Optional<Document> documentOptional = documentRepository.findById(request.getDocumentId());
//        if(!documentOptional.isPresent()) return null - Написать реализацию ответа - Документ не найден ;
//        Document document = documentOptional.get();
//
//
//        if (document.getStatus() != Status.SUBMITTED.SUBMITTED) {
//            return null Написать реализацию ответа - Статус документа не соответствует требуемому (SUBMITTED) ;
//        }
        //  !1


        // A1 : Запускаем параллельные попытки
        ExecutorService executor = Executors.newFixedThreadPool(request.getThreads());
//        CountDownLatch latch = new CountDownLatch(request.getThreads() * request.getAttempts());

        List<Future<AttemptDetailDto>> futures = new ArrayList<>();                                                     //  AttemptDetailDto - То что вернется из Future

        // Для КАЖДОГО потока создаем одну задачу, которая делает несколько попыток
        for (int t = 0; t < threads; t++) {
            final int threadNumber = t + 1;

            Callable<List<AttemptDetailDto>> threadTask = () -> {
                List<AttemptDetailDto> threadResults = new ArrayList<>();

                // Внутри одного потока делаем attempts попыток ПОСЛЕДОВАТЕЛЬНО
                for (int a = 0; a < attempts; a++) {
                    final int attemptNumber = a + 1;

                    log.debug("Thread {}/{}: Starting attempt", threadNumber, attemptNumber);

                    try {

                        Document approved = documentService.approveDocument(
                                documentId,
                                "concurrent-tester",
                                String.format("Thread %d, attempt %d", threadNumber, attemptNumber)
                        );

                        threadResults.add(AttemptDetailDto.success(
                                threadNumber,
                                attemptNumber,
                                "Document approved"
                        ));

                    } catch (ObjectOptimisticLockingFailureException e) {
                        threadResults.add(AttemptDetailDto.conflict(
                                threadNumber,
                                attemptNumber,
                                "Optimistic lock - document was modified by another thread"
                        ));
                    } catch (Exception e) {
                        threadResults.add(AttemptDetailDto.error(
                                threadNumber,
                                attemptNumber,
                                e.getMessage()
                        ));
                    }
                }

                return threadResults;
            };

            threadFutures.add(executor.submit(threadTask));
        }


        // Собираем результаты от всех потоков
        List<AttemptDetailDto> allResults = new ArrayList<>();
        for (Future<List<AttemptDetailDto>> future : threadFutures) {

            try {
                allResults.addAll(future.get(30, TimeUnit.SECONDS));
            } catch (Exception e) {
                log.error("Failed to get results from thread", e);
            }

        }

        executor.shutdown();

        // Анализируем результаты
        return analyzeResults(documentId, allResults);
        // !A1 Запускаем параллельные попытки
        */


        return null;
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
            return null /** Написать далее реализацию ответа - Статус документа не соответствует требуемому (SUBMITTED) */ ;
        }

        log.debug("endMethod, к возврату document: {}", document);
        return document;
    }


    private ConcurrentStatus getConcurrentApproveStatus(OperationStatus documentOperationStatus) {
        log.debug("startMethod, к возврату OperationStatus documentOperationStatus: {}", documentOperationStatus);

        switch (documentOperationStatus) {

            case SUCCESS: return ConcurrentStatus.SUCCESS;

            case CONFLICT: return ConcurrentStatus.CONFLICT;

            default: return ConcurrentStatus.ERROR;

        }

    }


    private String mapResultStatus(String apiStatus) {

        switch (apiStatus) {
            case "SUCCESS": return "SUCCESS";
            case "CONFLICT": return "CONFLICT";
            default: return "ERROR";
        }

    }





//    @Override
//    @Transactional
//    public CompletableFuture<List<DocumentSubmitResponseDto>> parallelApproveOne(Long id, int threads, int attempts) {
//        log.info("startMethod, получен id: {}, threads: {}, attempts: {}", id, threads, attempts);
//
//        ExecutorService executorService = Executors.newFixedThreadPool(threads);
//        List<Long> idList = new ArrayList<>();
//
//        for (int i = 0; i < attempts; i++) idList.add(id);
//
//        return CompletableFuture.supplyAsync(() -> approve(idList), executorService);
//    }
//
//
//    @Override
//    @Transactional
//    public List<DocumentSubmitResponseDto> approve(List<Long> ids) {
//        List<DocumentSubmitResponseDto> documentSubmitResponseDtoList = new ArrayList<>();
//        for (Long id: ids){
//            if (documentRepository.findById(id).isPresent()){
//                if (documentRepository.findById(id).get().getStatus() != Status.SUBMITTED){
//                    documentSubmitResponseDtoList.add(new DocumentSubmitResponseDto(id, OperationStatus.CONFLICT));
//                } else {
//                    try {
//                        save(documentRepository.findById(id).get());
//                        documentSubmitResponseDtoList.add(new DocumentSubmitResponseDto(id, OperationStatus.SUCCESS));
//                    } catch (Exception e){
//                        documentSubmitResponseDtoList.add(new DocumentSubmitResponseDto(id, OperationStatus.REGISTER_ERROR));
//                    }
//                }
//            } else {
//                documentSubmitResponseDtoList.add(new DocumentSubmitResponseDto(id, OperationStatus.NOT_FOUND));
//            }
//        }
//        return documentSubmitResponseDtoList;
//    }
//
//
//    @Async
//    @Transactional
//    @Override
//    public CompletableFuture<List<DocumentSubmitResponseDto>> parallelApproveTwo(Long id){
//        return CompletableFuture.completedFuture(approve(List.of(id)));
//    }
//
//
//    @Override
//    @Transactional
//    public Register save(Document document) {
//
//        Register register = Register.builder()
//                .document(document)
////                .status(Status.APPROVED)
//                .build();
//        registerRepository.save(register);
//
//        History history = History.builder()
//                .date(ZonedDateTime.now())
//                .action(Action.APPROVE)
//                .author(register.getDocument().getAuthor())
//                .document(register.getDocument())
//                .build();
//        historyRepository.save(history);
//
//        document.setStatus(Status.APPROVED);
//
//        Set<History> historySet = new HashSet<>();
//        historySet.add(history);
//        //document.getHistorySet().clear();
//        document.getHistorySet().addAll(historySet);
////        for (History history1 : historySet) {
////            history1.setDocument(document);
////        }
//        document.setRegister(register);
//
//        documentRepository.save(document);
//        return register;
//    }
//
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
