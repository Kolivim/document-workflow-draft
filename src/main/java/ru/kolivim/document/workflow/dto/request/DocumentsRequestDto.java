package ru.kolivim.document.workflow.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.*;

import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "Для поиска документа/документов, по переданному списку id документов", type = "object")
public class DocumentsRequestDto {

//    @NotNull
    @Size(min = 1, max = 1000, message = "Список должен содержать от 1 до 1000 Id")
    private List<@Min(1) @Max(1000) Long> ids;

    @Schema(description = "Лицо, которое отправило документ на утверждение")
    private String author;

    @Schema(description = "Комментарий")
    private String comment;

}
