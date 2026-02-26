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

    @Schema(description = "id")
    @NotEmpty
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

    @Schema(description = "description")
    private String description;

    @Schema(description = "status")
    @NotEmpty
    private Status status;

    @Schema(description = "create_date")
    @NotEmpty
    private ZonedDateTime createDate;

    @Schema(description = "update_date")
    private ZonedDateTime updateDate;


    @Schema(description = "historySet")
    private Set<HistoryDto> historySet;

    @Schema(description = "register")
    private RegisterDto register;

}
