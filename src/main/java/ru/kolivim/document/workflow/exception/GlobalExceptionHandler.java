package ru.kolivim.document.workflow.exception;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.converter.HttpMessageNotReadableException;
import ru.kolivim.document.workflow.dto.response.ErrorResponse;
import ru.kolivim.document.workflow.exception.ResourceNotFoundException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;
import jakarta.validation.ConstraintViolationException;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.context.request.WebRequest;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;

import java.time.ZonedDateTime;
import java.util.HashMap;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

@Slf4j
@ControllerAdvice
public class GlobalExceptionHandler {

    /** 404 - Resource Not Found */
    @ExceptionHandler(ResourceNotFoundException.class)
    public ResponseEntity<ErrorResponse> handleNotFound(ResourceNotFoundException ex, WebRequest request) {
        log.error("Не найдено: {}", ex.getMessage());

        return ResponseEntity.status(HttpStatus.NOT_FOUND)
                .body(ErrorResponse.builder()
                        .code("NOT_FOUND")
                        .message(ex.getMessage())
                        .status(HttpStatus.NOT_FOUND.value())
                        .timestamp(ZonedDateTime.now())
                        .path(request.getDescription(false).replace("uri=", ""))
                        .build());

    }


    /** 409 - Conflict для недопустимых переходов статуса */
    @ExceptionHandler(InvalidStatusTransitionException.class)
    public ResponseEntity<ErrorResponse> handleInvalidStatusTransition(InvalidStatusTransitionException ex, WebRequest request) {
        log.error("Недопустимый переход статуса: {}", ex.getMessage());

        ErrorResponse error = ErrorResponse.builder()
                .code("INVALID_STATUS_TRANSITION")
                .message(ex.getMessage())
                .status(HttpStatus.CONFLICT.value())
                .timestamp(ZonedDateTime.now())
                .path(request.getDescription(false).replace("uri=", ""))
                .build();

        return new ResponseEntity<>(error, HttpStatus.CONFLICT);
    }


    /** 409 - Conflict для ошибок регистрации в реестре */
    @ExceptionHandler(RegisterSaveException.class)
    public ResponseEntity<ErrorResponse> handleRegisterSave(RegisterSaveException ex, WebRequest request) {
        log.error("Ошибка сохранения в реестре документа {}: {}", ex.getDocumentId(), ex.getMessage());

        ErrorResponse error = ErrorResponse.builder()
                .code("REGISTER_SAVE_ERROR")
                .message(ex.getMessage())
                .status(HttpStatus.CONFLICT.value())
                .timestamp(ZonedDateTime.now())
                .path(request.getDescription(false).replace("uri=", ""))
                .build();

        return new ResponseEntity<>(error, HttpStatus.CONFLICT);
    }


    /** 400 - Validation ошибки при @Valid */
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ErrorResponse> handleValidationExceptions(MethodArgumentNotValidException ex, WebRequest request) {
        log.error("Ошибка валидации входных данных: {}", ex.getMessage());

        Map<String, String> errors = new HashMap<>();
        ex.getBindingResult().getAllErrors().forEach((error) -> {
            String fieldName = ((FieldError) error).getField();
            String errorMessage = error.getDefaultMessage();
            errors.put(fieldName, errorMessage);
        });

        ErrorResponse error = ErrorResponse.builder()
                .code("VALIDATION_ERROR")
                .message("Ошибка валидации входных данных")
                .errors(errors)
                .status(HttpStatus.BAD_REQUEST.value())
                .timestamp(ZonedDateTime.now())
                .path(request.getDescription(false).replace("uri=", ""))
                .build();

        return new ResponseEntity<>(error, HttpStatus.BAD_REQUEST);
    }


    /** 400 - Нарушение ограничений */
    @ExceptionHandler(ConstraintViolationException.class)
    public ResponseEntity<ErrorResponse> handleConstraintViolation(ConstraintViolationException ex, WebRequest request) {
        log.error("Нарушение ограничений: {}", ex.getMessage());

        Map<String, String> errors = ex.getConstraintViolations().stream()
                .collect(Collectors.toMap(
                        v -> v.getPropertyPath().toString(),
                        v -> v.getMessage(),
                        (existing, replacement) -> existing
                ));

        ErrorResponse error = ErrorResponse.builder()
                .code("CONSTRAINT_VIOLATION")
                .message("Ошибка валидации")
                .errors(errors)
                .status(HttpStatus.BAD_REQUEST.value())
                .timestamp(ZonedDateTime.now())
                .path(request.getDescription(false).replace("uri=", ""))
                .build();

        return new ResponseEntity<>(error, HttpStatus.BAD_REQUEST);
    }


