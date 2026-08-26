package ir.sepahan.app.auth;

import java.util.Arrays;
import java.util.List;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationConverter;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

/**
 * توکن Bearer صادرشده توسط Keycloak Realm «sepahan» را اعتبارسنجی می‌کند (ADR-0003).
 * بدون Session — این یک Resource Server کاملاً Stateless است.
 *
 * EnableMethodSecurity: از Phase 8 به بعد Controllerها از @PreAuthorize("hasRole('admin')")
 * طبق docs/authentication/rbac-matrix.md استفاده می‌کنند (نقش‌ها با پیشوند ROLE_ در
 * KeycloakRoleConverter نگاشت شده‌اند).
 */
@Configuration
@EnableMethodSecurity
public class SecurityConfig {

    @Value("${sepahan.cors.allowed-origins}")
    private String[] corsAllowedOrigins;

    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        http.csrf(AbstractHttpConfigurer::disable)
                .cors(cors -> cors.configurationSource(corsConfigurationSource()))
                .sessionManagement(s -> s.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .authorizeHttpRequests(auth -> auth
                        .requestMatchers("/actuator/health", "/actuator/health/**").permitAll()
                        // Prometheus از داخل Docker Compose این مسیر را بدون Bearer Token اسکرِیپ
                        // می‌کند (خودش یک کاربر Keycloak نیست) -- به‌جای permitAll مطلق (TODO امنیتی
                        // صریح از Phase 17)، از Phase 19 فقط از شبکه‌ی خصوصی/Loopback در دسترس است
                        // (جزئیات/تست در MonitoringNetworkAuthorizationManager).
                        .requestMatchers("/actuator/prometheus").access(new MonitoringNetworkAuthorizationManager())
                        // Zibal مرورگر کاربر را مستقیم به این مسیر Redirect می‌کند (بدون Bearer Token) --
                        // امنیت واقعی از طریق Verify سرور-به-سرور در PaymentService تأمین می‌شود، نه Auth این مسیر (ADR-0011)
                        .requestMatchers("/api/v1/payments/*/callback").permitAll()
                        // تصاویر خبر باید در تگ <img> مرورگر بدون هیچ Header سفارشی بارگذاری شوند --
                        // مرورگر برای <img src> هرگز Authorization Header نمی‌فرستد؛ محتوا هم صرفاً
                        // فایل رسانه‌ی عمومی است، نه داده‌ی حساس (ADR-0013)
                        .requestMatchers("/media/**").permitAll()
                        // یک Partner خارجی کاربر Fan ID نیست -- با کلید API خودش احراز هویت می‌شود، نه
                        // Keycloak؛ تأیید دستی داخل PartnerSelfController انجام می‌شود (ADR-0015)
                        .requestMatchers("/api/v1/partners/me").permitAll()
                        // بدون این خط، وقتی یک Controller اجازه‌دار (بالا) خطایی پرتاب کند (مثلاً 404)،
                        // Forward داخلی Tomcat به /error دوباره از این زنجیره‌ی امنیتی رد می‌شود و چون
                        // /error مجاز نیست، پاسخ واقعی (404) با یک 401 گمراه‌کننده جایگزین می‌شود --
                        // در تست واقعی همین فاز کشف شد.
                        .requestMatchers("/error").permitAll()
                        .anyRequest().authenticated())
                .oauth2ResourceServer(oauth2 -> oauth2.jwt(jwt -> jwt.jwtAuthenticationConverter(jwtAuthenticationConverter())));
        return http.build();
    }

    private JwtAuthenticationConverter jwtAuthenticationConverter() {
        JwtAuthenticationConverter converter = new JwtAuthenticationConverter();
        converter.setJwtGrantedAuthoritiesConverter(new KeycloakRoleConverter());
        return converter;
    }

    /**
     * فقط برای نسخه‌ی Web اپ موبایل (Preview/تست -- ADR-0017) لازم است؛ Admin
     * Panel نیازی ندارد چون الگوی BFF دارد (مرورگر هرگز مستقیم این API را
     * صدا نمی‌زند). بدون withCredentials/Cookie -- فقط Bearer Token در Header،
     * پس Origin مشخص (نه Wildcard) کافی و امن است.
     */
    private CorsConfigurationSource corsConfigurationSource() {
        CorsConfiguration configuration = new CorsConfiguration();
        configuration.setAllowedOrigins(Arrays.asList(corsAllowedOrigins));
        configuration.setAllowedMethods(List.of("GET", "POST", "PUT", "DELETE", "OPTIONS"));
        configuration.setAllowedHeaders(List.of("Authorization", "Content-Type"));
        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/api/v1/**", configuration);
        return source;
    }
}
