package ru.kolivim.document.workflow.scheduler;

import ru.kolivim.document.workflow.dto.DocumentDto;
import ru.kolivim.document.workflow.dto.SearchDocumentDto;
import ru.kolivim.document.workflow.entity.Document;
import ru.kolivim.document.workflow.service.DocumentService;
import jakarta.annotation.PostConstruct;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.ZonedDateTime;

@Slf4j
@Component
public class CreateScheduler {

    private final DocumentService documentService;

    @Value("${number}")
    Integer number;

    public CreateScheduler(DocumentService documentService) {
        this.documentService = documentService;
    }

    @PostConstruct
    public void init() {}

    @Scheduled(cron = "0 * * * * *")
    //@Transactional
    protected void create() {

        log.info("startMethod, получено количество документов для создания = {}", number);

        for (int i = 0; i < number; i++){


            //  Создаём тестовые ДТОшки :
            DocumentDto documentDto = new DocumentDto();
            documentDto.setAuthor("Автор номер ".concat(String.valueOf(i)));
            documentDto.setInnerId(ZonedDateTime.now().toInstant().toString().concat("_").concat(String.valueOf(i)));
            documentDto.setName("Документ номер ".concat(String.valueOf(i)));
                // SC
            //  !Создаём тестовые ДТОшки


            DocumentDto createDocumentDto = documentService.create(documentDto);
            log.info("Создан документ с id: {}, итого обработано {} документов, из общего количества документов к созданию = {}",
                    createDocumentDto.getId().toString(), i + 1, number);

        }

    }

}
