package ru.kolivim.document.workflow.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import org.springframework.data.domain.Page;

@Schema(description = "Обёртка для ответа с пагинацией")
public class ApiResponsePageDocumentDto {

    @Schema(description = "Успешность операции", example = "true")
    private boolean success;

    @Schema(description = "Сообщение", example = "Не найдено документов: 2")
    private String message;

    @Schema(description = "Данные с пагинацией")
    private Page<DocumentDto> data;

}
