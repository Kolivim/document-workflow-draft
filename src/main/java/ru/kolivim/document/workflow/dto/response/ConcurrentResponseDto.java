package ru.kolivim.document.workflow.dto.response;

import lombok.*;
import io.swagger.v3.oas.annotations.media.Schema;
import ru.kolivim.document.workflow.entity.enums.Status;

@Data
@Schema(description = "Результат тестирования конкурентного утверждения документа")
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ConcurrentResponseDto {

    @Schema(description = "Id документа", example = "1")
    private Long id;

    @Schema(description = "Финальный статус документа после всех попыток",
            example = "APPROVED", allowableValues = {"DRAFT", "SUBMITTED", "APPROVED"})
    private Status status;

    @Schema(description = "Количество успешных утверждений (ожидается 1)", example = "1", minimum = "0")
    private Integer countSuccessfulApprove;

    @Schema(description = "Количество попыток c конфликтом или ошибкой", example = "49", minimum = "0")
    private Integer countFailedApprove;

    @Schema(description = "Указывает, что переданный Id документа был протестирован в полном объёме", example = "true")
    private Boolean isSuccess;

}
