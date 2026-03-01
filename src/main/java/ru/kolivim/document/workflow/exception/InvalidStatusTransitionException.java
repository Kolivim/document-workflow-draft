package ru.kolivim.document.workflow.exception;

import lombok.Getter;
import ru.kolivim.document.workflow.entity.enums.Status;

@Getter
public class InvalidStatusTransitionException extends RuntimeException {

    private final Long documentId;

    private final Status currentStatus;

    private final Status attemptedStatus;


    public InvalidStatusTransitionException(Long documentId, Status currentStatus, Status attemptedStatus) {
        super(String.format("Недопустимый переход статуса для документа %d: %s -> %s",
                documentId, currentStatus, attemptedStatus));
        this.documentId = documentId;
        this.currentStatus = currentStatus;
        this.attemptedStatus = attemptedStatus;
    }

}
