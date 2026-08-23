package ir.sepahan.app.users;

import ir.sepahan.app.fan.FanProfile;
import ir.sepahan.app.fan.FanProfileRepository;
import java.util.UUID;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

/**
 * معادل سمت Spring Boot تصمیم ADR-0008 (که برای Django نوشته شده بود):
 * اولین باری که یک Bearer Token معتبر Keycloak به این Backend می‌رسد، اگر AppUser/FanProfile
 * متناظرش وجود نداشته باشد، همین‌جا ساخته می‌شود — بدون گام دستی جدا.
 */
@Service
public class UserProvisioningService {

    private final AppUserRepository appUserRepository;
    private final FanProfileRepository fanProfileRepository;

    public UserProvisioningService(AppUserRepository appUserRepository, FanProfileRepository fanProfileRepository) {
        this.appUserRepository = appUserRepository;
        this.fanProfileRepository = fanProfileRepository;
    }

    @Transactional
    public AppUser ensureUserForToken(Jwt jwt) {
        UUID keycloakSubject = UUID.fromString(jwt.getSubject());

        AppUser appUser = appUserRepository.findByKeycloakSubjectAndDeletedAtIsNull(keycloakSubject)
                .orElseGet(() -> createUserSafely(jwt, keycloakSubject));

        fanProfileRepository.findByUserIdAndDeletedAtIsNull(appUser.getId())
                .orElseGet(() -> createFanProfileSafely(appUser.getId()));

        return appUser;
    }

    /**
     * دو درخواست هم‌زمان اولین ورود یک کاربر کاملاً جدید ممکن است هر دو findBy...IsNull را خالی ببینند
     * و هر دو تلاش کنند رکورد بسازند؛ Unique Index دیتابیس (نه این کد) مانع تکرار می‌شود — این‌جا فقط
     * آن خطای دیتابیسی را می‌گیریم و به‌جای شکست، رکوردی که درخواست موازی ساخته را برمی‌گردانیم.
     *
     * saveAndFlush عمداً به‌جای save: JPA معمولاً INSERT را تا لحظه‌ی Commit
     * تراکنش به تعویق می‌اندازد -- یعنی DataIntegrityViolationException بدون
     * Flush صریح بعد از خروج از همین متد (بیرون try/catch) پرتاب می‌شد و اصلاً
     * گرفته نمی‌شد (کشف واقعی در Phase 15: دو تماس هم‌زمان اولین ورود اپ
     * موبایل -- /users/me و /loyalty/account -- دقیقاً همین بار مسابقه را
     * برای اولین بار به‌طور واقعی رخ داد).
     */
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    protected AppUser createUserSafely(Jwt jwt, UUID keycloakSubject) {
        try {
            String nationalCode = jwt.getClaimAsString("national_code");
            String phoneNumber = jwt.getClaimAsString("phone_number");
            return appUserRepository.saveAndFlush(new AppUser(keycloakSubject, nationalCode, phoneNumber));
        } catch (DataIntegrityViolationException raceLost) {
            return appUserRepository.findByKeycloakSubjectAndDeletedAtIsNull(keycloakSubject)
                    .orElseThrow(() -> raceLost);
        }
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    protected FanProfile createFanProfileSafely(UUID userId) {
        try {
            return fanProfileRepository.saveAndFlush(new FanProfile(userId));
        } catch (DataIntegrityViolationException raceLost) {
            return fanProfileRepository.findByUserIdAndDeletedAtIsNull(userId)
                    .orElseThrow(() -> raceLost);
        }
    }
}
