package ru.kolivim.document.workflow.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;
import java.util.Map;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "Для возврата документа/документов, по переданному списку id документов, а также сообщений при наличии ошибок", type = "object")
public class DocumentsResponseDto {

    private List<DocumentDto> documents;

    /** Не найденные Id */
    private Map<Long, String> notFoundIds;

    private int totalRequested;

    private int totalFound;

    private int totalNotFound;

//    private NotFoundMetadata notFound;     private List<Long> ids;           // Список не найденных ID     private Map<Long, String> reasons; // Причины для каждого ID (опциональн

//    @JsonInclude(JsonInclude.Include.NON_NULL)
//    private String warning;

}
