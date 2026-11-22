package com.openclassrooms.starterjwt;

import com.openclassrooms.starterjwt.exception.BadRequestException;
import com.openclassrooms.starterjwt.models.Session;
import com.openclassrooms.starterjwt.models.Teacher;
import com.openclassrooms.starterjwt.models.User;
import com.openclassrooms.starterjwt.repository.SessionRepository;
import com.openclassrooms.starterjwt.repository.TeacherRepository;
import com.openclassrooms.starterjwt.repository.UserRepository;
import com.openclassrooms.starterjwt.security.services.UserDetailsImpl;
import com.openclassrooms.starterjwt.services.SessionService;
import com.openclassrooms.starterjwt.services.UserService;
import jakarta.persistence.EntityManager;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionTemplate;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.CyclicBarrier;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

import static java.util.Objects.requireNonNull;
import static org.assertj.core.api.Assertions.*;

@Tag("integration")
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
    @Autowired TransactionTemplate transactions;

    User newMember() {
        return users.save(new User("delivery-" + UUID.randomUUID().toString().substring(0, 12) + "@example.invalid",
                "Delivery", "Validation", "synthetic-test-hash", false));
    }

    Session newSession(List<User> participants) {
        Teacher teacher = teachers.save(requireNonNull(Teacher.builder().firstName("Delivery").lastName("Validation").build()));
        return sessions.saveAndFlush(requireNonNull(Session.builder().name("Delivery validation").date(new Date())
                .description("Temporary fixture rolled back after test").teacher(teacher)
                .users(new ArrayList<>(participants)).build()));
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
        assertThat(users.findById(requireNonNull(removed.getId()))).isEmpty();
        assertThat(users.findById(requireNonNull(retained.getId()))).isPresent();
        assertThat(sessions.findById(requireNonNull(first.getId())).orElseThrow().getUsers())
                .extracting(user -> user.getId()).containsExactly(retained.getId());
        assertThat(sessions.findById(requireNonNull(second.getId())).orElseThrow().getUsers()).isEmpty();
        assertThat(teachers.findById(requireNonNull(first.getTeacher().getId()))).isPresent();
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    void updatingSessionDetailsPreservesMembershipsOnMySql() {
        User participant = newMember();
        Session original = newSession(List.of(participant));
        entityManager.clear();
        var createdAt = sessions.findById(requireNonNull(original.getId())).orElseThrow().getCreatedAt();
        Session changes = Session.builder().name("Updated delivery session").date(new Date())
                .description("x".repeat(2500)).teacher(original.getTeacher()).build();
        sessionService.update(original.getId(), changes);
        sessions.flush();
        entityManager.clear();
        Session reloaded = sessions.findById(requireNonNull(original.getId())).orElseThrow();
        assertThat(reloaded.getUsers()).extracting(user -> user.getId()).containsExactly(participant.getId());
        assertThat(reloaded.getCreatedAt()).isEqualTo(createdAt);
        assertThat(reloaded.getDescription()).hasSize(2500);
    }

    @ParameterizedTest
    @ValueSource(booleans = {false, true})
    @EnabledIfEnvironmentVariable(named = "YOGA_MYSQL_CONCURRENCY_TESTS", matches = "true")
    @Transactional(propagation = Propagation.NOT_SUPPORTED)
    void concurrentParticipationPreservesDistinctMembersAndRejectsDuplicates(boolean sameAccount) throws Exception {
        // Independent transactions need committed fixtures visible to both connections.
        record Fixture(long session, long teacher, long first, long second) {}
        Fixture fixture = requireNonNull(transactions.execute(status -> {
            Session created = newSession(List.of());
            return new Fixture(created.getId(), created.getTeacher().getId(), newMember().getId(), newMember().getId());
        }));
        try (var workers = Executors.newFixedThreadPool(2)) {
            var start = new CyclicBarrier(2);
            long second = sameAccount ? fixture.first() : fixture.second();
            var a = workers.submit(() -> enrollTogether(start, fixture.session(), fixture.first()));
            var b = workers.submit(() -> enrollTogether(start, fixture.session(), second));
            var outcomes = List.of(a.get(20, TimeUnit.SECONDS), b.get(20, TimeUnit.SECONDS));
            var members = jdbc.queryForList("select user_id from participate where session_id = ?",
                    Long.class, fixture.session());
            if (sameAccount) {
                assertThat(outcomes).containsExactlyInAnyOrder(true, false);
                assertThat(members).containsExactly(fixture.first());
            } else {
                assertThat(outcomes).containsExactly(true, true);
                assertThat(members).containsExactlyInAnyOrder(fixture.first(), fixture.second());
            }
        } finally {
            transactions.executeWithoutResult(status -> {
                jdbc.update("delete from participate where session_id = ?", fixture.session());
                jdbc.update("delete from sessions where id = ?", fixture.session());
                jdbc.update("delete from teachers where id = ?", fixture.teacher());
                jdbc.update("delete from users where id in (?, ?)", fixture.first(), fixture.second());
            });
        }
    }

    private boolean enrollTogether(CyclicBarrier start, long sessionId, long userId) throws Exception {
        var principal = UserDetailsImpl.builder().id(userId).username("concurrency-test").build();
        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken(principal, null, principal.getAuthorities()));
        try {
            start.await(10, TimeUnit.SECONDS);
            sessionService.participate(sessionId, userId);
            return true;
        } catch (BadRequestException duplicate) {
            return false;
        } finally {
            SecurityContextHolder.clearContext();
        }
    }

    @Test
    void foreignKeysStillRejectAnUnknownTeacher() {
        assertThatThrownBy(() -> jdbc.update(
                "INSERT INTO sessions(name,date,description,teacher_id) VALUES (?,CURRENT_TIMESTAMP,?,?)",
                "Orphan validation", "Must not be persisted", Long.MAX_VALUE))
                .isInstanceOf(DataIntegrityViolationException.class);
    }

    @Test
    void aTeacherCannotBeAssignedToTwoSessions() {
        Session original = newSession(List.of());
        assertThatThrownBy(() -> jdbc.update(
                "INSERT INTO sessions(name,date,description,teacher_id) VALUES (?,CURRENT_TIMESTAMP,?,?)",
                "Conflicting validation", "Must not be persisted", original.getTeacher().getId()))
                .isInstanceOf(DataIntegrityViolationException.class);
        assertThat(jdbc.queryForObject("SELECT COUNT(*) FROM sessions WHERE teacher_id = ?", Long.class,
                original.getTeacher().getId())).isEqualTo(1L);
        assertThat(sessions.findById(requireNonNull(original.getId()))).isPresent();
    }

    @Test
    void duplicateEmailsRemainRejectedByMySql() {
        User original = newMember();
        users.flush();
        assertThatThrownBy(() -> users.saveAndFlush(new User(original.getEmail(), "Other", "Member", "synthetic-test-hash", false)))
                .isInstanceOf(DataIntegrityViolationException.class);
    }
}
