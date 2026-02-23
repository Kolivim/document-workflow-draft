package ru.kolivim.document.workflow.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class DocumentBatchResponse {

    private boolean success;

    private String message;

    private List<Long> processedIds;

    private List<Long> failedIds;

}
