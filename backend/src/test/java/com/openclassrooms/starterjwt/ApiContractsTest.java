package com.openclassrooms.starterjwt;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.openclassrooms.starterjwt.models.Session;
import com.openclassrooms.starterjwt.models.Teacher;
import com.openclassrooms.starterjwt.models.User;
import com.openclassrooms.starterjwt.mapper.SessionMapper;
import com.openclassrooms.starterjwt.mapper.TeacherMapper;
import com.openclassrooms.starterjwt.mapper.UserMapper;
import com.openclassrooms.starterjwt.repository.SessionRepository;
import com.openclassrooms.starterjwt.repository.TeacherRepository;
import com.openclassrooms.starterjwt.repository.UserRepository;
import com.openclassrooms.starterjwt.security.jwt.JwtUtils;
import com.openclassrooms.starterjwt.security.services.UserDetailsImpl;
import com.openclassrooms.starterjwt.security.services.UserDetailsServiceImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.lang.NonNull;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.RequestBuilder;
import org.springframework.test.web.servlet.ResultActions;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Map;

import static java.util.Objects.requireNonNull;
import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@Tag("integration")
@SpringBootTest(useMainMethod = SpringBootTest.UseMainMethod.ALWAYS, properties = {
        "spring.docker.compose.enabled=false",
        "spring.datasource.url=jdbc:h2:mem:yoga-contracts;MODE=MySQL;DB_CLOSE_DELAY=-1",
        "spring.datasource.driver-class-name=org.h2.Driver",
        "spring.datasource.username=sa",
        "spring.datasource.password=",
        "spring.jpa.hibernate.ddl-auto=create-drop",
        "spring.jpa.show-sql=false",
        "spring.jpa.properties.hibernate.generate_statistics=true",
        "logging.level.org.hibernate.engine.internal.StatisticalLoggingSessionEventListener=OFF",
        "oc.app.jwtSecret=QUFBQUFBQUFBQUFBQUFBQUFBQUFBQUFBQUFBQUFBQUFBQUFBQUFBQUFBQUFBQUFBQUFBQUFBQUFBQUFBQUFBQQ=="
})
@AutoConfigureMockMvc
@Transactional
class ApiContractsTest {
    @NonNull
    private static final MediaType JSON_MEDIA_TYPE = requireNonNull(MediaType.APPLICATION_JSON);

    @Autowired jakarta.persistence.EntityManager entityManager;
    @Autowired MockMvc mvc;
    @Autowired ObjectMapper json;
    @Autowired UserRepository users;
    @Autowired TeacherRepository teachers;
    @Autowired SessionRepository sessions;
    @Autowired PasswordEncoder passwords;
    @Autowired JwtUtils jwt;
    @Autowired UserDetailsServiceImpl identities;
    @Autowired UserMapper userMapper;
    @Autowired TeacherMapper teacherMapper;
    @Autowired SessionMapper sessionMapper;

    User owner;
    User admin;
    Teacher teacher;
    Session session;

    @BeforeEach
    void seedOwnedFixtures() {
        owner = users.save(new User("member@example.invalid", "Member", "Test", passwords.encode("Secret!123"), false));
        admin = users.save(new User("admin@example.invalid", "Admin", "Test", passwords.encode("Secret!123"), true));
        teacher = teachers.save(requireNonNull(Teacher.builder().firstName("Test").lastName("Teacher").build()));
        session = sessions.save(requireNonNull(Session.builder().name("Morning yoga").description("Practice")
                .date(new Date(1_800_000_000_000L)).teacher(teacher).users(new ArrayList<>()).build()));
        sessions.flush();
    }

    MockHttpServletRequestBuilder authenticated(MockHttpServletRequestBuilder request, User user) {
        UserDetailsImpl principal = (UserDetailsImpl) identities.loadUserByUsername(user.getEmail());
        String token = jwt.generateJwtToken(new UsernamePasswordAuthenticationToken(principal, null, principal.getAuthorities()));
        return request.header("Authorization", "Bearer " + token);
    }

