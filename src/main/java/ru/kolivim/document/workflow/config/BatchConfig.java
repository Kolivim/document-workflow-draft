package ru.kolivim.document.workflow.config;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.annotation.PostConstruct;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

@Slf4j
@Getter
@Setter
@Schema(
        name = "BatchConfig",
        description = "Конфигурация пакетной обработки документов",
        title = "Настройки пакетной обработки"
)
@Configuration
@ConfigurationProperties(prefix = "app.batch")
public class BatchConfig {

    @Min(value = 1, message = "Размер пачки не может быть меньше 1")
    @Schema(description = "Размер пачки документов для пакетной обработки",
            example = "10", minimum = "1", defaultValue = "100")
    private int size = 100;

    @Schema(description = "Настройки для отправки документов (статус SUBMITTED)")
    @NotNull
    private SubmitConfig submit;

    @Schema(description = "Настройки для утверждения документов (статус APPROVE)")
    @NotNull
    private ApproveConfig approve;


    @PostConstruct
    public void validate() {

        log.info("startMethod, Размер пачки документов: {}, Submit delay: {} ms, Approve delay: {} ms",
                size,
                submit != null ? submit.getFixedDelay() : "not configured",
                approve != null ? approve.getFixedDelay() : "not configured"
                );

        if (size <= 0) {
            log.error("CRITICAL: Batch size is {}! Must be positive. Using default value 100", size);
            this.size = 100;
        }

        if (submit == null || submit.getFixedDelay() <= 0) {
            log.debug("Задержка отправки (SUBMITTED) не указана, используем по умолчанию 60000 ms");
            if (submit == null) {submit = new SubmitConfig();}

            if (submit.getFixedDelay() <= 0) {submit.setFixedDelay(60000);}
        }

        if (approve == null || approve.getFixedDelay() <= 0) {
            log.warn("Задержка утверждения (APPROVE) не указана, используем по умолчанию 120000 ms");
            if (approve == null) {approve = new ApproveConfig();}
            if (approve.getFixedDelay() <= 0) {approve.setFixedDelay(120000);}
        }

    }

}
