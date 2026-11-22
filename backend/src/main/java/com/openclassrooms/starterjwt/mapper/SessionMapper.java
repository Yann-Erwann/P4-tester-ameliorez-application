package com.openclassrooms.starterjwt.mapper;

import com.openclassrooms.starterjwt.dto.SessionDto;
import com.openclassrooms.starterjwt.models.Session;
import org.mapstruct.InjectionStrategy;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.Mappings;

import java.util.Collections;
import java.util.Optional;
import java.util.stream.Collectors;

@Mapper(componentModel = "spring", uses = SessionReferences.class, injectionStrategy = InjectionStrategy.SETTER,
        imports = {Collectors.class, Collections.class, Optional.class})
public interface SessionMapper extends EntityMapper<SessionDto, Session> {

    @Mappings({
            @Mapping(source = "description", target = "description"),
            @Mapping(target = "teacher", source = "teacher_id", qualifiedByName = "resolveTeacher"),
            @Mapping(target = "users", source = "users", qualifiedByName = "resolveUsers"),
    })
    Session toEntity(SessionDto sessionDto);


    @Mappings({
            @Mapping(source = "description", target = "description"),
            @Mapping(source = "session.teacher.id", target = "teacher_id"),
            @Mapping(target = "users", expression = "java(Optional.ofNullable(session.getUsers()).orElseGet(Collections::emptyList).stream().map(u -> u.getId()).collect(Collectors.toList()))"),
    })
    SessionDto toDto(Session session);
}
