package ir.sepahan.app.loyalty;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

/**
 * منطق کسب/خرج امتیاز و محاسبه‌ی سطح. طبق بند ۱۶ بریف، نرخ امتیازدهی از
 * {@link PointsEarningRule} (Configurable) خوانده می‌شود، نه یک عدد Hardcode.
 *
 * Idempotency رویدادهای earn دقیقاً هم‌الگوی UserProvisioningService (JIT، Phase 5) است:
 * تلاش برای Insert در یک Transaction جدا (REQUIRES_NEW)، و اگر Unique Index دیتابیس
 * (نه این کد) رد کرد، رکورد موجود را برمی‌گردانیم -- نه خطا.
 */
@Service
public class LoyaltyAccountService {

    private final LoyaltyAccountRepository accountRepository;
    private final LoyaltyTransactionRepository transactionRepository;
    private final PointsEarningRuleRepository earningRuleRepository;
    private final LoyaltyLevelRepository levelRepository;

    public LoyaltyAccountService(LoyaltyAccountRepository accountRepository,
                                  LoyaltyTransactionRepository transactionRepository,
                                  PointsEarningRuleRepository earningRuleRepository,
                                  LoyaltyLevelRepository levelRepository) {
        this.accountRepository = accountRepository;
        this.transactionRepository = transactionRepository;
        this.earningRuleRepository = earningRuleRepository;
        this.levelRepository = levelRepository;
    }

    public LoyaltyAccount getOrCreateAccount(UUID userId) {
        return accountRepository.findByUserId(userId).orElseGet(() -> createAccountSafely(userId));
    }

    /**
     * saveAndFlush عمداً به‌جای save: بدون Flush صریح، DataIntegrityViolationException
     * فقط در لحظه‌ی Commit تراکنش (بیرون این متد، بیرون try/catch) پرتاب می‌شد
     * و اصلاً گرفته نمی‌شد -- دقیقاً همان باگ کشف‌شده در UserProvisioningService
     * حین همین فاز (دو تماس هم‌زمان اولین ورود اپ موبایل).
     */
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    protected LoyaltyAccount createAccountSafely(UUID userId) {
        try {
            return accountRepository.saveAndFlush(new LoyaltyAccount(userId));
        } catch (DataIntegrityViolationException raceLost) {
            return accountRepository.findByUserId(userId).orElseThrow(() -> raceLost);
        }
    }

    /**
     * منبع فراخوانی: {@link LoyaltyPaymentListener} (خرید Shop/تئاتر) یا Polling بلیط فوتبال از
     * Django. اگر برای {@code sourceType} هیچ قانون فعالی تعریف نشده باشد، عمداً هیچ امتیازی
     * داده نمی‌شود (مستند در Log، نه سکوت پنهان در محاسبه‌ی اشتباه).
     */
    @Transactional
    public LoyaltyTransaction earnPoints(UUID userId, EarningSourceType sourceType, UUID sourceReferenceId, BigDecimal amount) {
        Optional<LoyaltyTransaction> existing = transactionRepository
                .findBySourceTypeAndSourceReferenceId(sourceType.name(), sourceReferenceId);
        if (existing.isPresent()) {
            return existing.get();
        }

        PointsEarningRule rule = earningRuleRepository
                .findBySourceTypeAndActiveTrueAndDeletedAtIsNull(sourceType).orElse(null);
        if (rule == null) {
            return null;
        }
        int points = amount.divide(rule.getPointsPerAmount(), 0, RoundingMode.DOWN).intValueExact();
        if (points <= 0) {
            return null;
        }

        LoyaltyAccount account = getOrCreateAccount(userId);
        Optional<LoyaltyTransaction> inserted = tryInsertTransaction(account, LoyaltyTransactionType.earn, points,
                sourceType.name(), sourceReferenceId, "کسب امتیاز از " + sourceType);
        if (inserted.isEmpty()) {
            // رقابت هم‌زمان روی همین رویداد -- تلاش دیگری قبلاً موفق شده؛ هیچ افزایش دوباره‌ای اعمال نمی‌شود
            return transactionRepository.findBySourceTypeAndSourceReferenceId(sourceType.name(), sourceReferenceId).orElse(null);
        }

        accountRepository.incrementEarnedPoints(account.getId(), points);
        recomputeLevel(account.getId());
        return inserted.get();
    }

    /** {@code delta} مثبت مثل earn رفتار می‌کند (lifetimePoints هم زیاد می‌شود)؛ منفی فقط از balance کم می‌شود. */
    @Transactional
    public LoyaltyTransaction adjustPointsManually(UUID userId, UUID adminId, int delta, String reason) {
        if (delta == 0) {
            throw new IllegalArgumentException("مقدار اصلاح نمی‌تواند صفر باشد");
        }
        LoyaltyAccount account = getOrCreateAccount(userId);
        if (delta > 0) {
            accountRepository.incrementEarnedPoints(account.getId(), delta);
            recomputeLevel(account.getId());
        } else {
            int affected = accountRepository.decrementSpentPoints(account.getId(), -delta);
            if (affected == 0) {
                throw new InsufficientPointsException();
            }
        }
        LoyaltyTransaction txn = new LoyaltyTransaction(account, LoyaltyTransactionType.adjust, delta, null, null, reason);
        txn.setCreatedBy(adminId);
        return transactionRepository.save(txn);
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    protected Optional<LoyaltyTransaction> tryInsertTransaction(LoyaltyAccount account, LoyaltyTransactionType type,
                                                                 int points, String sourceType, UUID sourceReferenceId,
                                                                 String description) {
        try {
            return Optional.of(transactionRepository.saveAndFlush(
                    new LoyaltyTransaction(account, type, points, sourceType, sourceReferenceId, description)));
        } catch (DataIntegrityViolationException raceLost) {
            return Optional.empty();
        }
    }

    /**
     * این متد همیشه بعد از یک UPDATE اتمی (مثلاً incrementEarnedPoints) در همین Transaction صدا
     * زده می‌شود -- findById این‌جا باید مقدار *تازه* را ببیند، نه نسخه‌ی قدیمی در Persistence
     * Context (طبق clearAutomatically روی آن Queryها). اگر این ترتیب رعایت نشود، save() زیر
     * می‌تواند مقدار صحیح lifetimePoints/pointsBalance را با مقدار قدیمی بازنویسی کند --
     * دقیقاً همان باگ واقعی که در تست همین فاز کشف و رفع شد.
     */
    private void recomputeLevel(UUID accountId) {
        LoyaltyAccount account = accountRepository.findById(accountId).orElseThrow();
        LoyaltyLevel newLevel = levelRepository
                .findTopByMinPointsLessThanEqualAndDeletedAtIsNullOrderByMinPointsDesc(account.getLifetimePoints())
                .orElse(null);
        UUID currentLevelId = account.getLevel() != null ? account.getLevel().getId() : null;
        UUID newLevelId = newLevel != null ? newLevel.getId() : null;
        if (!Objects.equals(currentLevelId, newLevelId)) {
            account.setLevel(newLevel);
            accountRepository.save(account);
        }
    }
}
