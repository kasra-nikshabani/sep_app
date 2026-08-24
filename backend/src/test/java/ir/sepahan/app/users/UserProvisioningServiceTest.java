package ir.sepahan.app.users;

import static org.assertj.core.api.Assertions.assertThat;

import ir.sepahan.app.fan.FanProfileRepository;
import java.time.Instant;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.oauth2.jwt.Jwt;

/**
 * به Postgres واقعی نیاز دارد (هم‌الگوی ReservationServiceTest -- ADR-0010). هدف: اثبات رفتار
 * واقعی JIT Provisioning تحت هم‌زمانی واقعی، دقیقاً همان بار مسابقه‌ای که اپ موبایل (Phase 15)
 * با چند فراخوانی هم‌زمان API در اولین ورود یک کاربر تازه تولید می‌کند.
 *
 * <p>این تست یک نقص واقعی را که در تست زنده‌ی Phase 16 کشف شد بازتولید می‌کند: بدون رفع اضافه‌شده
 * در همین فاز، نیمی از درخواست‌های هم‌زمان به‌جای برگرداندن رکورد برنده‌ی مسابقه، با یک 500 واقعی
 * (SQLState=25P02 -- تلاش برای Query در یک Transaction از قبل Aborted) شکست می‌خوردند.
 */
@SpringBootTest
class UserProvisioningServiceTest {

    @Autowired
    private UserProvisioningService userProvisioningService;
    @Autowired
    private AppUserRepository appUserRepository;
    @Autowired
    private FanProfileRepository fanProfileRepository;

    @AfterEach
    void tearDown() {
        fanProfileRepository.deleteAll();
        appUserRepository.deleteAll();
    }

    @Test
    void ensureUserForToken_concurrentFirstLogin_allCallsSucceedWithSameUser() throws InterruptedException {
        UUID keycloakSubject = UUID.randomUUID();
        Jwt jwt = Jwt.withTokenValue("test-token")
                .header("alg", "none")
                .claim("sub", keycloakSubject.toString())
                .claim("national_code", "1111111111")
                .claim("phone_number", "09120000000")
                .issuedAt(Instant.now())
                .expiresAt(Instant.now().plusSeconds(300))
                .build();

        // ۴، نه بیشتر: هر Thread حین ensureUserForToken تا دو Connection هم‌زمان لازم دارد (یکی
        // برای Transaction بیرونی، یکی برای JitInsertHelper.insertUser با REQUIRES_NEW واقعی) --
        // با Pool پیش‌فرض HikariCP (اندازه‌ی ۱۰)، تعداد بیشتر خودِ تست را (نه کد Production را) به
        // Connection Timeout می‌رساند؛ عدد واقعی بار مسابقه‌ی این باگ («چند تماس هم‌زمان اپ موبایل
        // در اولین ورود») هم ۲ تا ۳ تماس بود، نه ده‌ها.
        int attempts = 4;
        ExecutorService pool = Executors.newFixedThreadPool(attempts);
        CountDownLatch startLine = new CountDownLatch(1);
        CountDownLatch finishLine = new CountDownLatch(attempts);
        List<AppUser> results = new CopyOnWriteArrayList<>();
        List<Throwable> failures = new CopyOnWriteArrayList<>();

        for (int i = 0; i < attempts; i++) {
            pool.submit(() -> {
                try {
                    startLine.await();
                    results.add(userProvisioningService.ensureUserForToken(jwt));
                } catch (Throwable t) {
                    failures.add(t);
                } finally {
                    finishLine.countDown();
                }
            });
        }

        startLine.countDown(); // همه‌ی Threadها تقریباً هم‌زمان شروع کنند
        boolean finished = finishLine.await(30, TimeUnit.SECONDS);
        pool.shutdown();

        assertThat(finished).as("تمام تلاش‌ها باید در بازه‌ی زمانی معقول تمام شوند").isTrue();
        assertThat(failures).as("هیچ فراخوانی نباید Exception پرتاب کند -- دقیقاً همان نقصی که در Phase 16 کشف و رفع شد").isEmpty();
        assertThat(results).hasSize(attempts);

        Set<UUID> distinctUserIds = results.stream().map(AppUser::getId).collect(java.util.stream.Collectors.toSet());
        assertThat(distinctUserIds).as("همه باید دقیقاً همان رکورد برنده‌ی مسابقه را برگردانند").hasSize(1);

        assertThat(appUserRepository.findByKeycloakSubjectAndDeletedAtIsNull(keycloakSubject)).isPresent();
        assertThat(fanProfileRepository.findByUserIdAndDeletedAtIsNull(distinctUserIds.iterator().next())).isPresent();
    }
}
