package ru.kolivim.document.workflow.dto.request;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class DocumentBatchRequest {

    private List<Long> documentIds;

    private String action;

}
