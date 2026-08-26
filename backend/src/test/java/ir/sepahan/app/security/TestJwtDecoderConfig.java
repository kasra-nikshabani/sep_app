package ir.sepahan.app.security;

import com.nimbusds.jose.JOSEException;
import java.security.interfaces.RSAPublicKey;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.jwt.NimbusJwtDecoder;

/**
 * جایگزینی {@code JwtDecoder} پیش‌فرض (که در Production از JWKS واقعی Keycloak می‌خواند) با
 * یک نسخه‌ی محلی که کلید عمومی {@link TestJwtSupport} را می‌شناسد -- وجود این Bean باعث
 * می‌شود Auto-Configuration اسپرینگ‌بوت اصلاً تلاش نکند به Keycloak (که در این تست‌ها بالا
 * نیست) وصل شود.
 */
@TestConfiguration(proxyBeanMethods = false)
public class TestJwtDecoderConfig {

    @Bean
    JwtDecoder jwtDecoder() throws JOSEException {
        RSAPublicKey publicKey = TestJwtSupport.RSA_KEY.toRSAPublicKey();
        return NimbusJwtDecoder.withPublicKey(publicKey).build();
    }
}
