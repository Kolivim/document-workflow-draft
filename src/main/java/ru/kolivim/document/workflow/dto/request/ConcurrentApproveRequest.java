package ru.kolivim.document.workflow.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import lombok.Builder;
import lombok.Data;

@Data
@Schema(description = "Запрос на тестирование конкурентного утверждения")
@Builder
public class ConcurrentApproveRequest {

    @NotNull(message = "Id документа обязателен")
    @Schema(description = "Идентификатор документа для конкурентного утверждения", example = "1")
    private Long documentId;

    @Min(value = 1, message = "Количество потоков должно быть не менее 1")
    @Schema(description = "Количество параллельных потоков", example = "2")
    private int threads;

    @Min(value = 1, message = "Количество попыток должно быть не менее 1")
    @Schema(description = "Количество попыток в каждом потоке", example = "5")
    private int attempts;

}
