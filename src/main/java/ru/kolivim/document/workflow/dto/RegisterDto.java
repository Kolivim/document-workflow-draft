package ru.kolivim.document.workflow.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import ru.kolivim.document.workflow.entity.enums.Status;
import jakarta.validation.constraints.NotEmpty;
import lombok.*;

@Setter
@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "Переводы документов в статус ЗАРЕГИСТРИРОВАН", type = "object")
public class RegisterDto {
    @Schema(description = "id")
    @NotEmpty
    private Long id;

    @Schema(description = "status")
    private Status status = Status.APPROVED;

    @Schema(description = "document")
    private DocumentDto document;
}
