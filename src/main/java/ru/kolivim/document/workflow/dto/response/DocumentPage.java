package ru.kolivim.document.workflow.dto.response;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.*;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;

import java.util.List;

@Getter
@Setter
@ToString
@EqualsAndHashCode(callSuper = false)
public class DocumentPage<T> extends PageImpl<T> {

    @Schema(description = "Список Id, не найденных в БД")
    private List<Long> notFoundIds;

    @Schema(description = "Количество Id, не найденных в БД")
    private int notFoundCount;

    @Schema(description = "Общее количество Id, полученных для поиска документов")
    private int totalCount;


    public DocumentPage(List<T> content, Pageable pageable, long total,
                        List<Long> notFoundIds, int notFoundCount, int totalCount) {
        super(content, pageable, total);
        this.notFoundIds = notFoundIds;
        this.notFoundCount = notFoundCount;
        this.totalCount = totalCount;
    }


}
