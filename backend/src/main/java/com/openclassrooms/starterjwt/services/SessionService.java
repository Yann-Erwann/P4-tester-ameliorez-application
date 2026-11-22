package com.openclassrooms.starterjwt.services;

import com.openclassrooms.starterjwt.dto.SessionSummary;
import com.openclassrooms.starterjwt.exception.BadRequestException;
import com.openclassrooms.starterjwt.exception.NotFoundException;
import com.openclassrooms.starterjwt.models.Session;
import com.openclassrooms.starterjwt.models.User;
import com.openclassrooms.starterjwt.repository.SessionRepository;
import com.openclassrooms.starterjwt.repository.UserRepository;
import org.springframework.stereotype.Service;
import org.springframework.lang.NonNull;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

@Service
public class SessionService {
    private final SessionRepository sessionRepository;

    private final UserRepository userRepository;

    public SessionService(SessionRepository sessionRepository, UserRepository userRepository) {
        this.sessionRepository = sessionRepository;
        this.userRepository = userRepository;
    }

    @PreAuthorize("hasRole('ADMIN')")
    public Session create(Session session) {
        return this.sessionRepository.save(Objects.requireNonNull(session));
    }

    @Transactional
    @PreAuthorize("hasRole('ADMIN')")
    public void delete(Long id) {
        this.sessionRepository.delete(getById(id));
    }

    public List<Session> findAll() {
        return this.sessionRepository.findAll();
    }

    @Transactional(readOnly = true)
    public List<SessionSummary> findSummaries() {
        return sessionRepository.findSummaries();
    }

    @NonNull
    public Session getById(Long id) {
        return Objects.requireNonNull(this.sessionRepository.findById(Objects.requireNonNull(id))
                .orElseThrow(NotFoundException::new));
    }

    @Transactional
    @PreAuthorize("hasRole('ADMIN')")
    public Session update(Long id, Session changes) {
        Session existing = getById(id);
        existing.setName(changes.getName());
        existing.setDescription(changes.getDescription());
        existing.setDate(changes.getDate());
        existing.setTeacher(changes.getTeacher());
        // Membership is managed exclusively by the participation operations.
        return this.sessionRepository.save(existing);
    }

    @Transactional
    @PreAuthorize("isAuthenticated() and #p1 == principal.id")
    public void participate(Long id, Long userId) {
        Session session = this.sessionRepository.findById(Objects.requireNonNull(id)).orElse(null);
        User user = this.userRepository.findById(Objects.requireNonNull(userId)).orElse(null);
        if (session == null || user == null) {
            throw new NotFoundException();
        }

        boolean alreadyParticipate = session.getUsers().stream().anyMatch(o -> o.getId().equals(userId));
        if (alreadyParticipate) {
            throw new BadRequestException();
        }

        session.getUsers().add(user);

        this.sessionRepository.save(session);
    }

    @Transactional
    @PreAuthorize("isAuthenticated() and #p1 == principal.id")
    public void noLongerParticipate(Long id, Long userId) {
        Session session = this.sessionRepository.findById(Objects.requireNonNull(id)).orElse(null);
        if (session == null) {
            throw new NotFoundException();
        }

        boolean alreadyParticipate = session.getUsers().stream().anyMatch(o -> o.getId().equals(userId));
        if (!alreadyParticipate) {
            throw new BadRequestException();
        }

        session.setUsers(session.getUsers().stream().filter(user -> !user.getId().equals(userId)).collect(Collectors.toList()));

        this.sessionRepository.save(session);
    }
}
