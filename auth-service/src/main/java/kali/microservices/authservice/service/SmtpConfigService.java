package kali.microservices.authservice.service;

import kali.microservices.authservice.dto.SmtpConfigDto;
import kali.microservices.authservice.dto.TestEmailRequest;
import kali.microservices.authservice.dto.UpdateSmtpConfigRequest;
import kali.microservices.authservice.entities.SmtpConfig;
import kali.microservices.authservice.repository.SmtpConfigRepository;
import kali.microservices.authservice.security.CryptoUtil;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;

@Service
@RequiredArgsConstructor
public class SmtpConfigService {

    private final SmtpConfigRepository smtpConfigRepository;
    private final CryptoUtil cryptoUtil;
    private final AuthService authService;
    private final MailService mailService;

    public SmtpConfigDto getConfig(String token) {
        authService.requireAdmin(token);
        SmtpConfig config = smtpConfigRepository.findById(1L).orElseGet(this::emptyConfig);
        return toDto(config);
    }

    public SmtpConfigDto updateConfig(String token, UpdateSmtpConfigRequest request) {
        authService.requireAdmin(token);
        SmtpConfig config = smtpConfigRepository.findById(1L).orElseGet(this::emptyConfig);

        config.setHost(request.getHost());
        config.setPort(request.getPort());
        config.setUsername(request.getUsername());
        config.setFromAddress(request.getFromAddress());
        config.setFromName(request.getFromName());
        config.setAuthEnabled(request.isAuthEnabled());
        config.setEncryption(SmtpConfig.Encryption.valueOf(request.getEncryption().toUpperCase()));
        if (request.getPassword() != null && !request.getPassword().isBlank()) {
            config.setEncryptedPassword(cryptoUtil.encrypt(request.getPassword()));
        }
        config.setUpdatedAt(LocalDateTime.now());

        smtpConfigRepository.save(config);
        return toDto(config);
    }

    public void sendTestEmail(String token, TestEmailRequest request) {
        authService.requireAdmin(token);
        mailService.sendTestEmail(request.getRecipient());
    }

    private SmtpConfig emptyConfig() {
        SmtpConfig config = new SmtpConfig();
        config.setId(1L);
        return config;
    }

    private SmtpConfigDto toDto(SmtpConfig config) {
        return new SmtpConfigDto(
                config.getHost(),
                config.getPort(),
                config.getUsername(),
                config.getEncryptedPassword() != null,
                config.getFromAddress(),
                config.getFromName(),
                config.isAuthEnabled(),
                config.getEncryption().name(),
                config.getUpdatedAt()
        );
    }
}
