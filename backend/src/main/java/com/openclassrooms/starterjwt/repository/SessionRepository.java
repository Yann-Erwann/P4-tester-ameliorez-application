package com.openclassrooms.starterjwt.repository;

import com.openclassrooms.starterjwt.dto.SessionSummary;
import com.openclassrooms.starterjwt.models.Session;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.List;

public interface SessionRepository extends JpaRepository<Session, Long> {
    @Override
    @EntityGraph(attributePaths = {"teacher", "users"})
    List<Session> findAll();

    @Query("""
            select new com.openclassrooms.starterjwt.dto.SessionSummary(s.id, s.name, s.date, s.description)
            from Session s order by s.id
            """)
    List<SessionSummary> findSummaries();

    List<Session> findAllByUsers_Id(Long userId);
}
