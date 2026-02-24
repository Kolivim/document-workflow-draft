package ru.kolivim.document.workflow.util.generator;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.CommandLineRunner;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@Order(100)                                                                                                             /** Установлен низкий приоритет, чтобы не мешать основному приложению */
@RequiredArgsConstructor
public class DocumentGenerator implements CommandLineRunner {

    private final GeneratorService generatorService;


    @Override
    public void run(String... args) throws Exception {


        if (!shouldRunGenerator(args)) {return;}

        log.info("startMethod, запуск утилиты массового создания документов");


        generatorService.generate();

        if (isStandaloneRun(args)) {
            log.info("✅ Генерация завершена, выход из утилиты массового создания Документов");
            System.exit(0);
        }

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