package ru.kolivim.document.workflow.dto.response;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.domain.Page;

import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = """
                Для возврата документа/документов, по переданному списку id документов, 
                а также сообщений при наличии ошибок""", type = "object")
public class PageResponseDto {

    private Page page;

    @Schema(description = "Список Id, не найденных в БД")
    private List<Long> notFoundIds;

    @Schema(description = "Количество Id, не найденных в БД")
    private int notFoundCount;

    @Schema(description = "Общее количество Id, полученных для поиска Документов")
    private int totalRequested;


    public boolean hasNotFound() {return notFoundIds != null && !notFoundIds.isEmpty();}

}
