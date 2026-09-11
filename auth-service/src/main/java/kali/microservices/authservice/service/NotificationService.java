package kali.microservices.authservice.service;

import kali.microservices.authservice.dto.NotifyUserRequest;
import kali.microservices.authservice.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

/**
 * Lets another admin-gated caller (e.g. support-service, on behalf of an agent replying to a
 * ticket) notify a user by id without needing to know their email — only auth-service's DB has
 * that. Never throws on an unresolved user or a send failure; both just silently no-op.
 */
@Service
@RequiredArgsConstructor
public class NotificationService {

    private final AuthService authService;
    private final UserRepository userRepository;
    private final MailService mailService;

    public void notifyUser(String token, NotifyUserRequest request) {
        authService.requireAdmin(token);
        userRepository.findById(request.getUserId())
                .ifPresent(user -> mailService.sendNotification(user.getEmail(), request.getSubject(), request.getBody()));
    }
}
