package ru.kolivim.document.workflow.config;

import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.constraints.Min;
import lombok.Data;

@Data
@Schema(
        name = "SubmitConfig",
        description = "Конфигурация для отправки документов в рабочий процесс",
        title = "Настройки отправки"
)
public class SubmitConfig {

    @Min(value = 1000, message = "Задержка должна быть не менее 1000 мс (1 секунда)")
    @Schema(description = "Фиксированная задержка между задачами (в миллисекундах)", type = "integer")
    private long fixedDelay;

}
