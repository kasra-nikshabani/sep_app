package ir.sepahan.app.auth;

import static org.assertj.core.api.Assertions.assertThat;

import ir.sepahan.app.TestcontainersConfig;
import ir.sepahan.app.security.TestJwtDecoderConfig;
import ir.sepahan.app.security.TestJwtSupport;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.actuate.observability.AutoConfigureObservability;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.SpringBootTest.WebEnvironment;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.context.annotation.Import;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.test.context.ActiveProfiles;

/**
 * تست‌های واقعی لایه‌ی HTTP/Security -- طبق بند ۸ بریف (Security اولویت اول)، شکاف واقعی
 * کشف‌شده در Phase 18 (ADR-0020): تا این فاز هیچ تستی از طریق یک درخواست HTTP واقعی
 * (با فیلترهای امنیتی واقعی درگیر) اجرا نمی‌شد -- تست‌های Service مستقیم متد را صدا می‌زدند و
 * کل زنجیره‌ی Spring Security (JWT، {@link KeycloakRoleConverter}، {@code @PreAuthorize}, CORS،
 * مسیرهای permitAll) را دور می‌زدند.
 *
 * <p>{@code TestRestTemplate} روی یک Port واقعی (RANDOM_PORT) درخواست می‌فرستد -- دقیقاً مثل
 * یک Client واقعی. {@link TestJwtDecoderConfig} تنها چیزی است که جایگزین شده (کلید محلی
 * به‌جای JWKS واقعی کیکلوک)؛ بقیه‌ی مسیر (اعتبارسنجی، نگاشت نقش، RBAC) کد واقعی Production است.
 *
 * <p>{@code @AutoConfigureObservability} لازم است چون {@code @SpringBootTest} به‌طور پیش‌فرض
 * تمام Exporterهای Metrics/Observability (از جمله Prometheus) را غیرفعال می‌کند
 * ({@code ObservabilityContextCustomizerFactory} در {@code spring-boot-test-autoconfigure}) --
 * بدون آن، {@code /actuator/prometheus} حتی با تنظیم درست {@code exposure.include} همیشه ۴۰۴
 * برمی‌گرداند (کشف واقعی همین فاز، نه مستند در جایی که مستقیم پیدا شود).
 */
@SpringBootTest(webEnvironment = WebEnvironment.RANDOM_PORT)
@AutoConfigureObservability
@Import({TestcontainersConfig.class, TestJwtDecoderConfig.class})
@ActiveProfiles("test")
class SecurityRbacIntegrationTest {

    @Autowired
    private TestRestTemplate restTemplate;

    @Test
    void actuatorHealth_isPubliclyAccessible_withoutToken() {
        ResponseEntity<String> response = restTemplate.getForEntity("/actuator/health", String.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
    }

    /** طبق ADR-0019 -- Prometheus بدون Bearer Token اسکرِیپ می‌کند. */
    @Test
    void actuatorPrometheus_isPubliclyAccessible_withoutToken() {
        ResponseEntity<String> response = restTemplate.getForEntity("/actuator/prometheus", String.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
    }

    @Test
    void usersMe_withoutToken_returns401NotSomethingElse() {
        ResponseEntity<String> response = restTemplate.getForEntity("/api/v1/users/me", String.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
    }

    /** همزمان JIT Provisioning (ADR-0008/ADR-0018) را هم از طریق یک درخواست HTTP واقعی اثبات می‌کند. */
    @Test
    void usersMe_withValidFanToken_returns200() {
        String token = TestJwtSupport.tokenFor(UUID.randomUUID(), "fan");

        ResponseEntity<String> response = restTemplate.exchange("/api/v1/users/me", HttpMethod.GET,
                new HttpEntity<>(authHeaders(token)), String.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
    }

    @Test
    void usersAdmin_withFanToken_returns403NotEmptyList() {
        String token = TestJwtSupport.tokenFor(UUID.randomUUID(), "fan");

        ResponseEntity<String> response = restTemplate.exchange("/api/v1/users/admin", HttpMethod.GET,
                new HttpEntity<>(authHeaders(token)), String.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.FORBIDDEN);
    }

    @Test
    void usersAdmin_withAdminToken_returns200() {
        String token = TestJwtSupport.tokenFor(UUID.randomUUID(), "admin");

        ResponseEntity<String> response = restTemplate.exchange("/api/v1/users/admin", HttpMethod.GET,
                new HttpEntity<>(authHeaders(token)), String.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
    }

    /**
     * رگرسیون مستقیم نقصی که در تست واقعی Phase 14 کشف شد: بدون permitAll روی {@code /error}،
     * Forward داخلی Tomcat برای یک ۴۰۴ واقعی دوباره از زنجیره‌ی امنیتی رد می‌شود و با یک ۴۰۱
     * گمراه‌کننده جایگزین می‌شد.
     */
    @Test
    void nonExistentArticle_withValidToken_returnsRealNotFound_not401() {
        String token = TestJwtSupport.tokenFor(UUID.randomUUID(), "fan");

        ResponseEntity<String> response = restTemplate.exchange("/api/v1/news/articles/does-not-exist-slug",
                HttpMethod.GET, new HttpEntity<>(authHeaders(token)), String.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
    }

    /** طبق ADR-0017 -- فقط برای نسخه‌ی Web اپ موبایل؛ Origin پیش‌فرض بدون هیچ Env Var. */
    @Test
    void corsPreflight_fromMobileWebOrigin_isAllowed() {
        HttpHeaders headers = new HttpHeaders();
        headers.set(HttpHeaders.ORIGIN, "http://localhost:8082");
        headers.set(HttpHeaders.ACCESS_CONTROL_REQUEST_METHOD, "GET");

        ResponseEntity<String> response = restTemplate.exchange("/api/v1/users/me", HttpMethod.OPTIONS,
                new HttpEntity<>(headers), String.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getHeaders().getFirst("Access-Control-Allow-Origin")).isEqualTo("http://localhost:8082");
    }

    @Test
    void corsPreflight_fromUnknownOrigin_isRejected() {
        HttpHeaders headers = new HttpHeaders();
        headers.set(HttpHeaders.ORIGIN, "https://evil.example.com");
        headers.set(HttpHeaders.ACCESS_CONTROL_REQUEST_METHOD, "GET");

        ResponseEntity<String> response = restTemplate.exchange("/api/v1/users/me", HttpMethod.OPTIONS,
                new HttpEntity<>(headers), String.class);

        assertThat(response.getHeaders().getFirst("Access-Control-Allow-Origin")).isNull();
    }

    private HttpHeaders authHeaders(String token) {
        HttpHeaders headers = new HttpHeaders();
        headers.setBearerAuth(token);
        return headers;
    }
}
