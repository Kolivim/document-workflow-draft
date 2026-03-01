package ru.kolivim.document.workflow.entity.enums;
import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "Действие с документом", enumAsRef = true)
public enum Action {

    @Schema(description = "Отправка документа на согласование (переход DRAFT → SUBMITTED)")
    SUBMIT,

    @Schema(description = "Утверждение документа (переход SUBMITTED → APPROVED)")
    APPROVE

}
