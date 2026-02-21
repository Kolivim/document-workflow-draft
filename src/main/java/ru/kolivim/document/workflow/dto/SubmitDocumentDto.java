package ru.kolivim.document.workflow.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import ru.kolivim.document.workflow.entity.enums.OperationStatus;
import lombok.*;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "Документ отправленный на согласование/утверждение", type = "object")
public class SubmitDocumentDto {

    @Schema(description = "id")
    private Long id;

    @Schema(description = "operationStatus")
    private OperationStatus operationStatus;

}
