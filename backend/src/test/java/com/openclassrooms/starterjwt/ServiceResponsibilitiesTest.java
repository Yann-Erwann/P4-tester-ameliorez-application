package com.openclassrooms.starterjwt;

import com.openclassrooms.starterjwt.exception.NotFoundException;
import com.openclassrooms.starterjwt.exception.UnauthorizedOperationException;
import com.openclassrooms.starterjwt.models.Session;
import com.openclassrooms.starterjwt.models.User;
import com.openclassrooms.starterjwt.repository.SessionRepository;
import com.openclassrooms.starterjwt.repository.UserRepository;
import com.openclassrooms.starterjwt.services.SessionService;
import com.openclassrooms.starterjwt.services.UserService;
import org.junit.jupiter.api.Test;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.*;

class ServiceResponsibilitiesTest {
    @Test
    void sessionDeletionRequiresAnExistingSessionWithoutControllerChecks() {
        SessionRepository sessions = mock(SessionRepository.class);
        SessionService service = new SessionService(sessions, mock(UserRepository.class));
        when(sessions.findById(42L)).thenReturn(Optional.empty());
        assertThatThrownBy(() -> service.delete(42L)).isInstanceOf(NotFoundException.class);
        verify(sessions, never()).delete(any(Session.class));
        Session existing = Session.builder().id(7L).build();
        when(sessions.findById(7L)).thenReturn(Optional.of(existing));
        service.delete(7L);
        verify(sessions).delete(existing);
    }

    @Test
    void accountDeletionEnforcesExistenceAndOwnershipInsideTheService() {
        UserRepository users = mock(UserRepository.class);
        UserService service = new UserService(users);
        when(users.findById(42L)).thenReturn(Optional.empty());
        assertThatThrownBy(() -> service.delete(42L, "owner@example.invalid")).isInstanceOf(NotFoundException.class);
        User owner = User.builder().id(7L).email("owner@example.invalid").firstName("Test").lastName("Owner").password("test-only-hash").build();
        when(users.findById(7L)).thenReturn(Optional.of(owner));
        assertThatThrownBy(() -> service.delete(7L, "other@example.invalid"))
                .isInstanceOf(UnauthorizedOperationException.class);
        verify(users, never()).delete(any(User.class));
        service.delete(7L, owner.getEmail());
        verify(users).delete(owner);
    }
}
