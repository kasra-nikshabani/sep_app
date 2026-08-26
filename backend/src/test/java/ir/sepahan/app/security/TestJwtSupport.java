package ir.sepahan.app.security;

import com.nimbusds.jose.JOSEException;
import com.nimbusds.jose.JWSAlgorithm;
import com.nimbusds.jose.JWSHeader;
import com.nimbusds.jose.crypto.RSASSASigner;
import com.nimbusds.jose.jwk.RSAKey;
import com.nimbusds.jose.jwk.gen.RSAKeyGenerator;
import com.nimbusds.jwt.JWTClaimsSet;
import com.nimbusds.jwt.SignedJWT;
import java.time.Instant;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ThreadLocalRandom;

/**
 * توکن JWT واقعی (امضاشده، نه Mock) برای تست‌های لایه‌ی HTTP -- با یک کلید RSA محلی که فقط
 * برای همین JVM تست ساخته می‌شود، نه یک Keycloak واقعی. {@link TestJwtDecoderConfig} همین کلید
 * را به {@code JwtDecoder} برنامه معرفی می‌کند؛ بقیه‌ی مسیر (اعتبارسنجی امضا/انقضا،
 * {@link ir.sepahan.app.auth.KeycloakRoleConverter}) دقیقاً همان کد واقعی Production است.
 */
public final class TestJwtSupport {

    static final RSAKey RSA_KEY = generateKey();

    private TestJwtSupport() {
    }

    private static RSAKey generateKey() {
        try {
            return new RSAKeyGenerator(2048).keyID("test-key").generate();
        } catch (JOSEException e) {
            throw new IllegalStateException("ساخت کلید RSA تست ناموفق بود", e);
        }
    }

    /** {@code roles} دقیقاً هم‌شکل realm_access.roles واقعی کیکلوک است (بدون پیشوند ROLE_). */
    public static String tokenFor(UUID subject, String... roles) {
        long unique = ThreadLocalRandom.current().nextLong(1_000_000_000L, 9_999_999_999L);
        JWTClaimsSet claims = new JWTClaimsSet.Builder()
                .subject(subject.toString())
                .issueTime(Date.from(Instant.now()))
                .expirationTime(Date.from(Instant.now().plusSeconds(300)))
                .claim("realm_access", Map.of("roles", List.of(roles)))
                .claim("national_code", String.valueOf(unique))
                .claim("phone_number", "09" + String.valueOf(unique).substring(0, 9))
                .build();
        try {
            SignedJWT jwt = new SignedJWT(new JWSHeader.Builder(JWSAlgorithm.RS256).keyID("test-key").build(), claims);
            jwt.sign(new RSASSASigner(RSA_KEY));
            return jwt.serialize();
        } catch (JOSEException e) {
            throw new IllegalStateException("امضای JWT تست ناموفق بود", e);
        }
    }
}
