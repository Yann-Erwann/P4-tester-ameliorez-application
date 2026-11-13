package com.openclassrooms.starterjwt;

import com.openclassrooms.starterjwt.models.Session;
import com.openclassrooms.starterjwt.models.Teacher;
import com.openclassrooms.starterjwt.models.User;
import com.openclassrooms.starterjwt.repository.SessionRepository;
import com.openclassrooms.starterjwt.repository.TeacherRepository;
import com.openclassrooms.starterjwt.repository.UserRepository;
import com.openclassrooms.starterjwt.services.SessionService;
import com.openclassrooms.starterjwt.services.UserService;
import jakarta.persistence.EntityManager;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.*;

@SpringBootTest(properties = {
        "spring.docker.compose.enabled=false",
        "spring.datasource.url=${YOGA_MYSQL_TEST_URL}",
        "spring.datasource.driver-class-name=com.mysql.cj.jdbc.Driver",
        "spring.jpa.hibernate.ddl-auto=validate"
})
@EnabledIfEnvironmentVariable(named = "YOGA_MYSQL_TEST_URL", matches = "jdbc:mysql:.*")
@Transactional
class MySqlPersistenceTest {
    @Autowired UserRepository users;
    @Autowired TeacherRepository teachers;
    @Autowired SessionRepository sessions;
    @Autowired UserService userService;
    @Autowired SessionService sessionService;
    @Autowired EntityManager entityManager;
    @Autowired JdbcTemplate jdbc;

    User newMember() {
        return users.save(new User("delivery-" + UUID.randomUUID().toString().substring(0, 12) + "@example.invalid",
                "Delivery", "Validation", "synthetic-test-hash", false));
    }

    Session newSession(List<User> participants) {
        Teacher teacher = teachers.save(Teacher.builder().firstName("Delivery").lastName("Validation").build());
        return sessions.saveAndFlush(Session.builder().name("Delivery validation").date(new Date())
                .description("Temporary fixture rolled back after test").teacher(teacher)
                .users(new ArrayList<>(participants)).build());
    }

    @Test
    void deletingAnEnrolledAccountPreservesSessionsAndOtherMembers() {
        User removed = newMember();
        User retained = newMember();
        Session first = newSession(List.of(removed, retained));
        Session second = newSession(List.of(removed));
        userService.delete(removed.getId(), removed.getEmail());
        users.flush();
        entityManager.clear();
        assertThat(users.findById(removed.getId())).isEmpty();
        assertThat(users.findById(retained.getId())).isPresent();
        assertThat(sessions.findById(first.getId()).orElseThrow().getUsers())
                .extracting(User::getId).containsExactly(retained.getId());
        assertThat(sessions.findById(second.getId()).orElseThrow().getUsers()).isEmpty();
        assertThat(teachers.findById(first.getTeacher().getId())).isPresent();
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    void updatingSessionDetailsPreservesMembershipsOnMySql() {
        User participant = newMember();
        Session original = newSession(List.of(participant));
        entityManager.clear();
        var createdAt = sessions.findById(original.getId()).orElseThrow().getCreatedAt();
        Session changes = Session.builder().name("Updated delivery session").date(new Date())
                .description("x".repeat(2500)).teacher(original.getTeacher()).build();
        sessionService.update(original.getId(), changes);
        sessions.flush();
        entityManager.clear();
        Session reloaded = sessions.findById(original.getId()).orElseThrow();
        assertThat(reloaded.getUsers()).extracting(User::getId).containsExactly(participant.getId());
        assertThat(reloaded.getCreatedAt()).isEqualTo(createdAt);
        assertThat(reloaded.getDescription()).hasSize(2500);
    }

    @Test
    void foreignKeysStillRejectAnUnknownTeacher() {
        assertThatThrownBy(() -> jdbc.update(
                "INSERT INTO sessions(name,date,description,teacher_id) VALUES (?,CURRENT_TIMESTAMP,?,?)",
                "Orphan validation", "Must not be persisted", Long.MAX_VALUE))
                .isInstanceOf(DataIntegrityViolationException.class);
    }

    @Test
    void duplicateEmailsRemainRejectedByMySql() {
        User original = newMember();
        users.flush();
        assertThatThrownBy(() -> users.saveAndFlush(new User(original.getEmail(), "Other", "Member", "synthetic-test-hash", false)))
                .isInstanceOf(DataIntegrityViolationException.class);
    }
}
