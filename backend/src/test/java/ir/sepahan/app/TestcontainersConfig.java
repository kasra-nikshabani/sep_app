package ir.sepahan.app;

import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.context.annotation.Bean;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.utility.DockerImageName;

/**
 * Postgres/Redis موقت و ایزوله برای هر اجرای تست -- جایگزین تصمیم قبلی (وصل‌شدن مستقیم به
 * Stack مشترک {@code infra/docker-compose.yml} روی پورت‌های ثابت با Env Var دستی، طبق
 * ReservationServiceTest از Phase 8). {@code @ServiceConnection} خودکار
 * {@code spring.datasource.*}/{@code spring.data.redis.*} را با جزئیات این دو Container
 * جایگزین می‌کند -- بدون نیاز به {@code @DynamicPropertySource} دستی (Phase 18، ADR-0020).
 *
 * <p>نسخه‌ی Image عمداً هم‌سطح {@code infra/docker-compose.yml} است تا رفتار Postgres/Redis
 * بین تست و توسعه‌ی محلی یکسان بماند.
 */
@TestConfiguration(proxyBeanMethods = false)
public class TestcontainersConfig {

    @Bean
    @ServiceConnection
    PostgreSQLContainer<?> postgresContainer() {
        return new PostgreSQLContainer<>(DockerImageName.parse("postgres:16-alpine"));
    }

    @Bean
    @ServiceConnection(name = "redis")
    GenericContainer<?> redisContainer() {
        return new GenericContainer<>(DockerImageName.parse("redis:7-alpine")).withExposedPorts(6379);
    }
}
