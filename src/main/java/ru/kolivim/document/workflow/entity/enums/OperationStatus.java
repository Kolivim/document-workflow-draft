package ru.kolivim.document.workflow.entity.enums;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "Статус выполнения операции с документом", enumAsRef = true)
public enum OperationStatus {

    @Schema(description = "Операция выполнена успешно")
    SUCCESS,

    @Schema(description = "Конфликт при выполнении операции (недопустимый статус или конкурентное изменение)")
    CONFLICT,

    @Schema(description = "Документ не найден")
    NOT_FOUND,

    @Schema(description = "Ошибка при сохранении в реестр утверждений")
    REGISTER_ERROR

}
