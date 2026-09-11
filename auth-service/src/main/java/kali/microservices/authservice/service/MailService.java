package kali.microservices.authservice.service;

import kali.microservices.authservice.entities.SmtpConfig;
import kali.microservices.authservice.repository.SmtpConfigRepository;
import kali.microservices.authservice.security.CryptoUtil;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.mail.MailException;
import org.springframework.mail.javamail.JavaMailSenderImpl;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Service;

import jakarta.mail.internet.MimeMessage;
import java.util.Properties;

@Slf4j
@Service
@RequiredArgsConstructor
public class MailService {

    private final SmtpConfigRepository smtpConfigRepository;
    private final CryptoUtil cryptoUtil;

    public void sendTestEmail(String recipient) {
        SmtpConfig config = smtpConfigRepository.findById(1L)
                .orElseThrow(() -> new RuntimeException("Aucune configuration SMTP enregistrée"));
        send(config, recipient, "Test SMTP - Safozi", "Ceci est un email de test envoyé depuis le panneau d'administration Safozi.");
    }

    public void sendPasswordResetEmail(String toAddress, String resetLink) {
        SmtpConfig config = smtpConfigRepository.findById(1L)
                .orElseThrow(() -> new RuntimeException("Aucune configuration SMTP enregistrée"));
        String body = "Vous avez demandé la réinitialisation de votre mot de passe.\n\n"
                + "Cliquez sur le lien suivant pour choisir un nouveau mot de passe (valide 30 minutes) :\n"
                + resetLink + "\n\n"
                + "Si vous n'êtes pas à l'origine de cette demande, ignorez cet email.";
        send(config, toAddress, "Réinitialisation de votre mot de passe - Safozi", body);
    }

    /**
     * Background/cross-service notifications (e.g. support-ticket updates): unlike the
     * admin-triggered methods above, this must never throw — a missing SMTP config or a
     * delivery failure should just be skipped, not fail whatever triggered the notification.
     */
    public boolean sendNotification(String toAddress, String subject, String body) {
        return smtpConfigRepository.findById(1L)
                .map(config -> {
                    try {
                        send(config, toAddress, subject, body);
                        return true;
                    } catch (RuntimeException e) {
                        log.warn("Notification email to {} failed: {}", toAddress, e.getMessage());
                        return false;
                    }
                })
                .orElse(false);
    }

    private void send(SmtpConfig config, String to, String subject, String body) {
        try {
            JavaMailSenderImpl sender = buildSender(config);
            MimeMessage message = sender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message, false, "UTF-8");
            String from = config.getFromName() != null && !config.getFromName().isBlank()
                    ? config.getFromName() + " <" + config.getFromAddress() + ">"
                    : config.getFromAddress();
            helper.setFrom(from);
            helper.setTo(to);
            helper.setSubject(subject);
            helper.setText(body, false);
            sender.send(message);
        } catch (MailException e) {
            String reason = e.getMostSpecificCause() != null ? e.getMostSpecificCause().getMessage() : e.getMessage();
            throw new RuntimeException("Échec de l'envoi de l'email: " + reason);
        } catch (jakarta.mail.MessagingException e) {
            throw new RuntimeException("Échec de l'envoi de l'email: " + e.getMessage());
        }
    }

    private JavaMailSenderImpl buildSender(SmtpConfig config) {
        JavaMailSenderImpl sender = new JavaMailSenderImpl();
        sender.setHost(config.getHost());
        sender.setPort(config.getPort());
        sender.setUsername(config.getUsername());
        if (config.getEncryptedPassword() != null) {
            sender.setPassword(cryptoUtil.decrypt(config.getEncryptedPassword()));
        }

        Properties props = sender.getJavaMailProperties();
        props.put("mail.transport.protocol", "smtp");
        props.put("mail.smtp.auth", String.valueOf(config.isAuthEnabled()));

        SmtpConfig.Encryption encryption = config.getEncryption();
        props.put("mail.smtp.starttls.enable", String.valueOf(encryption == SmtpConfig.Encryption.STARTTLS));
        if (encryption == SmtpConfig.Encryption.SSL) {
            props.put("mail.smtp.ssl.enable", "true");
        }
        props.put("mail.smtp.connectiontimeout", "5000");
        props.put("mail.smtp.timeout", "5000");
        props.put("mail.smtp.writetimeout", "5000");

        return sender;
    }
}
