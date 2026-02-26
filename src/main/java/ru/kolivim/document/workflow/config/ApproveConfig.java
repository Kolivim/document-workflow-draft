package ru.kolivim.document.workflow.config;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

@Data
@Schema(
        name = "ApproveConfig",
        description = "Конфигурация для утверждения документов (статус APPROVE)",
        title = "Настройки утверждения документов"
)
public class ApproveConfig {

    @Schema(
            description = "Фиксированная задержка между утверждениями документов (в миллисекундах)",
            example = "120000", type = "integer"
    )
    private long fixedDelay;

}
