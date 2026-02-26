package ru.kolivim.document.workflow.dto.response;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@Schema(description = "Ответ при пакетной обработке списка документов")
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class DocumentBatchResponse {

    @Schema(description = "Все документы в пакете успешно обработаны", example = "true")
    private boolean success;

    @Schema(description = "Сообщение")
    private String message;

    @Schema(description = "Список Id успешно обработанных документов",
            example = "[1001, 1002, 1003, 1004, 1005]")
    private List<Long> processedIds;

    @Schema(description = "Список Id документов, обработка которых завершилась ошибкой",
            example = "[1006, 1007]")
    private List<Long> failedIds;

}
