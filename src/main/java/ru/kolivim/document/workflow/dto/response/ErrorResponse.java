package ru.kolivim.document.workflow.dto.response;

import com.fasterxml.jackson.annotation.JsonInclude;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Builder;
import lombok.Data;

import java.time.ZonedDateTime;
import java.util.Map;

@Data
@Builder
@Schema(description = "Единый формат ошибки")
@JsonInclude(JsonInclude.Include.NON_NULL)
public class ErrorResponse {

    @Schema(description = "Код ошибки", example = "NOT_FOUND")
    private String code;

    @Schema(description = "Сообщение об ошибке", example = "Документ не найден")
    private String message;

    @Schema(description = "HTTP статус", example = "404")
    private int status;

    @Schema(description = "Время возникновения ошибки")
    private ZonedDateTime timestamp;

    @Schema(description = "Путь запроса", example = "/api/v1/document/999")
    private String path;

    @Schema(description = "Детали ошибки (для validation ошибок)")
    private Map<String, String> errors;

}
