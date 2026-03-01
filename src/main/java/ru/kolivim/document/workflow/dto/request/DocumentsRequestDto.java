package ru.kolivim.document.workflow.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.*;

import java.util.List;

@Data
@Schema(description = "Для поиска документа/документов, по переданному списку id документов")
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class DocumentsRequestDto {

    @Size(min = 1, max = 1000, message = "Список должен содержать от 1 до 1000 Id")
    @Schema(description = "Список Id документов для поиска", example = "[1, 2, 3, 4, 5]")
    @NotNull
    private List<@Min(1) @Max(1000) Long> ids;

    @Schema(description = "Инициатор действия с документом")
    private String author;

    @Schema(description = "Комментарий")
    private String comment;

}
