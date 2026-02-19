package ru.kolivim.document.workflow.mappers;

import ru.kolivim.document.workflow.dto.HistoryDto;
import ru.kolivim.document.workflow.entity.History;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

import java.util.Set;

@Mapper
public interface HistoryMapper {
    @Mapping(target = "document", ignore = true)
    History dtoToEntity(HistoryDto historyDto);

    @Mapping(target = "document", ignore = true)
    HistoryDto entityToDto(History history);

    Set<History> dtosToEntities(Set<HistoryDto> historyDtos);

    Set<HistoryDto> entitiesToDtos(Set<History> historyList);
}
