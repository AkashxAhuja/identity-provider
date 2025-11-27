package com.example.accesstoken.repository;

import com.example.accesstoken.model.ApplicationClient;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface ApplicationClientRepository extends JpaRepository<ApplicationClient, Long> {

    Optional<ApplicationClient> findByClientIdAndActiveTrue(String clientId);

    Optional<ApplicationClient> findByStaticTokenHashAndActiveTrue(String staticTokenHash);
}
