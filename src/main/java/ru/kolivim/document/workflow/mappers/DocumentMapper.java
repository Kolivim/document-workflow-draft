package ru.kolivim.document.workflow.mappers;

import ru.kolivim.document.workflow.dto.DocumentDto;
import ru.kolivim.document.workflow.dto.HistoryDto;
import ru.kolivim.document.workflow.dto.RegisterDto;
import ru.kolivim.document.workflow.entity.Document;
import ru.kolivim.document.workflow.entity.History;
import ru.kolivim.document.workflow.entity.Register;
import ru.kolivim.document.workflow.entity.enums.Action;
import ru.kolivim.document.workflow.entity.enums.Status;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.Named;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

@Mapper
public interface DocumentMapper {
    @Mapping(target = "historySet", source = "historySet", qualifiedByName = "toEntityHistorySet")
    @Mapping(target = "register", source = "register", qualifiedByName = "toEntityRegister")
    Document dtoToEntity(DocumentDto documentDto);

    @Mapping(target = "historySet", source = "historySet", qualifiedByName = "toDtoHistorySet")
    @Mapping(target = "register", source = "register", qualifiedByName = "toDtoRegister")
    DocumentDto entityToDto(Document document);

    List<Document> dtosToEntities(List<DocumentDto> documentDtos);

    List<DocumentDto> entitiesToDtos(List<Document> documents);

    @Named("toEntityHistorySet")
    default Set<History> toEntityHistorySet(Set<HistoryDto> historyDtoSet){
        Set<History> historySet = new HashSet<>();

        for (HistoryDto historyDto : new HashSet<>(historyDtoSet)) {
            historySet.add(History.builder()
                    .action(historyDto.getAction())
                    .time(historyDto.getTime())
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
                .status(registerDto == null? Status.DRAFT: registerDto.getStatus())
                .build();
    }



    @Named("toDtoHistorySet")
    default Set<HistoryDto> toDtoHistorySet(Set<History> historySet){
        Set<HistoryDto> historyDtoSet = new HashSet<>();

        for (History history : new HashSet<>(historySet)) {
            historyDtoSet.add(HistoryDto.builder()
                    .action(history == null? Action.SUBMIT: history.getAction())
                    .time(history == null? null: history.getTime())
                    .author(history == null? "": history.getAuthor())
                    .id(history == null? 0: history.getId())
                    .comment(history == null? "": history.getComment()).build());
        }
        return historyDtoSet;
    }

    @Named("toDtoRegister")
    default RegisterDto toDtoRegister(Register register){
        return RegisterDto.builder()
                .id(register == null? 0: register.getId())
                .status(register == null? Status.DRAFT: register.getStatus())
                .document(register == null ? null:
                        DocumentDto.builder()
                                .id(register.getDocument().getId())
                                .author(register.getDocument().getAuthor())
                                .title(register.getDocument().getTitle())
                                .createTime(register.getDocument().getCreateTime())
                                .updateTime(register.getDocument().getUpdateTime())
                                .innerId(register.getDocument().getInnerId())
                                .status(register.getStatus())
                                .build())
                .build();
    }
}
