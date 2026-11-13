package com.openclassrooms.starterjwt.services;

import com.openclassrooms.starterjwt.models.Teacher;
import com.openclassrooms.starterjwt.exception.NotFoundException;
import com.openclassrooms.starterjwt.repository.TeacherRepository;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Objects;

@Service
public class TeacherService {
    private final TeacherRepository teacherRepository;

    public TeacherService(TeacherRepository teacherRepository) {
        this.teacherRepository = teacherRepository;
    }

    public List<Teacher> findAll() {
        return this.teacherRepository.findAll();
    }

    public Teacher getRequiredById(Long id) {
        return this.teacherRepository.findById(Objects.requireNonNull(id)).orElseThrow(NotFoundException::new);
    }

    public Teacher findById(Long id) {
        return this.teacherRepository.findById(Objects.requireNonNull(id)).orElse(null);
    }
}
