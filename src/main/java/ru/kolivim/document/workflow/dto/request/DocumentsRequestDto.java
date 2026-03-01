package ru.kolivim.document.workflow.dto.request;

import com.fasterxml.jackson.annotation.JsonProperty;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "Запрос на пакетную обработку документов, в т.ч. для поиска документов")
public class DocumentsRequestDto {

    public interface Submit {}

    public interface Approve {}

    public interface Create {}


    @NotEmpty(message = "Список ID документов не может быть пустым")
    @Size(min = 1, max = 1000, message = "Список должен содержать от 1 до 1000 Id")
    @Schema(description = "Список Id документов", example = "[1, 2, 3]", requiredMode = Schema.RequiredMode.REQUIRED)
    private List<@Min(1) @Max(1000) Long> ids;

    @NotBlank(message = "Инициатор не может быть пустым", groups = {Submit.class, Approve.class})
    @Size(min = 1, max = 255, message = "Имя инициатора должно содержать от 1 до 255 символов")
    @Schema(description = "Инициатор действия с документом", example = "Петров П.П.")
    private String author;

    @Schema(description = "Комментарий")
    private String comment;

}
