package com.openclassrooms.starterjwt.services;

import com.openclassrooms.starterjwt.models.*;
import com.openclassrooms.starterjwt.repository.*;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import java.util.*;
import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;

@Tag("unit")
class UserServiceTest {
    @Test void removesOnlyTheDeletedAccountsMembershipBeforeRemovingTheAccount() {
        UserRepository users = mock(UserRepository.class);
        SessionRepository sessions = mock(SessionRepository.class);
        User owner = new User("owner@example.invalid", "Last", "First", "encoded", false).setId(7L);
        User other = new User("other@example.invalid", "Last", "Other", "encoded", false).setId(8L);
        Session session = Session.builder().users(new ArrayList<>(List.of(owner, other))).build();
        when(users.findById(7L)).thenReturn(Optional.of(owner));
        when(sessions.findAllByUsers_Id(7L)).thenReturn(List.of(session));
        new UserService(users, sessions).delete(7L, owner.getEmail());
        assertThat(session.getUsers()).containsExactly(other);
        var ordered = inOrder(sessions, users);
        ordered.verify(sessions).flush(); ordered.verify(users).delete(owner);
    }
    @ParameterizedTest @ValueSource(ints = {0, 1, 2, 3})
    void rejectsMissingIdentityFieldsAtConstruction(int field) {
        String[] values = {"member@example.invalid", "Last", "First", "encoded"}; values[field] = null;
        assertThatThrownBy(() -> new User(values[0], values[1], values[2], values[3], false))
                .isInstanceOf(NullPointerException.class);
    }
}
