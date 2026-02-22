package ru.kolivim.document.workflow.dto;

import io.swagger.v3.oas.annotations.media.Schema;
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
    @Size(min = 1 /*, max = 50 */ , message = "Список должен содержать хотя бы один Id" /* "Количество ID должно быть от 1 до 50" */ )
    private List<@Min(1) Long> ids;

}
