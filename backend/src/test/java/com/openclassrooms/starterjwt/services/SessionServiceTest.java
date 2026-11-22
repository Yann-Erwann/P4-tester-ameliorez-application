package com.openclassrooms.starterjwt.services;

import com.openclassrooms.starterjwt.exception.*;
import com.openclassrooms.starterjwt.models.*;
import com.openclassrooms.starterjwt.repository.*;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import java.util.*;
import static java.util.Objects.requireNonNull;
import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;

@Tag("unit")
class SessionServiceTest {
    final SessionRepository sessions = mock(SessionRepository.class);
    final UserRepository users = mock(UserRepository.class);
    final SessionService service = new SessionService(sessions, users);
    final User member = new User("member@example.invalid", "Last", "First", "encoded", false).setId(7L);
    final Session session = Session.builder().id(1L).name("Original").users(new ArrayList<>()).build();

    @Test void updatesOnlyEditableFieldsAndPreservesMembersAndIdentity() {
        session.getUsers().add(member);
        when(sessions.findById(1L)).thenReturn(Optional.of(session));
        when(sessions.save(requireNonNull(session))).thenReturn(session);
        var changes = Session.builder().id(999L).name("Updated").description("Practice").date(new Date())
                .teacher(Teacher.builder().id(8L).build()).users(List.of()).build();
        assertThat(service.update(1L, changes)).isSameAs(session);
        assertThat(session.getUsers()).containsExactly(member);
        assertThat(session.getId()).isEqualTo(1L);
        assertThat(session.getName()).isEqualTo("Updated");
    }
    @SuppressWarnings("null") // Mockito supplies a dummy null when registering the matcher.
    @Test void refusesUpdatingAnAbsentSessionWithoutWriting() {
        assertThatThrownBy(() -> service.update(1L, session)).isInstanceOf(NotFoundException.class);
        verify(sessions, never()).save(any());
    }
    @SuppressWarnings("null") // Mockito supplies a dummy null when registering the matcher.
    @ParameterizedTest @ValueSource(booleans = {false, true})
    void participationRequiresBothAnAccountAndASession(boolean missingAccount) {
        if (missingAccount) when(sessions.findForParticipation(1L)).thenReturn(Optional.of(session));
        else when(users.findById(7L)).thenReturn(Optional.of(member));
        assertThatThrownBy(() -> service.participate(1L, 7L)).isInstanceOf(NotFoundException.class);
        verify(sessions, never()).save(any());
    }
    @Test void addsOneMemberAndRejectsDuplicateParticipation() {
        when(sessions.findForParticipation(1L)).thenReturn(Optional.of(session));
        when(users.findById(7L)).thenReturn(Optional.of(member));
        service.participate(1L, 7L);
        assertThat(session.getUsers()).containsExactly(member);
        assertThatThrownBy(() -> service.participate(1L, 7L)).isInstanceOf(BadRequestException.class);
        verify(sessions, times(1)).save(requireNonNull(session));
    }
    @Test void removingAMemberPreservesOtherMembers() {
        User other = new User("other@example.invalid", "Last", "Other", "encoded", false).setId(8L);
        session.getUsers().addAll(List.of(member, other));
        when(sessions.findForParticipation(1L)).thenReturn(Optional.of(session));
        service.noLongerParticipate(1L, 7L);
        assertThat(session.getUsers()).containsExactly(other);
        assertThatThrownBy(() -> service.noLongerParticipate(1L, 7L)).isInstanceOf(BadRequestException.class);
    }
    @Test void removingFromAMissingSessionReturnsNotFound() {
        assertThatThrownBy(() -> service.noLongerParticipate(1L, 7L)).isInstanceOf(NotFoundException.class);
    }
}
