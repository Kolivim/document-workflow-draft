package ru.kolivim.document.workflow.dto.response;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.*;
import ru.kolivim.document.workflow.entity.enums.Status;

@Data
@Schema(description = "Документ отправленный на согласование/утверждение", type = "object")
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ConcurrentResponseDto {

    @Schema(description = "Id документа")
    private Long id;

    @Schema(description = "Финальный статус документа")
    private Status status;

    @Schema(description = "Количество успешных утверждений")
    private Integer countSuccessfulApprove;

    @Schema(description = "Количество попыток c конфликтом/ошибкой")
    private Integer countFailedApprove;

    @Schema(description = "Указывает, что переданный Id документа был протестирован в полном объёме")
    private Boolean isSuccess;

}
