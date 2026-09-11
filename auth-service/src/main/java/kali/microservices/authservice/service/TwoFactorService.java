package kali.microservices.authservice.service;

import dev.samstevens.totp.code.CodeVerifier;
import dev.samstevens.totp.code.DefaultCodeGenerator;
import dev.samstevens.totp.code.DefaultCodeVerifier;
import dev.samstevens.totp.exceptions.QrGenerationException;
import dev.samstevens.totp.qr.QrData;
import dev.samstevens.totp.qr.QrGenerator;
import dev.samstevens.totp.qr.ZxingPngQrGenerator;
import dev.samstevens.totp.secret.DefaultSecretGenerator;
import dev.samstevens.totp.secret.SecretGenerator;
import dev.samstevens.totp.time.SystemTimeProvider;
import kali.microservices.authservice.security.CryptoUtil;
import org.springframework.stereotype.Service;

import java.util.Base64;

/** RFC 6238 TOTP: secret generation, QR enrollment codes, and code verification. */
@Service
public class TwoFactorService {

    private static final String ISSUER = "Safozi Cloud";

    private final CryptoUtil cryptoUtil;
    private final SecretGenerator secretGenerator = new DefaultSecretGenerator();
    private final QrGenerator qrGenerator = new ZxingPngQrGenerator();
    private final CodeVerifier codeVerifier = new DefaultCodeVerifier(new DefaultCodeGenerator(), new SystemTimeProvider());

    public TwoFactorService(CryptoUtil cryptoUtil) {
        this.cryptoUtil = cryptoUtil;
    }

    public record EnrollmentResult(String plainSecret, String encryptedSecret, String qrCodeImage) {}

    public EnrollmentResult generateEnrollment(String userEmail) {
        String secret = secretGenerator.generate();
        String qrCodeImage = renderQrCode(userEmail, secret);
        return new EnrollmentResult(secret, cryptoUtil.encrypt(secret), qrCodeImage);
    }

    public boolean verifyCode(String encryptedSecret, String code) {
        if (encryptedSecret == null || code == null) return false;
        String secret = cryptoUtil.decrypt(encryptedSecret);
        return codeVerifier.isValidCode(secret, code);
    }

    private String renderQrCode(String userEmail, String secret) {
        QrData data = new QrData.Builder()
                .label(userEmail)
                .secret(secret)
                .issuer(ISSUER)
                .algorithm(dev.samstevens.totp.code.HashingAlgorithm.SHA1)
                .digits(6)
                .period(30)
                .build();
        try {
            byte[] imageBytes = qrGenerator.generate(data);
            return "data:image/png;base64," + Base64.getEncoder().encodeToString(imageBytes);
        } catch (QrGenerationException e) {
            throw new RuntimeException("Erreur de génération du QR code", e);
        }
    }
}
