package ru.kolivim.document.workflow.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import ru.kolivim.document.workflow.entity.enums.Status;
import jakarta.validation.constraints.NotEmpty;
import lombok.*;

import java.time.ZonedDateTime;
import java.util.Set;

@Setter
@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "Документ", type = "object")
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

    @Schema(description = "title")
    @NotEmpty
    private String title;

    @Schema(description = "status")
    @NotEmpty
    private Status status;

    @Schema(description = "create_time")
    @NotEmpty
    private ZonedDateTime createTime;

    @Schema(description = "update_time")
    @NotEmpty
    private ZonedDateTime updateTime;

    @Schema(description = "historySet")
    private Set<HistoryDto> historySet;

    @Schema(description = "register")
    private RegisterDto register;
}
