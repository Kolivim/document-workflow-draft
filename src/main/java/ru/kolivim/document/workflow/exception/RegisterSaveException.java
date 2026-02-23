package ru.kolivim.document.workflow.exception;

import lombok.Getter;
import lombok.ToString;

/**
 * Исключение, выбрасываемое при ошибках сохранения записи в реестре документов.
 * Возникает, когда не удаётся создать запись в таблице register для утверждённого документа.
 */
@Getter
@ToString
public class RegisterSaveException extends RuntimeException {

    private final Long documentId;

    public RegisterSaveException(String message, Long documentId) {
        super(message);
        this.documentId = documentId;
    }

    public RegisterSaveException(String message, Throwable cause, Long documentId) {
        super(message, cause);
        this.documentId = documentId;
    }

}