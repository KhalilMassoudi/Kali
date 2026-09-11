package kali.microservices.authservice.service;

import kali.microservices.authservice.dto.AdminUserDto;
import kali.microservices.authservice.dto.AuthResponse;
import kali.microservices.authservice.dto.ChangePasswordRequest;
import kali.microservices.authservice.dto.LoginRequest;
import kali.microservices.authservice.dto.RegisterRequest;
import kali.microservices.authservice.dto.TwoFactorSetupResponse;
import kali.microservices.authservice.dto.UpdateProfileRequest;
import kali.microservices.authservice.dto.UserInfoDto;
import kali.microservices.authservice.entities.PasswordResetToken;
import kali.microservices.authservice.entities.User;
import kali.microservices.authservice.repository.PasswordResetTokenRepository;
import kali.microservices.authservice.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class AuthService {

    private static final Logger log = LoggerFactory.getLogger(AuthService.class);

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final IJwtService jwtService;
    private final PasswordResetTokenRepository passwordResetTokenRepository;
    private final MailService mailService;
    private final TwoFactorService twoFactorService;

    @Value("${app.frontend-url}")
    private String frontendUrl;

    @Value("${app.password-reset-expiry-minutes}")
    private long resetExpiryMinutes;

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

        String token = jwtService.generateToken(user.getId(), user.getEmail(), user.getRole().name());
        String redirectTo = resolveRedirect(user.getRole());

        return AuthResponse.success(token, user.getRole().name(), redirectTo);
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

        if (user.isTwoFactorEnabled()) {
            String pendingToken = jwtService.generatePendingTwoFactorToken(user.getId(), user.getEmail());
            return AuthResponse.pendingTwoFactor(pendingToken);
        }

        String token = jwtService.generateToken(user.getId(), user.getEmail(), user.getRole().name());
        String redirectTo = resolveRedirect(user.getRole());

        return AuthResponse.success(token, user.getRole().name(), redirectTo);
    }

    public AuthResponse verifyTwoFactorLogin(String pendingToken, String code) {
        Long userId = jwtService.extractPendingTwoFactorUserId(pendingToken);
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("Utilisateur non trouvé"));

        if (!user.isTwoFactorEnabled() || !twoFactorService.verifyCode(user.getTwoFactorSecret(), code)) {
            throw new RuntimeException("Code de vérification erroné");
        }

        String token = jwtService.generateToken(user.getId(), user.getEmail(), user.getRole().name());
        String redirectTo = resolveRedirect(user.getRole());
        return AuthResponse.success(token, user.getRole().name(), redirectTo);
    }

    public TwoFactorSetupResponse setupTwoFactor(String token) {
        User user = resolveUser(token);
        if (user.isTwoFactorEnabled()) {
            throw new RuntimeException("La double authentification est déjà activée");
        }

        TwoFactorService.EnrollmentResult enrollment = twoFactorService.generateEnrollment(user.getEmail());
        user.setTwoFactorSecret(enrollment.encryptedSecret());
        userRepository.save(user);

        return new TwoFactorSetupResponse(enrollment.plainSecret(), enrollment.qrCodeImage());
    }

    public void verifyTwoFactorSetup(String token, String code) {
        User user = resolveUser(token);
        if (user.getTwoFactorSecret() == null) {
            throw new RuntimeException("Aucune configuration 2FA en attente, recommencez");
        }
        if (!twoFactorService.verifyCode(user.getTwoFactorSecret(), code)) {
            throw new RuntimeException("Code de vérification erroné");
        }
        user.setTwoFactorEnabled(true);
        userRepository.save(user);
    }

    public void disableTwoFactor(String token, String password, String code) {
        User user = resolveUser(token);
        if (!passwordEncoder.matches(password, user.getPassword())) {
            throw new RuntimeException("Mot de passe erroné");
        }
        if (!user.isTwoFactorEnabled() || !twoFactorService.verifyCode(user.getTwoFactorSecret(), code)) {
            throw new RuntimeException("Code de vérification erroné");
        }
        user.setTwoFactorEnabled(false);
        user.setTwoFactorSecret(null);
        userRepository.save(user);
    }

    public boolean validateToken(String token) {
        return jwtService.isTokenValid(token);
    }

    public UserInfoDto getUserInfo(String token) {
        return toUserInfoDto(resolveUser(token));
    }

    public UserInfoDto updateProfile(String token, UpdateProfileRequest request) {
        User user = resolveUser(token);
        if (request.getFirstName() != null) {
            user.setFirstName(request.getFirstName().isBlank() ? null : request.getFirstName().trim());
        }
        if (request.getLastName() != null) {
            user.setLastName(request.getLastName().isBlank() ? null : request.getLastName().trim());
        }
        userRepository.save(user);
        return toUserInfoDto(user);
    }

    public void changePassword(String token, ChangePasswordRequest request) {
        User user = resolveUser(token);
        if (!passwordEncoder.matches(request.getCurrentPassword(), user.getPassword())) {
            throw new RuntimeException("Mot de passe actuel incorrect");
        }
        user.setPassword(passwordEncoder.encode(request.getNewPassword()));
        userRepository.save(user);
    }

    public UserInfoDto regenerateApiKey(String token) {
        User user = resolveUser(token);
        user.setApiKey(UUID.randomUUID().toString());
        userRepository.save(user);
        return toUserInfoDto(user);
    }

    public void deactivateAccount(String token) {
        User user = resolveUser(token);
        user.setEnabled(false);
        userRepository.save(user);
    }

    private User resolveUser(String token) {
        String jwt = token.startsWith("Bearer ") ? token.substring(7) : token;

        if (!jwtService.isTokenValid(jwt)) {
            throw new RuntimeException("Token invalide ou expiré");
        }

        String email = jwtService.extractEmail(jwt);
        return userRepository.findByEmail(email)
                .orElseThrow(() -> new RuntimeException("Utilisateur non trouvé"));
    }

    private UserInfoDto toUserInfoDto(User user) {
        return new UserInfoDto(
                user.getId(),
                user.getEmail(),
                user.getFirstName(),
                user.getLastName(),
                user.getRole().name(),
                user.getApiKey(),
                user.getCreatedAt(),
                user.isEnabled(),
                user.isTwoFactorEnabled()
        );
    }

    public User requireAdmin(String token) {
        User user = resolveUser(token);
        if (user.getRole() != User.Role.ADMIN) {
            throw new RuntimeException("Accès refusé: rôle ADMIN requis");
        }
        return user;
    }

    public List<AdminUserDto> listUsers(String token) {
        requireAdmin(token);
        return userRepository.findAll().stream().map(this::toAdminUserDto).toList();
    }

    public AdminUserDto updateUserRole(String token, Long targetId, String role) {
        User admin = requireAdmin(token);
        if (admin.getId().equals(targetId)) {
            throw new RuntimeException("Accès refusé: vous ne pouvez pas modifier votre propre rôle");
        }
        User target = userRepository.findById(targetId)
                .orElseThrow(() -> new RuntimeException("Utilisateur non trouvé"));
        target.setRole(User.Role.valueOf(role.toUpperCase()));
        userRepository.save(target);
        return toAdminUserDto(target);
    }

    public AdminUserDto setUserEnabled(String token, Long targetId, boolean enabled) {
        User admin = requireAdmin(token);
        if (admin.getId().equals(targetId)) {
            throw new RuntimeException("Accès refusé: vous ne pouvez pas modifier votre propre compte");
        }
        User target = userRepository.findById(targetId)
                .orElseThrow(() -> new RuntimeException("Utilisateur non trouvé"));
        target.setEnabled(enabled);
        userRepository.save(target);
        return toAdminUserDto(target);
    }

    public void deleteUser(String token, Long targetId) {
        User admin = requireAdmin(token);
        if (admin.getId().equals(targetId)) {
            throw new RuntimeException("Accès refusé: vous ne pouvez pas supprimer votre propre compte");
        }
        User target = userRepository.findById(targetId)
                .orElseThrow(() -> new RuntimeException("Utilisateur non trouvé"));
        userRepository.delete(target);
    }

    public void forgotPassword(String email) {
        userRepository.findByEmail(email).ifPresent(user -> {
            try {
                String token = UUID.randomUUID().toString();
                PasswordResetToken resetToken = new PasswordResetToken(
                        user.getId(), token, LocalDateTime.now().plusMinutes(resetExpiryMinutes));
                passwordResetTokenRepository.save(resetToken);

                String resetLink = frontendUrl + "/reset-password?token=" + token;
                mailService.sendPasswordResetEmail(user.getEmail(), resetLink);
            } catch (Exception e) {
                log.warn("Échec de l'envoi de l'email de réinitialisation pour {}: {}", email, e.getMessage());
            }
        });
    }

    public void resetPassword(String token, String newPassword) {
        PasswordResetToken resetToken = passwordResetTokenRepository.findByToken(token)
                .orElseThrow(() -> new RuntimeException("Token invalide ou expiré"));

        if (resetToken.isUsed() || resetToken.getExpiresAt().isBefore(LocalDateTime.now())) {
            throw new RuntimeException("Token invalide ou expiré");
        }

        User user = userRepository.findById(resetToken.getUserId())
                .orElseThrow(() -> new RuntimeException("Utilisateur non trouvé"));

        user.setPassword(passwordEncoder.encode(newPassword));
        userRepository.save(user);

        resetToken.setUsed(true);
        passwordResetTokenRepository.save(resetToken);
    }

    private AdminUserDto toAdminUserDto(User user) {
        return new AdminUserDto(
                user.getId(),
                user.getEmail(),
                user.getFirstName(),
                user.getLastName(),
                user.getRole().name(),
                user.isEnabled(),
                user.getCreatedAt()
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

        String token = jwtService.generateToken(admin.getId(), admin.getEmail(), admin.getRole().name());
        return AuthResponse.success(token, admin.getRole().name(), "/admin");
    }

    private String resolveRedirect(User.Role role) {
        return role == User.Role.ADMIN ? "/admin" : "/dashboard";
    }
}