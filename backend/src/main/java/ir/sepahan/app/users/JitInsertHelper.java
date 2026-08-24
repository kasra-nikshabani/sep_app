package ir.sepahan.app.users;

import ir.sepahan.app.fan.FanProfile;
import ir.sepahan.app.fan.FanProfileRepository;
import java.util.UUID;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

/**
 * عمداً یک Bean جداست، نه دو متد داخل {@link UserProvisioningService} -- طبق مستندات رسمی
 * اسپرینگ، {@code @Transactional(REQUIRES_NEW)} فقط وقتی از طریق Proxy اسپرینگ فراخوانی شود اثر
 * دارد؛ Self-Invocation (یک متد همان کلاس که مستقیم {@code this.method()} را صدا بزند) کاملاً
 * نادیده گرفته می‌شود و متد در همان Transaction فراخوان اجرا می‌شود -- بدون هیچ خطایی، به‌شکلی
 * کاملاً بی‌صدا.
 *
 * <p>این یک نقص واقعی بود که در Phase 16 با یک تست هم‌زمانی واقعی (۱۰ Thread واقعاً هم‌زمان، نه
 * صرفاً درخواست‌های نزدیک‌به‌هم از مرورگر) کشف شد: وقتی این دو متد داخل خودِ
 * {@code UserProvisioningService} بودند و REQUIRES_NEW عملاً هرگز فعال نمی‌شد، باخت مسابقه‌ی یک
 * Thread کل Transaction آن Thread (که تنها Transaction واقعی موجود بود) را آلوده می‌کرد و هر
 * Query بعدی (حتی SELECT بازیابی در catch) با SQLState=25P02 («transaction aborted») شکست
 * می‌خورد -- یک ۵۰۰ واقعی. جداکردن این متدها به یک Bean دیگر یعنی فراخوانی از
 * {@code UserProvisioningService} واقعاً از طریق Proxy اسپرینگ رد می‌شود و REQUIRES_NEW این‌بار
 * به‌طور واقعی یک Transaction/Connection کاملاً مستقل می‌سازد.
 */
@Component
class JitInsertHelper {

    private final AppUserRepository appUserRepository;
    private final FanProfileRepository fanProfileRepository;

    JitInsertHelper(AppUserRepository appUserRepository, FanProfileRepository fanProfileRepository) {
        this.appUserRepository = appUserRepository;
        this.fanProfileRepository = fanProfileRepository;
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    AppUser insertUser(Jwt jwt, UUID keycloakSubject) {
        String nationalCode = jwt.getClaimAsString("national_code");
        String phoneNumber = jwt.getClaimAsString("phone_number");
        return appUserRepository.saveAndFlush(new AppUser(keycloakSubject, nationalCode, phoneNumber));
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    FanProfile insertFanProfile(UUID userId) {
        return fanProfileRepository.saveAndFlush(new FanProfile(userId));
    }
}
