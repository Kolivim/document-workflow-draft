package ru.kolivim.document.workflow.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import lombok.Builder;
import lombok.Data;

@Data
@Schema(description = "Запрос на тестирование конкурентного утверждения")
@Builder
public class ConcurrentApproveRequest {



    @Min(value = 1, message = "ID документа должен быть не меньше 1")
    @Schema(description = "Идентификатор документа для конкурентного утверждения",
            example = "1", requiredMode = Schema.RequiredMode.REQUIRED)
    @NotNull(message = "Id документа обязателен")
    @Positive(message = "ID документа должен быть положительным числом")
    private Long documentId;

    @Min(value = 1, message = "Количество потоков должно быть не менее 1")
    @Schema(description = "Количество параллельных потоков", example = "2", requiredMode = Schema.RequiredMode.REQUIRED)
    @NotNull(message = "Количество потоков обязательно")
    private Integer threads;

    @Min(value = 1, message = "Количество попыток должно быть не менее 1")
    @Schema(description = "Количество попыток в каждом потоке",
            example = "5", requiredMode = Schema.RequiredMode.REQUIRED)
    @NotNull(message = "Количество попыток обязательно")
    private Integer attempts;

}
