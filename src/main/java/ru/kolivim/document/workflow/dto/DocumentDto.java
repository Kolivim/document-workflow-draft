package ru.kolivim.document.workflow.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.experimental.SuperBuilder;
import ru.kolivim.document.workflow.entity.enums.Status;
import jakarta.validation.constraints.NotEmpty;
import lombok.*;

import java.time.ZonedDateTime;
import java.util.Set;

@Data
@Schema(description = "Документ", type = "object")
@SuperBuilder
@NoArgsConstructor
@AllArgsConstructor
public class DocumentDto {

    @Schema(description = "id, игнорируется при создании")
    private Long id;

    @Schema(description = "inner_id")
    @NotEmpty
    private String innerId;

    @Schema(description = "author")
    @NotEmpty
    private String author;

    @Schema(description = "name")
    @NotEmpty
    private String name;

    @Schema(description = "description, игнорируется при создании")
    private String description;

    @Schema(description = "status, игнорируется при создании")
    private Status status;

    @Schema(description = "create_date, игнорируется при создании")
    private ZonedDateTime createDate;

    @Schema(description = "update_date, игнорируется при создании")
    private ZonedDateTime updateDate;


    @Schema(description = "historySet, игнорируется при создании")
    private Set<HistoryDto> historySet;

    @Schema(description = "register, игнорируется при создании")
    private RegisterDto register;

}
