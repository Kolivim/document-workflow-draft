package ru.kolivim.document.workflow.dto;

import ch.qos.logback.core.status.Status;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.*;

@Setter
@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "Документ отправленный на согласование/утверждение", type = "object")
public class ConcurrentApproveDto {

    @Schema(description = "id")
    private Long id;

    @Schema(description = "Сколько попыток прошло успешно")
    private int numberOfSuccessfulAttempts;

    @Schema(description = "Сколько попыток завершилось конфликтом/ошибкой")
    private int numberOfFailedAttempts;

    @Schema(description = "Финальный статус документа")
    private Status resultingDocumentStatus;

}
