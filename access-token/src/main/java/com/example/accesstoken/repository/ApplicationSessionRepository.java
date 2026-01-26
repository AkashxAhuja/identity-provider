package com.example.accesstoken.repository;

import com.example.accesstoken.model.ApplicationSession;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface ApplicationSessionRepository extends JpaRepository<ApplicationSession, Long> {

    Optional<ApplicationSession> findByTokenAndActiveTrue(String token);

    Optional<ApplicationSession> findByToken(String token);
}
