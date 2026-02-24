package ru.kolivim.document.workflow.dto.response;

import com.fasterxml.jackson.annotation.JsonInclude;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import ru.kolivim.document.workflow.dto.DocumentDto;

import java.util.List;
import java.util.Map;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "Для возврата документа/документов, по переданному списку id документов, а также сообщений при наличии ошибок", type = "object")
public class DocumentsResponseDto {

    @Schema(description = "Обрработанные документы")
    private List<DocumentDto> documents;

    @Schema(description = "Не найденные Id")
    private List<Long> /* Map<Long, String> */ notFoundIds;

    @Schema(description = "Общее направленное на обработку количество документов")
    private int totalRequested;

    @Schema(description = "Общее количество найденных документов")
    private int totalFound;

    @Schema(description = "Общее количество не найденных документов")
    private int totalNotFound;

    @Schema(description = "Успех обработки списка Id")
    private boolean success;

    @Schema(description = "Сообщение")
    private String message;

}
