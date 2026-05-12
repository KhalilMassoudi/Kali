package kali.microservices.authservice.service;

import kali.microservices.authservice.dto.AuthResponse;
import kali.microservices.authservice.dto.LoginRequest;
import kali.microservices.authservice.dto.RegisterRequest;
import kali.microservices.authservice.dto.UserInfoDto;
import kali.microservices.authservice.entities.User;
import kali.microservices.authservice.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.util.UUID;

@Service
@RequiredArgsConstructor
public class AuthService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final IJwtService jwtService;

    public AuthResponse register(RegisterRequest request) {
        if (userRepository.existsByEmail(request.getEmail())) {
            throw new RuntimeException("Email déjà utilisé");
        }

        User user = new User();
        user.setEmail(request.getEmail());
        user.setPassword(passwordEncoder.encode(request.getPassword()));
        user.setFirstName(request.getFirstName());
        user.setLastName(request.getLastName());
        user.setApiKey(UUID.randomUUID().toString());
        user.setRole(User.Role.USER);

        userRepository.save(user);

        String token = jwtService.generateToken(user.getEmail(), user.getRole().name());
        String redirectTo = resolveRedirect(user.getRole());

        return new AuthResponse(token, user.getRole().name(), redirectTo);
    }

    public AuthResponse login(LoginRequest request) {
        User user = userRepository.findByEmail(request.getEmail())
                .orElseThrow(() -> new RuntimeException("Email ou mot de passe incorrect"));

        if (!passwordEncoder.matches(request.getPassword(), user.getPassword())) {
            throw new RuntimeException("Email ou mot de passe incorrect");
        }

        if (!user.isEnabled()) {
            throw new RuntimeException("Compte désactivé");
        }

        String token = jwtService.generateToken(user.getEmail(), user.getRole().name());
        String redirectTo = resolveRedirect(user.getRole());

        return new AuthResponse(token, user.getRole().name(), redirectTo);
    }

    public boolean validateToken(String token) {
        return jwtService.isTokenValid(token);
    }

    public UserInfoDto getUserInfo(String token) {
        String jwt = token.startsWith("Bearer ") ? token.substring(7) : token;

        if (!jwtService.isTokenValid(jwt)) {
            throw new RuntimeException("Token invalide ou expiré");
        }

        String email = jwtService.extractEmail(jwt);
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new RuntimeException("Utilisateur non trouvé"));

        return new UserInfoDto(
                user.getId(),
                user.getEmail(),
                user.getFirstName(),
                user.getLastName(),
                user.getRole().name()
        );
    }

    public AuthResponse createAdmin(RegisterRequest request, String callerToken) {
        String jwt = callerToken.startsWith("Bearer ") ? callerToken.substring(7) : callerToken;

        if (!jwtService.isTokenValid(jwt)) {
            throw new RuntimeException("Token invalide ou expiré");
        }

        String callerEmail = jwtService.extractEmail(jwt);
        User caller = userRepository.findByEmail(callerEmail)
                .orElseThrow(() -> new RuntimeException("Appelant non trouvé"));

        if (caller.getRole() != User.Role.ADMIN) {
            throw new RuntimeException("Accès refusé: rôle ADMIN requis");
        }

        if (userRepository.existsByEmail(request.getEmail())) {
            throw new RuntimeException("Email déjà utilisé");
        }

        User admin = new User();
        admin.setEmail(request.getEmail());
        admin.setPassword(passwordEncoder.encode(request.getPassword()));
        admin.setFirstName(request.getFirstName());
        admin.setLastName(request.getLastName());
        admin.setApiKey(UUID.randomUUID().toString());
        admin.setRole(User.Role.ADMIN);

        userRepository.save(admin);

        String token = jwtService.generateToken(admin.getEmail(), admin.getRole().name());
        return new AuthResponse(token, admin.getRole().name(), "/admin");
    }

    private String resolveRedirect(User.Role role) {
        return role == User.Role.ADMIN ? "/admin" : "/dashboard";
    }
}