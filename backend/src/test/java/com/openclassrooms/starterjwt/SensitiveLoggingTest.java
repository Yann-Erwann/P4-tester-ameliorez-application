package com.openclassrooms.starterjwt;

import ch.qos.logback.classic.Logger;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.read.ListAppender;
import com.openclassrooms.starterjwt.controllers.SessionController;
import com.openclassrooms.starterjwt.dto.SessionDto;
import com.openclassrooms.starterjwt.mapper.SessionMapper;
import com.openclassrooms.starterjwt.models.Session;
import com.openclassrooms.starterjwt.models.User;
import com.openclassrooms.starterjwt.services.SessionService;
import org.junit.jupiter.api.Test;
import org.slf4j.LoggerFactory;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;

class SensitiveLoggingTest {
    @Test
    void entityRepresentationsExcludePasswordHashesAndMemberships() {
        User user = new User("private@example.invalid", "Test", "Member", "synthetic-sensitive-hash", false);
        Session session = Session.builder().id(7L).users(List.of(user)).build();
        assertThat(user.toString()).doesNotContain("synthetic-sensitive-hash");
        assertThat(session.toString()).doesNotContain("synthetic-sensitive-hash", "private@example.invalid");
    }

    @Test
    void sessionCreationLogsItsIdWithoutRequestOrParticipantDetails() {
        User user = new User("private@example.invalid", "Test", "Member", "synthetic-sensitive-hash", false);
        Session created = Session.builder().id(7L).users(List.of(user)).build();
        SessionDto dto = new SessionDto();
        dto.setDescription("private-session-description");
        SessionService service = mock(SessionService.class);
        SessionMapper mapper = mock(SessionMapper.class);
        when(mapper.toEntity(dto)).thenReturn(created);
        when(service.create(created)).thenReturn(created);
        when(mapper.toDto(created)).thenReturn(dto);
        Logger logger = (Logger) LoggerFactory.getLogger(SessionController.class);
        ListAppender<ILoggingEvent> captured = new ListAppender<>();
        captured.start();
        logger.addAppender(captured);
        try {
            new SessionController(service, mapper).create(dto);
            assertThat(captured.list).extracting(event -> event.getFormattedMessage())
                    .containsExactly("Created session 7")
                    .allSatisfy(message -> assertThat(message).doesNotContain(
                            "synthetic-sensitive-hash", "private@example.invalid", "private-session-description"));
        } finally {
            logger.detachAppender(captured);
            captured.stop();
        }
    }
}