    /** Обработка ошибок целостности данных */
    @ExceptionHandler(DataIntegrityViolationException.class)
    public ResponseEntity<ErrorResponse> handleDataIntegrityViolation(DataIntegrityViolationException ex,
                                                                      WebRequest request) {
        log.error("Нарушение целостности данных: {}", ex.getMessage());

        String errorMessage = ex.getMessage();
        String path = request.getDescription(false).replace("uri=", "");

        /** Дубликат inner_id -> 409 Conflict - Не выводим подробности ошибки */
        if (errorMessage.contains("unique constraint") && errorMessage.contains("documents_inner_id_key")) {

            String innerId = extractInnerIdFromException(errorMessage);
            String message = innerId != null
                    ? "Документ с внутренним ID '" + innerId + "' уже существует"
                    : "Документ с таким внутренним ID уже существует";

            ErrorResponse error = ErrorResponse.builder()
                    .code("DUPLICATE_INNER_ID")
                    .message(message)
                    .status(HttpStatus.CONFLICT.value())
                    .timestamp(ZonedDateTime.now())
                    .path(path)
                    .build();

            return new ResponseEntity<>(error, HttpStatus.CONFLICT);
        }


        /** NOT NULL violation -> 400 Bad Request */
        if (errorMessage.contains("not-null constraint")) {
            String fieldName = extractFieldName(errorMessage);
            String message = "Поле '" + fieldName + "' не может быть пустым";

            ErrorResponse error = ErrorResponse.builder()
                    .code("REQUIRED_FIELD_MISSING")
                    .message(message)
                    .status(HttpStatus.BAD_REQUEST.value())
                    .timestamp(ZonedDateTime.now())
                    .path(path)
                    .build();

            return new ResponseEntity<>(error, HttpStatus.BAD_REQUEST);
        }


        /** Другие ошибки -> 400 Bad Request */
        ErrorResponse error = ErrorResponse.builder()
                .code("DATA_INTEGRITY_VIOLATION")
                .message("Ошибка целостности данных: ".concat(extractConstraintDetails(errorMessage)) /* + extractConstraintDetails(errorMessage) */ )
                .status(HttpStatus.BAD_REQUEST.value())
                .timestamp(ZonedDateTime.now())
                .path(path)
                .build();

        return new ResponseEntity<>(error, HttpStatus.BAD_REQUEST);
    }


    /** 400 - Type mismatch (строка вместо числа и т.д.) */
    @ExceptionHandler(MethodArgumentTypeMismatchException.class)
    public ResponseEntity<ErrorResponse> handleTypeMismatch(MethodArgumentTypeMismatchException ex, WebRequest request) {
        log.error("startMethod, несоответствие типа параметра: {}", ex.getMessage());

        String message = String.format("Неверный формат параметра '%s'. Ожидается тип: %s",
                ex.getName(), ex.getRequiredType() != null ? ex.getRequiredType().getSimpleName() : "unknown");

        ErrorResponse error = ErrorResponse.builder()
                .code("TYPE_MISMATCH")
                .message(message)
                .status(HttpStatus.BAD_REQUEST.value())
                .timestamp(ZonedDateTime.now())
                .path(request.getDescription(false).replace("uri=", ""))
                .build();

        return new ResponseEntity<>(error, HttpStatus.BAD_REQUEST);
    }


    @ExceptionHandler(HttpMessageNotReadableException.class)
    public ResponseEntity<ErrorResponse> handleHttpMessageNotReadable(HttpMessageNotReadableException ex,
                                                                      WebRequest request) {
        log.error("Ошибка парсинга полученного JSON: {}", ex.getMessage());

        String message = "Неверный формат данных в запросе";

        if (ex.getMessage().contains("not one of the values accepted for Enum class")) {
            message = "Недопустимое значение статуса. Допустимые значения: DRAFT, SUBMITTED, APPROVED";
        }

        ErrorResponse error = ErrorResponse.builder()
                .code("INVALID_REQUEST_FORMAT")
                .message(message)
                .status(HttpStatus.BAD_REQUEST.value())
                .timestamp(ZonedDateTime.now())
                .path(request.getDescription(false).replace("uri=", ""))
                .build();

        return new ResponseEntity<>(error, HttpStatus.BAD_REQUEST);
    }


    /** 500 - Все остальные ошибки */
    @ExceptionHandler(Exception.class)
    public ResponseEntity<ErrorResponse> handleAllUncaught(Exception ex, WebRequest request) {
        log.error("Непредвиденная ошибка", ex);

        ErrorResponse error = ErrorResponse.builder()
                .code("INTERNAL_SERVER_ERROR")
                .message("Внутренняя ошибка сервера: " + ex.getMessage())
                .status(HttpStatus.INTERNAL_SERVER_ERROR.value())
                .timestamp(ZonedDateTime.now())
                .path(request.getDescription(false).replace("uri=", ""))
                .build();

        return new ResponseEntity<>(error, HttpStatus.INTERNAL_SERVER_ERROR);
    }


    private String extractConstraintDetails(String message) {

        Pattern pattern = Pattern.compile("Подробности:\\s*(.*?)(?=\n|$)");
        Matcher matcher = pattern.matcher(message);
        if (matcher.find()) {
            return matcher.group(1).trim();
        }


        pattern = Pattern.compile("Detail:\\s*(.*?)(?=\n|$)");
        matcher = pattern.matcher(message);
        if (matcher.find()) {
            return matcher.group(1).trim();
        }

        return null;
    }

    private String extractInnerIdFromException(String message) {

        Pattern pattern = Pattern.compile("Key \\(inner_id\\)=\\(([^)]+)\\)");
        Matcher matcher = pattern.matcher(message);
        if (matcher.find()) {
            return matcher.group(1);
        }


        pattern = Pattern.compile("Key \\(inner_id\\)=\\(([^)]+)\\)");
        matcher = pattern.matcher(message);
        if (matcher.find()) {
            return matcher.group(1);
        }

        return null;
    }

    private String extractFieldName(String message) {

        Pattern pattern = Pattern.compile("column\\s+\"([^\"]+)\"");
        Matcher matcher = pattern.matcher(message);
        if (matcher.find()) {
            return matcher.group(1);
        }

        return "unknown";
    }

}
