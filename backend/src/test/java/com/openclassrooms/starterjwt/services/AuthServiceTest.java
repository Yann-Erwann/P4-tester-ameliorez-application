package com.openclassrooms.starterjwt.services;

import com.openclassrooms.starterjwt.models.User;
import com.openclassrooms.starterjwt.payload.request.LoginRequest;
import com.openclassrooms.starterjwt.payload.request.SignupRequest;
import com.openclassrooms.starterjwt.repository.UserRepository;
import com.openclassrooms.starterjwt.security.jwt.JwtUtils;
import com.openclassrooms.starterjwt.security.services.UserDetailsImpl;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.*;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.*;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.password.PasswordEncoder;
import java.util.List;
import java.util.Optional;
import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;

@Tag("unit")
@ExtendWith(MockitoExtension.class)
class AuthServiceTest {
    @Mock AuthenticationManager authenticationManager;
    @Mock PasswordEncoder passwordEncoder;
    @Mock JwtUtils jwtUtils;
    @Mock UserRepository users;
    @InjectMocks AuthService service;
    @Captor ArgumentCaptor<User> registered;
    @AfterEach void clearIdentity() { SecurityContextHolder.clearContext(); }

    SignupRequest signup() {
        SignupRequest request = new SignupRequest();
        request.setEmail("member@example.invalid"); request.setFirstName("First");
        request.setLastName("Last"); request.setPassword("Secret!123"); return request;
    }

    @SuppressWarnings("null") // Mockito supplies a dummy null when registering the matcher.
    @Test void registersANonAdminWithAnEncodedPassword() {
        SignupRequest request = signup();
        when(passwordEncoder.encode(request.getPassword())).thenReturn("encoded-test-password");
        assertThat(service.register(request)).isTrue();
        verify(users).save(registered.capture());
        User user = registered.getValue();
        assertThat(user.isAdmin()).isFalse();
        assertThat(user.getPassword()).isEqualTo("encoded-test-password");
        assertThat(user.getFirstName()).isEqualTo("First");
        assertThat(user.getLastName()).isEqualTo("Last");
    }

    @SuppressWarnings("null") // Mockito supplies a dummy null when registering the matcher.
    @Test void refusesDuplicateEmailWithoutEncodingOrWriting() {
        when(users.existsByEmail(signup().getEmail())).thenReturn(true);
        assertThat(service.register(signup())).isFalse();
        verifyNoInteractions(passwordEncoder);
        verify(users, never()).save(any());
    }

    @ParameterizedTest @ValueSource(booleans = {false, true})
    void usesTheRoleReadFromPersistence(boolean admin) {
        var identity = UserDetailsImpl.builder().id(7L).username("member@example.invalid").firstName("First").lastName("Last").build();
        var authentication = new UsernamePasswordAuthenticationToken(identity, null, List.of());
        when(authenticationManager.authenticate(any())).thenReturn(authentication);
        when(jwtUtils.generateJwtToken(authentication)).thenReturn("signed-test-token");
        when(users.findByEmail(identity.getUsername())).thenReturn(Optional.of(new User(identity.getUsername(), "Last", "First", "encoded", admin)));
        LoginRequest request = new LoginRequest(); request.setEmail(identity.getUsername()); request.setPassword("Secret!123");
        var response = service.authenticate(request);
        assertThat(response.getToken()).isEqualTo("signed-test-token");
        assertThat(response.getAdmin()).isEqualTo(admin);
        assertThat(SecurityContextHolder.getContext().getAuthentication()).isSameAs(authentication);
    }

    @Test void rejectsBadCredentialsWithoutIssuingAToken() {
        when(authenticationManager.authenticate(any())).thenThrow(new BadCredentialsException("Invalid credentials"));
        assertThatThrownBy(() -> service.authenticate(new LoginRequest())).isInstanceOf(BadCredentialsException.class);
        verifyNoInteractions(jwtUtils, users);
    }
}
