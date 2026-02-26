package ru.kolivim.document.workflow.dto.response;

import io.swagger.v3.oas.annotations.media.Schema;
import ru.kolivim.document.workflow.entity.enums.OperationStatus;
import lombok.*;

@Data
@Schema(description = "Статус документа после отправки на согласование/утверждение", type = "object")
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class DocumentSubmitResponseDto {

    @Schema(description = "Id документа")
    private Long id;

    @Schema(description = "operationStatus")
    private OperationStatus operationStatus;

}
