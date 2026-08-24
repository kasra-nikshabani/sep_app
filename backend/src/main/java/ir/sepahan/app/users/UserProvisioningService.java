package ir.sepahan.app.users;

import ir.sepahan.app.fan.FanProfile;
import ir.sepahan.app.fan.FanProfileRepository;
import ir.sepahan.app.observability.JitRaceMetrics;
import java.util.UUID;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * معادل سمت Spring Boot تصمیم ADR-0008 (که برای Django نوشته شده بود):
 * اولین باری که یک Bearer Token معتبر Keycloak به این Backend می‌رسد، اگر AppUser/FanProfile
 * متناظرش وجود نداشته باشد، همین‌جا ساخته می‌شود — بدون گام دستی جدا.
 *
 * <p>Insert واقعی (با ایزوله‌سازی REQUIRES_NEW) عمداً در {@link JitInsertHelper} (یک Bean جدا)
 * انجام می‌شود -- دلیل کامل در Javadoc همان کلاس (نقص Self-Invocation، کشف Phase 16).
 */
@Service
public class UserProvisioningService {

    private final AppUserRepository appUserRepository;
    private final FanProfileRepository fanProfileRepository;
    private final JitInsertHelper jitInsertHelper;
    private final JitRaceMetrics jitRaceMetrics;

    public UserProvisioningService(AppUserRepository appUserRepository, FanProfileRepository fanProfileRepository,
                                    JitInsertHelper jitInsertHelper, JitRaceMetrics jitRaceMetrics) {
        this.appUserRepository = appUserRepository;
        this.fanProfileRepository = fanProfileRepository;
        this.jitInsertHelper = jitInsertHelper;
        this.jitRaceMetrics = jitRaceMetrics;
    }

    @Transactional
    public AppUser ensureUserForToken(Jwt jwt) {
        UUID keycloakSubject = UUID.fromString(jwt.getSubject());

        AppUser appUser = appUserRepository.findByKeycloakSubjectAndDeletedAtIsNull(keycloakSubject)
                .orElseGet(() -> {
                    // دو درخواست هم‌زمان اولین ورود یک کاربر کاملاً جدید ممکن است هر دو
                    // findBy...IsNull را خالی ببینند و هر دو تلاش کنند رکورد بسازند؛ Unique Index
                    // دیتابیس (نه این کد) مانع تکرار می‌شود.
                    try {
                        return jitInsertHelper.insertUser(jwt, keycloakSubject);
                    } catch (DataIntegrityViolationException raceLost) {
                        jitRaceMetrics.recordConflict("app_user");
                        return appUserRepository.findByKeycloakSubjectAndDeletedAtIsNull(keycloakSubject)
                                .orElseThrow(() -> raceLost);
                    }
                });

        fanProfileRepository.findByUserIdAndDeletedAtIsNull(appUser.getId())
                .orElseGet(() -> {
                    try {
                        return jitInsertHelper.insertFanProfile(appUser.getId());
                    } catch (DataIntegrityViolationException raceLost) {
                        jitRaceMetrics.recordConflict("fan_profile");
                        return fanProfileRepository.findByUserIdAndDeletedAtIsNull(appUser.getId())
                                .orElseThrow(() -> raceLost);
                    }
                });

        return appUser;
    }
}
