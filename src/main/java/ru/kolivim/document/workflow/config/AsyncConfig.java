package ru.kolivim.document.workflow.config;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;
import org.springframework.core.task.TaskExecutor;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;

@Slf4j
@Schema(
        name = "AsyncConfig",
        description = "Конфигурация асинхронной обработки и планировщика задач",
        title = "Настройки асинхронного выполнения"
)
@EnableAsync
@Configuration
@EnableScheduling
public class AsyncConfig {

    @Bean(name = "taskExecutor")
    @Schema(
            description = "Создает и настраивает исполнителя задач для асинхронной обработки документов",
            implementation = ThreadPoolTaskExecutor.class
    )
    public TaskExecutor taskExecutor() {
        log.debug("Инициализация пула потоков для асинхронной обработки документов");

        ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
        executor.setCorePoolSize(2);
        executor.setMaxPoolSize(4);
        executor.setQueueCapacity(100);
        executor.setThreadNamePrefix("doc-worker-");
        executor.initialize();

        return executor;
    }

}
