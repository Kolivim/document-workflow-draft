package ru.kolivim.document.workflow.entity.enums;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "Статус документа в его жизненном цикле")
public enum Status {

    @Schema(description = "Черновик - документ создан")
    DRAFT,

    @Schema(description = "Отправлен на согласование")
    SUBMITTED,

    @Schema(description = "Утвержден - финальный статус, вносится запись в реестр")
    APPROVED

}