    private ResultActions perform(RequestBuilder request) throws Exception {
        return mvc.perform(requireNonNull(request));
    }

    @NonNull
    String sessionBody(String name) throws Exception {
        return requireNonNull(json.writeValueAsString(Map.of(
                "name", name, "date", "2026-12-01", "teacher_id", teacher.getId(), "description", "Practice")));
    }

    @Test
    void registrationHashesPasswordAndKeepsPublicSuccessMessage() throws Exception {
        perform(post("/api/auth/register").contentType(JSON_MEDIA_TYPE)
                        .content("{\"email\":\"new@example.invalid\",\"firstName\":\"New\",\"lastName\":\"Member\",\"password\":\"Secret!123\"}"))
                .andExpect(status().isOk()).andExpect(jsonPath("$.message").value("User registered successfully!"));
        User created = users.findByEmail("new@example.invalid").orElseThrow();
        assertThat(created.isAdmin()).isFalse();
        assertThat(created.getFirstName()).isEqualTo("New");
        assertThat(passwords.matches("Secret!123", created.getPassword())).isTrue();
    }

    @Test
    void registrationRejectsAnExistingAddress() throws Exception {
        long before = users.count();
        perform(post("/api/auth/register").contentType(JSON_MEDIA_TYPE)
                        .content("{\"email\":\"member@example.invalid\",\"firstName\":\"New\",\"lastName\":\"Member\",\"password\":\"Secret!123\"}"))
                .andExpect(status().isBadRequest()).andExpect(jsonPath("$.message").value("Error: Email is already taken!"));
        assertThat(users.count()).isEqualTo(before);
    }

    @ParameterizedTest
    @ValueSource(booleans = {false, true})
    void loginReturnsTheIdentityRoleAndUsableJwt(boolean asAdmin) throws Exception {
        User user = asAdmin ? admin : owner;
        String body = perform(post("/api/auth/login").contentType(JSON_MEDIA_TYPE)
                        .content(requireNonNull(json.writeValueAsString(Map.of("email", user.getEmail(), "password", "Secret!123")))))
                .andExpect(status().isOk()).andExpect(jsonPath("$.id").value(user.getId()))
                .andExpect(jsonPath("$.admin").value(asAdmin)).andExpect(jsonPath("$.type").value("Bearer"))
                .andExpect(jsonPath("$.firstName").value(user.getFirstName()))
                .andReturn().getResponse().getContentAsString();
        String token = json.readTree(body).get("token").asText();
        assertThat(jwt.validateJwtToken(token)).isTrue();
        perform(get("/api/session").header("Authorization", "Bearer " + token)).andExpect(status().isOk());
    }

    @Test
    void incorrectAndUnknownCredentialsAreRefused() throws Exception {
        for (String email : List.of(owner.getEmail(), "unknown@example.invalid")) {
            perform(post("/api/auth/login").contentType(JSON_MEDIA_TYPE)
                            .content(requireNonNull(json.writeValueAsString(Map.of("email", email, "password", "Incorrect!123")))))
                    .andExpect(status().isUnauthorized());
        }
    }

    @Test
    void anonymousAndInvalidBearerRequestsAreRefused() throws Exception {
        perform(get("/api/session")).andExpect(status().isUnauthorized());
        perform(get("/api/session").header("Authorization", "Bearer malformed")).andExpect(status().isUnauthorized());
        perform(get("/api/session").header("Authorization", "Basic ignored")).andExpect(status().isUnauthorized());
    }

    @Test
    void aSignedTokenForADeletedAccountDoesNotGrantAccess() throws Exception {
        MockHttpServletRequestBuilder request = authenticated(get("/api/session"), owner);
        users.delete(requireNonNull(owner));
        users.flush();
        perform(request).andExpect(status().isUnauthorized());
    }

