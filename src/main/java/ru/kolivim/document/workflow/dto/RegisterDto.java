package ru.kolivim.document.workflow.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import ru.kolivim.document.workflow.entity.enums.Status;
import jakarta.validation.constraints.NotEmpty;
import lombok.*;

@Data
@Schema(description = "Переводы документов в статус APPROVE", type = "object")
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class RegisterDto {

    @Schema(description = "id")
    @NotEmpty
    private Long id;

    @Schema(description = "status")
    private Status status = Status.APPROVED;

    @Schema(description = "document")
    private DocumentDto document;

}
