package ir.sepahan.app.loyalty;

import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface LoyaltyAccountRepository extends JpaRepository<LoyaltyAccount, UUID> {

    Optional<LoyaltyAccount> findByUserId(UUID userId);

    /**
     * جمع‌شدنی است -- بدون نیاز به شرط محافظتی (برخلاف کاهش).
     *
     * {@code clearAutomatically}: ضروری است -- بدون آن، اگر همین Entity قبلاً در همین
     * Transaction بارگذاری شده باشد (مثلاً توسط {@code getOrCreateAccount})، Hibernate مقادیر
     * قدیمی را در Persistence Context نگه می‌دارد؛ یک {@code save()} بعدی روی همان Entity
     * (مثلاً برای تغییر سطح) مقادیر صحیحِ همین UPDATE را با مقادیر قدیمی از حافظه بازنویسی
     * می‌کند -- باگ واقعی که در تست همین فاز کشف شد (امتیاز واقعاً اضافه می‌شد ولی بلافاصله
     * توسط recomputeLevel صفر می‌شد).
     *
     * {@code flushAutomatically} همراهش اجباری است: بدون آن، clearAutomatically حافظه‌ی
     * Entityهای هنوز Flush-نشده‌ی همین Transaction را بدون نوشتن روی دیتابیس دور می‌ریزد --
     * دومین باگ واقعی که بلافاصله بعد از رفع اولی، با اجرای مجدد کل تست‌ها کشف شد (این‌بار در
     * ماژول Shop، چون همان الگو در ProductVariantRepository هم اصلاح شده بود).
     */
    @Modifying(flushAutomatically = true, clearAutomatically = true)
    @Query("UPDATE LoyaltyAccount a SET a.pointsBalance = a.pointsBalance + :points, "
            + "a.lifetimePoints = a.lifetimePoints + :points WHERE a.id = :id")
    void incrementEarnedPoints(@Param("id") UUID id, @Param("points") int points);

    /**
     * کاهش اتمی موجودی امتیاز (خرج‌کردن) -- شرط {@code pointsBalance >= :points} در همان UPDATE
     * تضمین می‌کند دو درخواست هم‌زمان هرگز موجودی را منفی نکنند (هم‌الگوی decrementStock در Shop).
     * lifetimePoints عمداً دست‌نخورده می‌ماند -- سطح عضویت با خرج‌کردن پایین نمی‌آید (ADR-0014).
     */
    @Modifying(flushAutomatically = true, clearAutomatically = true)
    @Query("UPDATE LoyaltyAccount a SET a.pointsBalance = a.pointsBalance - :points "
            + "WHERE a.id = :id AND a.pointsBalance >= :points")
    int decrementSpentPoints(@Param("id") UUID id, @Param("points") int points);

    /** فقط برای بازگشت امتیاز بعد از لغو یک مبادله -- lifetimePoints دست‌نخورده می‌ماند (هرگز کم نشده بود). */
    @Modifying(flushAutomatically = true, clearAutomatically = true)
    @Query("UPDATE LoyaltyAccount a SET a.pointsBalance = a.pointsBalance + :points WHERE a.id = :id")
    void incrementEarnedPointsBalanceOnly(@Param("id") UUID id, @Param("points") int points);
}
