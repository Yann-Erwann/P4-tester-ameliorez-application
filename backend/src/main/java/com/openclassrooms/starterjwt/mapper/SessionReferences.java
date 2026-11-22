package com.openclassrooms.starterjwt.mapper;

import com.openclassrooms.starterjwt.models.Teacher;
import com.openclassrooms.starterjwt.models.User;
import com.openclassrooms.starterjwt.services.TeacherService;
import com.openclassrooms.starterjwt.services.UserService;
import org.mapstruct.Named;
import org.springframework.stereotype.Component;

import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@Component
class SessionReferences {
    private final TeacherService teacherService;
    private final UserService userService;

    SessionReferences(TeacherService teacherService, UserService userService) {
        this.teacherService = teacherService;
        this.userService = userService;
    }

    @Named("resolveTeacher")
    Teacher resolveTeacher(Long id) {
        return id == null ? null : teacherService.getRequiredById(id);
    }

    @Named("resolveUsers")
    List<User> resolveUsers(List<Long> ids) {
        return Optional.ofNullable(ids).orElseGet(Collections::emptyList).stream()
                .map(userService::getRequiredById)
                .collect(Collectors.toList());
    }
}
