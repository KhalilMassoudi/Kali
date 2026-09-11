package kali.microservices.authservice.config;

import kali.microservices.authservice.entities.User;
import kali.microservices.authservice.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class BootstrapAdminRunner implements CommandLineRunner {

    private static final Logger log = LoggerFactory.getLogger(BootstrapAdminRunner.class);

    private final UserRepository userRepository;

    @Value("${app.bootstrap-admin-email:}")
    private String bootstrapAdminEmail;

    @Override
    public void run(String... args) {
        if (bootstrapAdminEmail == null || bootstrapAdminEmail.isBlank()) {
            return;
        }

        userRepository.findByEmail(bootstrapAdminEmail).ifPresentOrElse(user -> {
            if (user.getRole() != User.Role.ADMIN) {
                user.setRole(User.Role.ADMIN);
                userRepository.save(user);
                log.info("Bootstrap: {} promu ADMIN", bootstrapAdminEmail);
            }
        }, () -> log.warn("Bootstrap: app.bootstrap-admin-email={} ne correspond à aucun utilisateur existant", bootstrapAdminEmail));
    }
}
