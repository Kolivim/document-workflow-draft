package ru.kolivim.document.workflow.mappers;

import java.util.LinkedHashSet;
import java.util.Set;
import javax.annotation.processing.Generated;
import org.springframework.stereotype.Component;
import ru.kolivim.document.workflow.dto.HistoryDto;
import ru.kolivim.document.workflow.entity.History;

@Generated(
    value = "org.mapstruct.ap.MappingProcessor",
    date = "2026-02-19T21:42:39+0300",
    comments = "version: 1.6.3, compiler: javac, environment: Java 22.0.2 (Amazon.com Inc.)"
)
@Component
public class HistoryMapperImpl implements HistoryMapper {

    @Override
    public History dtoToEntity(HistoryDto historyDto) {
        if ( historyDto == null ) {
            return null;
        }

        History.HistoryBuilder history = History.builder();

        history.id( historyDto.getId() );
        history.author( historyDto.getAuthor() );
        history.time( historyDto.getTime() );
        history.action( historyDto.getAction() );
        history.comment( historyDto.getComment() );

        return history.build();
    }

    @Override
    public HistoryDto entityToDto(History history) {
        if ( history == null ) {
            return null;
        }

        HistoryDto.HistoryDtoBuilder historyDto = HistoryDto.builder();

        historyDto.id( history.getId() );
        historyDto.author( history.getAuthor() );
        historyDto.time( history.getTime() );
        historyDto.action( history.getAction() );
        historyDto.comment( history.getComment() );

        return historyDto.build();
    }

    @Override
    public Set<History> dtosToEntities(Set<HistoryDto> historyDtos) {
        if ( historyDtos == null ) {
            return null;
        }

        Set<History> set = new LinkedHashSet<History>( Math.max( (int) ( historyDtos.size() / .75f ) + 1, 16 ) );
        for ( HistoryDto historyDto : historyDtos ) {
            set.add( dtoToEntity( historyDto ) );
        }

        return set;
    }

    @Override
    public Set<HistoryDto> entitiesToDtos(Set<History> historyList) {
        if ( historyList == null ) {
            return null;
        }

        Set<HistoryDto> set = new LinkedHashSet<HistoryDto>( Math.max( (int) ( historyList.size() / .75f ) + 1, 16 ) );
        for ( History history : historyList ) {
            set.add( entityToDto( history ) );
        }

        return set;
    }
}
