package ru.kolivim.document.workflow.util.generator;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

@Slf4j
@Order(100)
@Component
@RequiredArgsConstructor
public class DocumentGenerator implements CommandLineRunner {

    private final GeneratorService generatorService;

    private final GeneratorProperties generatorProperties;


    @Override
    public void run(String... args) throws Exception {


        if (!shouldRunGenerator(args)) {return;}

        log.info("startMethod, запуск утилиты массового создания документов");

        /** Применяем параметры, полученные из командной строки */
        applyCommandLineArgs(args);
        generatorService.updateTotalDocuments(generatorProperties.getTotalDocuments());

        generatorService.generate();

        if (isStandaloneRun(args)) {
            log.info("✅ Генерация завершена, выход из утилиты массового создания Документов");
            System.exit(0);
        }

    }


    private void applyCommandLineArgs(String[] args) {
        log.info("startMethod");

        for (String arg : args) {

            if (arg.startsWith("--number=")) {
                String value = arg.substring("--number=".length());
                generatorProperties.setNumber(Integer.parseInt(value));
                log.info("Переопределен параметр number = {}", value);
            } else if (arg.startsWith("--count=")) {
                String value = arg.substring("--count=".length());
                generatorProperties.setCount(Integer.parseInt(value));
                log.info("Переопределен параметр count = {}", value);
            } else if (arg.startsWith("--author=")) {
                String value = arg.substring("--author=".length());
                generatorProperties.setAuthor(value);
                log.info("Переопределен параметр author = {}", value);
            } else if (arg.startsWith("--api-url=")) {
                String value = arg.substring("--api-url=".length());
                generatorProperties.setApiUrl(value);
                log.info("Переопределен параметр api-url = {}", value);
            }

        }

        log.info("endMethod");
    }


    /** Проверяет, нужно ли запускать генератор */
    private boolean shouldRunGenerator(String[] args) {
        for (String arg : args) {
            if (arg.equals("--generate") || arg.equals("-g")) {
                return true;
            }
        }
        return System.getProperty("generator.run") != null;
    }


    /** Проверяет, запущен ли генератор отдельно */
    private boolean isStandaloneRun(String[] args) {
        for (String arg : args) {
            if (arg.equals("--standalone") || arg.equals("-s")) {
                return true;
            }
        }
        return false;
    }

}