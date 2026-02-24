package ru.kolivim.document.workflow.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotEmpty;
import lombok.*;
import lombok.experimental.SuperBuilder;
import ru.kolivim.document.workflow.entity.enums.Status;

import java.time.ZonedDateTime;
import java.util.Set;

@Data
@SuperBuilder
@EqualsAndHashCode(callSuper = true)
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "Для поиска документа/документов", type = "object")
public class SearchDocumentDto extends DocumentDto {

    @Schema(description = "Дата создания, позднее которой будут отобраны документы")
    private ZonedDateTime startDate;

    @Schema(description = "Дата создания, ранее которой будут отобраны документы")
    private ZonedDateTime endDate;


    public SearchDocumentDto(DocumentDto documentDto, ZonedDateTime startDate, ZonedDateTime endDate) {

        super.setId(documentDto.getId());
        super.setInnerId(documentDto.getInnerId());
        super.setAuthor(documentDto.getAuthor());
        super.setName(documentDto.getName());
        super.setDescription(documentDto.getDescription());
        super.setStatus(documentDto.getStatus());
        super.setCreateDate(documentDto.getCreateDate());
        super.setUpdateDate(documentDto.getUpdateDate());
        super.setHistorySet(documentDto.getHistorySet());
        super.setRegister(documentDto.getRegister());

        this.startDate = startDate;
        this.endDate = endDate;

    }


    /*
    @Builder
    public SearchDocumentDto(Long id, String innerId, String author, String name, String description,
                             Status status, ZonedDateTime createDate, ZonedDateTime updateDate,
                             ZonedDateTime startDate, ZonedDateTime endDate) {

        super(id, innerId, author, name, description, status, createDate, updateDate, null , null );
        this.startDate = startDate;
        this.endDate = endDate;

    }
    */

}
