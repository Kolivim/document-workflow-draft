package ru.kolivim.document.workflow.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Builder;
import lombok.Data;

@Data
@Builder
@Schema(description = "Попытки")
public class AttemptDetailDto {

    @Schema(description = "Номер потока")
    private int threadNumber;

    @Schema(description = "Номер попытки")
    private int attemptNumber;

    @Schema(description = "Успешно ли выполнена")
    private Boolean isSuccess;

    @Schema(description = "Сообщение")
    private String message;

    @Schema(description = "Id документа")
    private Long id;

}
