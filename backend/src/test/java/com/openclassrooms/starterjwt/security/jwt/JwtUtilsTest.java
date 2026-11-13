package com.openclassrooms.starterjwt.security.jwt;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.openclassrooms.starterjwt.security.services.UserDetailsImpl;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.Base64;
import java.util.Date;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class JwtUtilsTest {
    private final byte[] keyBytes = "A".repeat(64).getBytes(java.nio.charset.StandardCharsets.UTF_8);
    private final JwtUtils jwt = new JwtUtils();

    @BeforeEach
    void configureTestKey() {
        ReflectionTestUtils.setField(jwt, "jwtSecret", Base64.getEncoder().encodeToString(keyBytes));
        ReflectionTestUtils.setField(jwt, "jwtExpirationMs", 60_000);
    }

    @Test
    void signsAnIdentityAndReadsItsVerifiedSubject() {
        UserDetailsImpl identity = UserDetailsImpl.builder().id(7L).username("user@example.invalid").build();
        String token = jwt.generateJwtToken(new UsernamePasswordAuthenticationToken(identity, null, List.of()));
        assertThat(jwt.validateJwtToken(token)).isTrue();
        assertThat(jwt.getUserNameFromJwtToken(token)).isEqualTo(identity.getUsername());
    }

    @Test
    void rejectsExpiredUnsignedMalformedEmptyAndDifferentlySignedTokens() {
        String expired = Jwts.builder().subject("expired").expiration(new Date(1))
                .signWith(Keys.hmacShaKeyFor(keyBytes), Jwts.SIG.HS512).compact();
        String unsigned = Jwts.builder().subject("unsigned").compact();
        String differentSignature = Jwts.builder().subject("forged")
                .signWith(Keys.hmacShaKeyFor("B".repeat(64).getBytes(java.nio.charset.StandardCharsets.UTF_8)), Jwts.SIG.HS512).compact();
        for (String token : List.of(expired, unsigned, "malformed", "", differentSignature)) {
            assertThat(jwt.validateJwtToken(token)).isFalse();
        }
    }

    @Test
    void principalUsesPersistentIdentityAndDoesNotSerializeItsPassword() throws Exception {
        UserDetailsImpl first = UserDetailsImpl.builder().id(1L).username("user@example.invalid").password("test-only-hash").build();
        UserDetailsImpl sameId = UserDetailsImpl.builder().id(1L).username("renamed@example.invalid").build();
        UserDetailsImpl other = UserDetailsImpl.builder().id(2L).build();
        assertThat(first.equals(first)).isTrue();
        assertThat(first.equals(sameId)).isTrue();
        assertThat(first.equals(other)).isFalse();
        assertThat(first.equals(null)).isFalse();
        assertThat(first.equals("user")).isFalse();
        assertThat(first.isEnabled()).isTrue();
        assertThat(first.isAccountNonExpired()).isTrue();
        assertThat(first.isAccountNonLocked()).isTrue();
        assertThat(first.isCredentialsNonExpired()).isTrue();
        assertThat(new ObjectMapper().readTree(new ObjectMapper().writeValueAsString(first)).has("password")).isFalse();
    }
}
