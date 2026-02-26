package ru.kolivim.document.workflow.util.generator;

import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.boot.context.properties.ConfigurationProperties;
import ru.kolivim.document.workflow.entity.enums.Status;

import java.io.IOException;
import java.io.InputStream;
import java.util.Properties;

@Data
@Slf4j
@Component
@ConfigurationProperties(prefix = "generator")
public class GeneratorProperties {

    /** URL API для создания Документов */
    private String apiUrl;

    /** Автор документов */
    private String author;

    /** Статус создаваемых документов (всегда DRAFT) */
    private Status status;

    /** Количество создаваемых Документов, в 1 пачке */
    private int number;

    /** Количество создаваемых пачек Документов, за 1 запуск */
    private int count;


    /** Загружает параметры из файла по умолчанию */
    public void loadFromDefaultFile() throws IOException {
        log.info("startMethod");

        Properties props = new Properties();


        try (InputStream inputStream = getClass().getClassLoader()
                .getResourceAsStream("generator.properties")) {
            props.load(inputStream);
        }


        if (props.containsKey("api-url")) {
            this.apiUrl = props.getProperty("api-url");
        } else {
            throw new IllegalArgumentException("В файле конфигурации отсутствует обязательный параметр 'api-url'");
        }


        if (props.containsKey("number")) {
            this.number = Integer.parseInt(props.getProperty("number"));
        } else {
            throw new IllegalArgumentException("В файле конфигурации отсутствует обязательный параметр 'number'");
        }


        if (props.containsKey("count")) {
            this.count = Integer.parseInt(props.getProperty("count"));
        } else {
            throw new IllegalArgumentException("В файле конфигурации отсутствует обязательный параметр 'count'");
        }


        if (props.containsKey("author")) {
            this.author = props.getProperty("author");
        }


        /** Читает статус из файла */
        if (props.containsKey("status")) {

            try {

                this.status = Status.valueOf(props.getProperty("status").toUpperCase());
                log.info("Прочитан status: {}", status);

            } catch (IllegalArgumentException e) {

                log.error("Некорректное значение status: {}, применен статус DRAFT", props.getProperty("status"));
                this.status = Status.DRAFT;

            }

        }


        log.info("endMethod");
    }


    /** Возвращает общее количество документов для создания */
    public int getTotalDocuments() {return number * count;}


    private void printLoadedConfigParameters() {
        log.info("");
        log.info("╔══════════════════════════════════════════════════════════════╗");
        log.info("║         УТИЛИТА МАССОВОГО СОЗДАНИЯ ДОКУМЕНТОВ                ║");
        log.info("╚══════════════════════════════════════════════════════════════╝");
        log.info("\tПолучены следующие параметры генерации Документов:");
        log.info("\t📊 Количество пачек для создания: {} (по умолчанию = 1)", this.count);
        log.info("\t📦 Количество документов для создания, в 1 пачке: {}", this.number);
        log.info("\t👤 Автор: {}", this.author);
        log.info("\t📌 Статус: {}", this.status);
        log.info("\t🌐 API URL: {}\n", this.apiUrl);

    }


    /** Выводит в консоль пример конфигурационного файла */
    private void printDefaultConfig() {
        log.info("");
        log.info("\tПример generator.properties:");
        log.info("\t# Обязательные параметры");
        log.info("\tapi.url=http://localhost:8080/api/v1/document/create");
        log.info("\tnumber=50");
        log.info("\tcount=1");
        log.info("\t# Опциональные параметры");
        log.info("\tauthor=test-user");
        log.info("\tstatus=DRAFT\n");
    }

}
