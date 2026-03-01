package ru.kolivim.document.workflow.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.experimental.SuperBuilder;
import ru.kolivim.document.workflow.entity.enums.Status;
import jakarta.validation.constraints.NotEmpty;
import lombok.*;

import java.time.ZonedDateTime;
import java.util.Set;

@Data
@SuperBuilder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "Документ")
public class DocumentDto {

    public interface Create {}

    public interface Search {}


    @Schema(description = "id документа (генерируется автоматически, игнорируется при создании, не указывать",
            accessMode = Schema.AccessMode.READ_ONLY)
    private Long id;

    @Schema(description = "Внутренний id документа, задаётся пользователем при создании, уникальный",
            requiredMode = Schema.RequiredMode.REQUIRED)
    @NotEmpty(groups = {Create.class})
    private String innerId;

    @NotBlank(message = "Автор не может быть пустым")
    @Size(min = 1, max = 255, message = "Имя автора должно содержать от 1 до 255 символов", groups = {Create.class})
    @Schema(description = "Автор документа", example = "Костючков Б.Е.", requiredMode = Schema.RequiredMode.REQUIRED)
    @NotEmpty(groups = {Create.class})
    private String author;

    @Schema(description = "name")
    @NotEmpty(groups = {Create.class})
    private String name;

    @Schema(description = "Статус документа" , accessMode = Schema.AccessMode.READ_ONLY)
    private Status status;

    @JsonProperty(access = JsonProperty.Access.READ_ONLY)
    @Schema(description = "Дата создания", accessMode = Schema.AccessMode.READ_ONLY)
    private ZonedDateTime createDate;

    @JsonProperty(access = JsonProperty.Access.READ_ONLY)
    @Schema(description = "Дата обновления", accessMode = Schema.AccessMode.READ_ONLY)
    private ZonedDateTime updateDate;

    @JsonProperty(access = JsonProperty.Access.READ_ONLY)
    @Schema(description = "История изменений", accessMode = Schema.AccessMode.READ_ONLY)
    private Set<HistoryDto> historySet;

    @JsonProperty(access = JsonProperty.Access.READ_ONLY)
    @Schema(description = "Запись в реестре", accessMode = Schema.AccessMode.READ_ONLY)
    private RegisterDto register;

}
