package com.openclassrooms.starterjwt.services;

import com.openclassrooms.starterjwt.exception.NotFoundException;
import com.openclassrooms.starterjwt.models.Teacher;
import com.openclassrooms.starterjwt.repository.TeacherRepository;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import java.util.List;
import java.util.Optional;
import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;

@Tag("unit")
class TeacherServiceTest {
    final TeacherRepository repository = mock(TeacherRepository.class);
    final TeacherService service = new TeacherService(repository);
    @Test void listsTeachersIncludingAnEmptyStudio() {
        when(repository.findAll()).thenReturn(List.of());
        assertThat(service.findAll()).isEmpty();
        Teacher teacher = Teacher.builder().id(7L).firstName("First").lastName("Last").build();
        when(repository.findAll()).thenReturn(List.of(teacher));
        assertThat(service.findAll()).containsExactly(teacher);
    }
    @Test void returnsAnExistingTeacherForBothLookupContracts() {
        Teacher teacher = Teacher.builder().id(7L).build();
        when(repository.findById(7L)).thenReturn(Optional.of(teacher));
        assertThat(service.findById(7L)).isSameAs(teacher);
        assertThat(service.getRequiredById(7L)).isSameAs(teacher);
    }
    @Test void distinguishesAnOptionalLookupFromARequiredLookup() {
        assertThat(service.findById(7L)).isNull();
        assertThatThrownBy(() -> service.getRequiredById(7L)).isInstanceOf(NotFoundException.class);
    }
}
