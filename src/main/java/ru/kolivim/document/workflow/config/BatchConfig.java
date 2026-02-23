package ru.kolivim.document.workflow.config;

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
@Configuration
@ConfigurationProperties(prefix = "app.batch")
public class BatchConfig {

    @Min(value = 1, message = "Batch size must be at least 1")
    private int size = 100;

    @NotNull
    private SubmitConfig submit;

    @NotNull
    private ApproveConfig approve;

    @PostConstruct
    public void validate() {

        log.info("startMethod, size: {}, Submit delay: {} ms, Approve delay: {} ms",
                size,
                submit != null ? submit.getFixedDelay() : "not configured",
                approve != null ? approve.getFixedDelay() : "not configured"
                );

        if (size <= 0) {
            log.error("CRITICAL: Batch size is {}! Must be positive. Using default value 100.", size);
            this.size = 100;
        }

        if (submit == null || submit.getFixedDelay() <= 0) {
            log.debug("Submit delay not properly configured, using default 60000 ms");
            if (submit == null) {submit = new SubmitConfig();}

            if (submit.getFixedDelay() <= 0) {submit.setFixedDelay(60000);}
        }

        if (approve == null || approve.getFixedDelay() <= 0) {
            log.warn("Approve delay not properly configured, using default 120000 ms");
            if (approve == null) {approve = new ApproveConfig();}
            if (approve.getFixedDelay() <= 0) {approve.setFixedDelay(120000);}
        }

    }

}
