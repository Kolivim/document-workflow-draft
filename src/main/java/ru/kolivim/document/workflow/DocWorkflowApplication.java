package ru.kolivim.document.workflow;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.validation.annotation.Validated;


@Validated
@EnableAsync
@EnableScheduling
@SpringBootApplication
public class DocWorkflowApplication {

    public static void main(String[] args) {
        SpringApplication.run(DocWorkflowApplication.class, args);
    }

}