package ru.kolivim.document.workflow.mapper;

import org.mapstruct.*;
import ru.kolivim.document.workflow.dto.DocumentDto;
import ru.kolivim.document.workflow.dto.HistoryDto;
import ru.kolivim.document.workflow.dto.RegisterDto;
import ru.kolivim.document.workflow.entity.Document;
import ru.kolivim.document.workflow.entity.History;
import ru.kolivim.document.workflow.entity.Register;
import ru.kolivim.document.workflow.entity.enums.Action;
import ru.kolivim.document.workflow.entity.enums.Status;
import ru.kolivim.document.workflow.mapper.qualification.ToExistingEntity;
import ru.kolivim.document.workflow.mapper.qualification.ToNewEntity;

import java.time.ZonedDateTime;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

@Mapper(componentModel = "spring",
        nullValuePropertyMappingStrategy = NullValuePropertyMappingStrategy.IGNORE /*, uses = {RegisterMapper.class , HistoryMapper.class} */ )
public interface DocumentMapper {

    @Mapping(target = "historySet", source = "historySet", qualifiedByName = "toEntityHistorySet")
    @Mapping(target = "register", source = "register", qualifiedByName = "toEntityRegister")
    /* @ToExistingEntity */
    Document dtoToEntity(DocumentDto documentDto);

    @Mappings({
            @Mapping(target = "historySet", ignore = true),
            @Mapping(target = "register", ignore = true),
            @Mapping(target = "status", source = "status", defaultValue = "DRAFT"),
            @Mapping(target = "createDate", expression = "java(java.time.ZonedDateTime.now())"),
            @Mapping(target = "updateDate", ignore = true)
    })
    /* @ToNewEntity */
    @Named("dtoToNewEntity")
    Document dtoToNewEntity(DocumentDto documentDto);

    @Mappings({
            @Mapping(target = "historySet", source = "historySet", qualifiedByName = "toDtoHistorySet", nullValuePropertyMappingStrategy = NullValuePropertyMappingStrategy.SET_TO_NULL),
            @Mapping(target = "register", source = "register", qualifiedByName = "toDtoRegister", nullValuePropertyMappingStrategy = NullValuePropertyMappingStrategy.SET_TO_NULL)
//            @Mapping(target = "description", ignore = true),
//            @Mapping(target = "updateDate", ignore = true)
    })
    DocumentDto entityToDto(Document document);

    List<Document> dtosToEntities(List<DocumentDto> documentDtos);

    List<DocumentDto> entitiesToDtos(List<Document> documents);

//    Page<DocumentDto> entitiesToDtos(Page<Document> documents);

    @Named("toEntityHistorySet")
    default Set<History> toEntityHistorySet(Set<HistoryDto> historyDtoSet){
        Set<History> historySet = new HashSet<>();

        for (HistoryDto historyDto : new HashSet<>(historyDtoSet)) {
            historySet.add(History.builder()
                    .action(historyDto.getAction())
                    .date(historyDto.getTime())
                    .author(historyDto.getAuthor())
                    .id(historyDto.getId())
                    .comment(historyDto.getComment()).build());
        }
        return historySet;
    }

    @Named("toEntityRegister")
    default Register toEntityRegister(RegisterDto registerDto){
        return Register.builder()
                .id(registerDto == null? 0: registerDto.getId())
//                .status(registerDto == null? Status.DRAFT: registerDto.getStatus())
                .build();
    }



    @Named("toDtoHistorySet")
    default Set<HistoryDto> toDtoHistorySet(Set<History> historySet){

        Set<HistoryDto> historyDtoSet = new HashSet<>();

        if(historySet == null) return null;

        for (History history : new HashSet<>(historySet)) {
            historyDtoSet.add(HistoryDto.builder()
                    .action(history == null? Action.SUBMIT: history.getAction())
                    .time(history == null? null: history.getDate())
                    .author(history == null? "": history.getAuthor())
                    .id(history == null? 0: history.getId())
                    .comment(history == null? "": history.getComment()).build());
        }
        return historyDtoSet;
    }

    @Named("toDtoRegister")
    default RegisterDto toDtoRegister(Register register){

        if(register == null) return null;

        return RegisterDto.builder()
                .id(register == null ? 0 : register.getId())
//                .status(register == null? Status.DRAFT: register.getStatus())
                .document(register == null ? null:
                        DocumentDto.builder()
                                .id(register.getDocument().getId())
                                .author(register.getDocument().getAuthor())
                                .name(register.getDocument().getName())
                                .createDate(register.getDocument().getCreateDate())
                                .updateDate(register.getDocument().getUpdateDate())
                                .innerId(register.getDocument().getInnerId())
                                .status(Status.APPROVED)
                                .build())
                .build();
    }

}