    @Test
    void mappingRoundTripsTransportFieldsAndCollections() {
        var userDto = userMapper.toDto(owner);
        assertThat(userMapper.toEntity(userDto).getEmail()).isEqualTo(owner.getEmail());
        assertThat(userMapper.toDto(List.of(owner))).extracting(dto -> dto.getId()).containsExactly(owner.getId());
        assertThat(userMapper.toEntity(List.of(userDto))).extracting(user -> user.getId()).containsExactly(owner.getId());
        var teacherDto = teacherMapper.toDto(teacher);
        assertThat(teacherMapper.toEntity(teacherDto).getLastName()).isEqualTo(teacher.getLastName());
        assertThat(teacherMapper.toEntity(List.of(teacherDto))).extracting(teacher -> teacher.getId()).containsExactly(teacher.getId());
        session.getUsers().add(owner);
        var sessionDto = sessionMapper.toDto(session);
        assertThat(sessionDto.getUsers()).containsExactly(owner.getId());
        assertThat(sessionMapper.toEntity(sessionDto).getUsers()).extracting(user -> user.getId()).containsExactly(owner.getId());
        assertThat(sessionMapper.toEntity(List.of(sessionDto))).extracting(session -> session.getName()).containsExactly(session.getName());
    }

    @Test
    void mappersPreserveAbsentSources() {
        assertThat(userMapper.toDto((User) null)).isNull();
        assertThat(userMapper.toEntity((com.openclassrooms.starterjwt.dto.UserDto) null)).isNull();
        assertThat(userMapper.toDto((List<User>) null)).isNull();
        assertThat(userMapper.toEntity((List<com.openclassrooms.starterjwt.dto.UserDto>) null)).isNull();
        assertThat(teacherMapper.toDto((Teacher) null)).isNull();
        assertThat(teacherMapper.toEntity((com.openclassrooms.starterjwt.dto.TeacherDto) null)).isNull();
        assertThat(sessionMapper.toDto((Session) null)).isNull();
        assertThat(sessionMapper.toEntity((com.openclassrooms.starterjwt.dto.SessionDto) null)).isNull();
    }

    @Test
    void persistentIdentityIsStableWhenEditableFieldsDiffer() {
        User userCopy = User.builder().id(owner.getId()).email("renamed@example.invalid")
                .firstName("New").lastName("Name").password("test-only-hash").build();
        Teacher teacherCopy = Teacher.builder().id(teacher.getId()).firstName("New").lastName("Name").build();
        Session sessionCopy = Session.builder().id(session.getId()).name("Renamed").build();
        assertThat(userCopy).isEqualTo(owner).hasSameHashCodeAs(owner);
        assertThat(teacherCopy).isEqualTo(teacher).hasSameHashCodeAs(teacher);
        assertThat(sessionCopy).isEqualTo(session).hasSameHashCodeAs(session);
    }

    @Test
    void mappingOptionalRelationsAndEmptyCollectionsIsSafe() {
        Session withoutRelations = Session.builder().name("Unassigned").description("Practice").date(new Date()).build();
        var dto = sessionMapper.toDto(withoutRelations);
        assertThat(dto.getTeacher_id()).isNull();
        assertThat(dto.getUsers()).isEmpty();
        dto.setUsers(null);
        assertThat(sessionMapper.toEntity(dto).getTeacher()).isNull();
        assertThat(sessionMapper.toEntity(dto).getUsers()).isEmpty();
        assertThat(sessionMapper.toDto(List.<Session>of())).isEmpty();
        assertThat(teacherMapper.toEntity(List.of())).isEmpty();
        assertThat(userMapper.toEntity(List.of())).isEmpty();
    }

