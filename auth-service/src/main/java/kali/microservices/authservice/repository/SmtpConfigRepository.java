package kali.microservices.authservice.repository;

import kali.microservices.authservice.entities.SmtpConfig;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface SmtpConfigRepository extends JpaRepository<SmtpConfig, Long> {
}