package com.openclassrooms.starterjwt.repository;

import com.openclassrooms.starterjwt.dto.SessionSummary;
import com.openclassrooms.starterjwt.models.Session;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.lang.NonNull;

import java.util.List;
import java.util.Optional;

public interface SessionRepository extends JpaRepository<Session, Long> {
    @Override
    @NonNull
    @EntityGraph(attributePaths = {"teacher", "users"})
    List<Session> findAll();

    @Query("""
            select new com.openclassrooms.starterjwt.dto.SessionSummary(s.id, s.name, s.date, s.description)
            from Session s order by s.id
            """)
    List<SessionSummary> findSummaries();

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select s from Session s where s.id = :id")
    Optional<Session> findForParticipation(@Param("id") Long id);

    List<Session> findAllByUsers_Id(Long userId);
}