    @Test
    void sessionSummaryReturnsCardFieldsAndKeepsTheOriginalListContract() throws Exception {
        perform(authenticated(get("/api/session").param("view", "summary"), owner))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].id").value(session.getId()))
                .andExpect(jsonPath("$[0].name").value(session.getName()))
                .andExpect(jsonPath("$[0].date").isNotEmpty())
                .andExpect(jsonPath("$[0].description").value(session.getDescription()))
                .andExpect(jsonPath("$[0].teacher_id").doesNotExist())
                .andExpect(jsonPath("$[0].users").doesNotExist());
        perform(authenticated(get("/api/session"), owner)).andExpect(status().isOk())
                .andExpect(jsonPath("$[0].teacher_id").value(teacher.getId()))
                .andExpect(jsonPath("$[0].users").isArray());
    }

    @ParameterizedTest
    @ValueSource(ints = {1, 25})
    void summaryReadsAnyNumberOfCardsInOneStatement(int total) {
        for (int n = 1; n < total; n++) {
            sessions.save(requireNonNull(Session.builder().name("Yoga " + n).date(session.getDate())
                    .description("Evening session").build()));
        }
        sessions.flush();
        entityManager.clear();
        var statistics = entityManager.getEntityManagerFactory()
                .unwrap(org.hibernate.SessionFactory.class).getStatistics();
        long before = statistics.getPrepareStatementCount();
        assertThat(sessions.findSummaries()).hasSize(total);
        assertThat(statistics.getPrepareStatementCount() - before).isEqualTo(1);
    }

    @Test
    void readsSessionTeacherAndAccountWithoutExposingThePassword() throws Exception {
        perform(authenticated(get("/api/session/{id}", session.getId()), owner))
                .andExpect(status().isOk()).andExpect(jsonPath("$.name").value("Morning yoga"))
                .andExpect(jsonPath("$.teacher_id").value(teacher.getId())).andExpect(jsonPath("$.users").isEmpty());
        perform(authenticated(get("/api/teacher"), owner)).andExpect(status().isOk()).andExpect(jsonPath("$[0].id").value(teacher.getId()));
        perform(authenticated(get("/api/teacher/{id}", teacher.getId()), owner))
                .andExpect(status().isOk()).andExpect(jsonPath("$.firstName").value("Test"));
        perform(authenticated(get("/api/user/{id}", owner.getId()), owner))
                .andExpect(status().isOk()).andExpect(jsonPath("$.email").value(owner.getEmail()))
                .andExpect(jsonPath("$.password").doesNotExist());
    }

    @ParameterizedTest
    @ValueSource(strings = {"/api/session", "/api/teacher", "/api/user"})
    void missingAndNonNumericReadIdsKeepEmpty404And400Responses(String path) throws Exception {
        perform(authenticated(get(path + "/999999999"), owner)).andExpect(status().isNotFound()).andExpect(content().string(""));
        perform(authenticated(get(path + "/invalid"), owner)).andExpect(status().isBadRequest()).andExpect(content().string(""));
    }

    @Test
    void createsUpdatesAndDeletesAnAdministratorSession() throws Exception {
        Teacher second = teachers.save(requireNonNull(Teacher.builder().firstName("Other").lastName("Teacher").build()));
        String body = json.writeValueAsString(Map.of("name", "New yoga", "date", "2026-12-02", "teacher_id", second.getId(), "description", "Practice"));
        String created = perform(authenticated(post("/api/session"), admin).contentType(JSON_MEDIA_TYPE).content(requireNonNull(body)))
                .andExpect(status().isOk()).andExpect(jsonPath("$.name").value("New yoga")).andReturn().getResponse().getContentAsString();
        long id = json.readTree(created).get("id").asLong();
        perform(authenticated(put("/api/session/{id}", id), admin).contentType(JSON_MEDIA_TYPE).content(requireNonNull(body.replace("New yoga", "Updated yoga"))))
                .andExpect(status().isOk()).andExpect(jsonPath("$.id").value(id)).andExpect(jsonPath("$.name").value("Updated yoga"));
        perform(authenticated(delete("/api/session/{id}", id), admin)).andExpect(status().isOk()).andExpect(content().string(""));
        assertThat(sessions.findById(id)).isEmpty();
    }

    @Test
    void updatingSessionDetailsKeepsParticipantsAndCreationDate() throws Exception {
        session.getUsers().add(owner);
        sessions.saveAndFlush(requireNonNull(session));
        long id = session.getId();
        long participantId = owner.getId();
        entityManager.clear();
        var createdAt = sessions.findById(id).orElseThrow().getCreatedAt();
        // The frontend omits users while editing session details.
        perform(authenticated(put("/api/session/{id}", id), admin)
                        .contentType(JSON_MEDIA_TYPE).content(sessionBody("Updated details")))
                .andExpect(status().isOk()).andExpect(jsonPath("$.users[0]").value(participantId))
                .andExpect(jsonPath("$.name").value("Updated details"));
        sessions.flush();
        entityManager.clear();
        Session reloaded = sessions.findById(id).orElseThrow();
        assertThat(reloaded.getUsers()).extracting(user -> user.getId()).containsExactly(participantId);
        assertThat(reloaded.getCreatedAt()).isEqualTo(createdAt);
        assertThat(reloaded.getName()).isEqualTo("Updated details");
    }

    @Test
    void updatingAnAbsentSessionReturns404WithoutCreatingOne() throws Exception {
        long before = sessions.count();
        perform(authenticated(put("/api/session/999999999"), admin)
                        .contentType(JSON_MEDIA_TYPE).content(sessionBody("Absent")))
                .andExpect(status().isNotFound()).andExpect(content().string(""));
        assertThat(sessions.count()).isEqualTo(before);
    }

    @Test
    void invalidMutationIdsDoNotWrite() throws Exception {
        perform(authenticated(put("/api/session/invalid"), admin).contentType(JSON_MEDIA_TYPE).content(sessionBody("Updated")))
                .andExpect(status().isBadRequest());
        for (String path : List.of("/api/session/invalid", "/api/user/invalid", "/api/session/invalid/participate/1")) {
            perform(authenticated(delete(requireNonNull(path)), owner)).andExpect(status().isBadRequest()).andExpect(content().string(""));
        }
        perform(authenticated(post("/api/session/invalid/participate/1"), owner)).andExpect(status().isBadRequest());
        assertThat(sessions.count()).isEqualTo(1);
    }

    @Test
    void deletingMissingResourcesReturns404() throws Exception {
        perform(authenticated(delete("/api/session/999999999"), admin)).andExpect(status().isNotFound());
        perform(authenticated(delete("/api/user/999999999"), owner)).andExpect(status().isNotFound());
    }

    @Test
    void accountDeletionKeepsTheOwnershipRule() throws Exception {
        perform(authenticated(delete("/api/user/{id}", owner.getId()), admin)).andExpect(status().isUnauthorized());
        assertThat(users.findById(requireNonNull(owner.getId()))).isPresent();
        perform(authenticated(delete("/api/user/{id}", owner.getId()), owner)).andExpect(status().isOk()).andExpect(content().string(""));
        assertThat(users.findById(requireNonNull(owner.getId()))).isEmpty();
    }

    @Test
    void deletingAnEnrolledAccountRemovesOnlyItsOwnMemberships() throws Exception {
        Teacher secondTeacher = teachers.save(requireNonNull(Teacher.builder().firstName("Second").lastName("Teacher").build()));
        Session secondSession = sessions.save(requireNonNull(Session.builder().name("Evening yoga").description("Practice")
                .date(new Date()).teacher(secondTeacher).users(new ArrayList<>(List.of(owner))).build()));
        session.getUsers().addAll(List.of(owner, admin));
        sessions.saveAndFlush(requireNonNull(session));
        long ownerId = owner.getId();
        long firstId = session.getId();
        long secondId = secondSession.getId();
        perform(authenticated(delete("/api/user/{id}", ownerId), owner))
                .andExpect(status().isOk()).andExpect(content().string(""));
        users.flush();
        entityManager.clear();
        assertThat(users.findById(ownerId)).isEmpty();
        assertThat(users.findById(requireNonNull(admin.getId()))).isPresent();
        assertThat(sessions.findById(firstId).orElseThrow().getUsers()).extracting(user -> user.getId()).containsExactly(admin.getId());
        assertThat(sessions.findById(secondId).orElseThrow().getUsers()).isEmpty();
        assertThat(sessions.count()).isEqualTo(2);
        assertThat(teachers.count()).isEqualTo(2);
    }

    @Test
    void participationAndRemovalKeepEmptySuccessResponsesAndPersistMembership() throws Exception {
        String path = "/api/session/" + session.getId() + "/participate/" + owner.getId();
        perform(authenticated(post(path), owner)).andExpect(status().isOk()).andExpect(content().string(""));
        sessions.flush();
        entityManager.clear();
        assertThat(sessions.findById(requireNonNull(session.getId())).orElseThrow().getUsers()).extracting(user -> user.getId()).containsExactly(owner.getId());
        perform(authenticated(post(path), owner)).andExpect(status().isBadRequest());
        perform(authenticated(delete(path), owner)).andExpect(status().isOk()).andExpect(content().string(""));
        sessions.flush();
        entityManager.clear();
        assertThat(sessions.findById(requireNonNull(session.getId())).orElseThrow().getUsers()).isEmpty();
        perform(authenticated(delete(path), owner)).andExpect(status().isBadRequest());
    }

    @Test
    void participationRequiresExistingSessionAndUser() throws Exception {
        perform(authenticated(post("/api/session/999999999/participate/{id}", owner.getId()), owner)).andExpect(status().isNotFound());
        perform(authenticated(post("/api/session/{id}/participate/999999999", session.getId()), owner)).andExpect(status().isForbidden());
        perform(authenticated(delete("/api/session/999999999/participate/{id}", owner.getId()), owner)).andExpect(status().isNotFound());
        assertThat(session.getUsers()).isEmpty();
    }

    @ParameterizedTest
    @ValueSource(booleans = {false, true})
    void sessionMutationsRequireAnAdministrator(boolean anonymous) throws Exception {
        var create = post("/api/session").contentType(JSON_MEDIA_TYPE).content(sessionBody("Forbidden"));
        var update = put("/api/session/{id}", session.getId()).contentType(JSON_MEDIA_TYPE).content(sessionBody("Forbidden"));
        var delete = delete("/api/session/{id}", session.getId());
        for (var request : List.of(create, update, delete)) {
            perform(anonymous ? request : authenticated(request, owner))
                    .andExpect(status().is(anonymous ? 401 : 403)).andExpect(content().string(""));
        }
        assertThat(sessions.count()).isEqualTo(1);
        entityManager.clear();
        assertThat(sessions.findById(requireNonNull(session.getId())).orElseThrow().getName()).isEqualTo("Morning yoga");
    }

    @ParameterizedTest
    @ValueSource(booleans = {false, true})
    void nobodyCanEnrollOrRemoveAnotherMember(boolean asAdmin) throws Exception {
        User caller = asAdmin ? admin : owner;
        User target = asAdmin ? owner : admin;
        String path = "/api/session/" + session.getId() + "/participate/" + target.getId();
        perform(authenticated(post(path), caller)).andExpect(status().isForbidden()).andExpect(content().string(""));
        assertThat(session.getUsers()).isEmpty();
        session.getUsers().add(target);
        sessions.saveAndFlush(requireNonNull(session));
        perform(authenticated(delete(path), caller)).andExpect(status().isForbidden()).andExpect(content().string(""));
        entityManager.clear();
        assertThat(sessions.findById(requireNonNull(session.getId())).orElseThrow().getUsers()).extracting(user -> user.getId()).containsExactly(target.getId());
    }

    @Test
    void authorizationReloadsRolesFromDatabaseInsteadOfTrustingTheClient() throws Exception {
        var request = authenticated(delete("/api/session/{id}", session.getId()), admin);
        admin.setAdmin(false);
        users.saveAndFlush(admin);
        perform(request).andExpect(status().isForbidden());
        assertThat(sessions.findById(requireNonNull(session.getId()))).isPresent();
        assertThat(identities.loadUserByUsername(owner.getEmail()).getAuthorities())
                .extracting(grantedAuthority -> grantedAuthority.getAuthority()).containsExactly("ROLE_USER");
    }

    @Test
    void errorDispatchKeepsTheOriginalStatusWithoutOpeningTheErrorUrl() throws Exception {
        perform(get("/error")).andExpect(status().isUnauthorized());
        perform(get("/error").with(request -> { request.setDispatcherType(jakarta.servlet.DispatcherType.ERROR); return request; })
                        .requestAttr(jakarta.servlet.RequestDispatcher.ERROR_STATUS_CODE, 500))
                .andExpect(status().isInternalServerError());
    }

    @Test
    void invalidJsonAndInvalidLengthsReturnEmpty400WithoutWrites() throws Exception {
        for (String body : List.of("{", "{}", "{\"email\":\"new@example.invalid\",\"firstName\":\"ab\",\"lastName\":\"Valid\",\"password\":\"Secret!123\"}")) {
            perform(post("/api/auth/register").contentType(JSON_MEDIA_TYPE).content(requireNonNull(body)))
                    .andExpect(status().isBadRequest()).andExpect(content().string(""));
        }
        var invalid = json.readTree(sessionBody("Too long"));
        ((com.fasterxml.jackson.databind.node.ObjectNode) invalid).put("description", "x".repeat(2501));
        perform(authenticated(post("/api/session"), admin).contentType(JSON_MEDIA_TYPE).content(requireNonNull(json.writeValueAsString(invalid))))
                .andExpect(status().isBadRequest()).andExpect(content().string(""));
        assertThat(users.count()).isEqualTo(2);
        assertThat(sessions.count()).isEqualTo(1);
    }

    @Test
    void creatingAnotherSessionForTheSameTeacherReturnsEmpty409() throws Exception {
        perform(authenticated(post("/api/session"), admin).contentType(JSON_MEDIA_TYPE)
                        .content(sessionBody("Conflicting teacher")))
                .andExpect(status().isConflict()).andExpect(content().string(""));
    }

    @Test
    void missingTeacherAndParticipantReferencesReturn404WithoutWrites() throws Exception {
        var body = (com.fasterxml.jackson.databind.node.ObjectNode) json.readTree(sessionBody("Missing relation"));
        body.put("teacher_id", 999999999);
        perform(authenticated(post("/api/session"), admin).contentType(JSON_MEDIA_TYPE).content(requireNonNull(json.writeValueAsString(body))))
                .andExpect(status().isNotFound()).andExpect(content().string(""));
        body.put("teacher_id", teacher.getId());
        body.putArray("users").add(999999999);
        perform(authenticated(post("/api/session"), admin).contentType(JSON_MEDIA_TYPE).content(requireNonNull(json.writeValueAsString(body))))
                .andExpect(status().isNotFound()).andExpect(content().string(""));
        assertThat(sessions.count()).isEqualTo(1);
    }

    @Test
    void invalidBodiesDoNotCreateResources() throws Exception {
        perform(post("/api/auth/register").contentType(JSON_MEDIA_TYPE).content("{}"))
                .andExpect(status().isBadRequest());
        perform(authenticated(post("/api/session"), admin).contentType(JSON_MEDIA_TYPE).content("{}"))
                .andExpect(status().isBadRequest());
        assertThat(users.count()).isEqualTo(2);
        assertThat(sessions.count()).isEqualTo(1);
    }
}
