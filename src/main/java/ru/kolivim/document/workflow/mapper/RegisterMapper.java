package ru.kolivim.document.workflow.mapper;

import ru.kolivim.document.workflow.dto.RegisterDto;
import ru.kolivim.document.workflow.entity.Register;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

import java.util.List;

@Mapper
public interface RegisterMapper {

    @Mapping(target = "document", ignore = true)
    Register dtoToEntity(RegisterDto registerDto);

    @Mapping(target = "document", ignore = true)
    RegisterDto entityToDto(Register register);

    List<Register> dtosToEntities(List<RegisterDto> registerDtos);

    List<RegisterDto> entitiesToDtos(List<Register> registers);
}
