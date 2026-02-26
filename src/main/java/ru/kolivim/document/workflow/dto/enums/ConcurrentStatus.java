package ru.kolivim.document.workflow.dto.enums;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "Статус операции при конкурентной процедуре утверждения",
        allowableValues = {"SUCCESS", "CONFLICT", "ERROR"})
public enum ConcurrentStatus {

    @Schema(description = "Операция выполнена успешно")
    SUCCESS,

    @Schema(description = "Обнаружен конфликт при выполнении операции")
    CONFLICT,

    @Schema(description = "Произошла ошибка при выполнении операции")
    ERROR

}
