package ir.sepahan.app.partners;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.util.Base64;
import java.util.Optional;
import org.springframework.stereotype.Service;

/**
 * تولید/تأیید کلید API هر Partner. کلید خام فقط یک‌بار، در لحظه‌ی ساخت، برمی‌گردد و هرگز
 * در دیتابیس ذخیره نمی‌شود -- فقط SHA-256 آن (طبق ADR-0015، دلیل انتخاب SHA-256 به‌جای
 * bcrypt در کامنت Migration V10 مستند شده: کلید پرآنتروپی است، نه رمز عبور کاربر).
 */
@Service
public class PartnerService {

    private final PartnerRepository partnerRepository;
    private final SecureRandom secureRandom = new SecureRandom();

    public PartnerService(PartnerRepository partnerRepository) {
        this.partnerRepository = partnerRepository;
    }

    public record CreatedPartner(Partner partner, String rawApiKey) {
    }

    public CreatedPartner createPartner(String name, String slug, String contactEmail, String contactPhone) {
        String rawKey = generateRawKey();
        Partner partner = partnerRepository.save(new Partner(name, slug, contactEmail, contactPhone, hash(rawKey)));
        return new CreatedPartner(partner, rawKey);
    }

    public Optional<Partner> authenticate(String rawApiKey) {
        if (rawApiKey == null || rawApiKey.isBlank()) {
            return Optional.empty();
        }
        return partnerRepository.findByApiKeyHashAndActiveTrueAndDeletedAtIsNull(hash(rawApiKey));
    }

    private String generateRawKey() {
        byte[] bytes = new byte[32];
        secureRandom.nextBytes(bytes);
        return "pk_" + Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
    }

    private String hash(String rawKey) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hashed = digest.digest(rawKey.getBytes(StandardCharsets.UTF_8));
            StringBuilder hex = new StringBuilder();
            for (byte b : hashed) {
                hex.append(String.format("%02x", b));
            }
            return hex.toString();
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException("SHA-256 در دسترس نیست", e);
        }
    }
}
