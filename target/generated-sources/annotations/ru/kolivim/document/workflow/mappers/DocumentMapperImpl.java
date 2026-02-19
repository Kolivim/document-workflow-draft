package ru.kolivim.document.workflow.mappers;

import java.util.ArrayList;
import java.util.List;
import javax.annotation.processing.Generated;
import org.springframework.stereotype.Component;
import ru.kolivim.document.workflow.dto.DocumentDto;
import ru.kolivim.document.workflow.entity.Document;

@Generated(
    value = "org.mapstruct.ap.MappingProcessor",
    date = "2026-02-19T21:42:39+0300",
    comments = "version: 1.6.3, compiler: javac, environment: Java 22.0.2 (Amazon.com Inc.)"
)
@Component
public class DocumentMapperImpl implements DocumentMapper {

    @Override
    public Document dtoToEntity(DocumentDto documentDto) {
        if ( documentDto == null ) {
            return null;
        }

        Document.DocumentBuilder document = Document.builder();

        document.historySet( toEntityHistorySet( documentDto.getHistorySet() ) );
        document.register( toEntityRegister( documentDto.getRegister() ) );
        document.id( documentDto.getId() );
        document.innerId( documentDto.getInnerId() );
        document.author( documentDto.getAuthor() );
        document.title( documentDto.getTitle() );
        document.status( documentDto.getStatus() );
        document.createTime( documentDto.getCreateTime() );
        document.updateTime( documentDto.getUpdateTime() );

        return document.build();
    }

    @Override
    public DocumentDto entityToDto(Document document) {
        if ( document == null ) {
            return null;
        }

        DocumentDto.DocumentDtoBuilder documentDto = DocumentDto.builder();

        documentDto.historySet( toDtoHistorySet( document.getHistorySet() ) );
        documentDto.register( toDtoRegister( document.getRegister() ) );
        documentDto.id( document.getId() );
        documentDto.innerId( document.getInnerId() );
        documentDto.author( document.getAuthor() );
        documentDto.title( document.getTitle() );
        documentDto.status( document.getStatus() );
        documentDto.createTime( document.getCreateTime() );
        documentDto.updateTime( document.getUpdateTime() );

        return documentDto.build();
    }

    @Override
    public List<Document> dtosToEntities(List<DocumentDto> documentDtos) {
        if ( documentDtos == null ) {
            return null;
        }

        List<Document> list = new ArrayList<Document>( documentDtos.size() );
        for ( DocumentDto documentDto : documentDtos ) {
            list.add( dtoToEntity( documentDto ) );
        }

        return list;
    }

    @Override
    public List<DocumentDto> entitiesToDtos(List<Document> documents) {
        if ( documents == null ) {
            return null;
        }

        List<DocumentDto> list = new ArrayList<DocumentDto>( documents.size() );
        for ( Document document : documents ) {
            list.add( entityToDto( document ) );
        }

        return list;
    }
}
