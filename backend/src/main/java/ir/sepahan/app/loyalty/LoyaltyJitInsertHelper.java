package ir.sepahan.app.loyalty;

import java.util.UUID;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

/**
 * عمداً یک Bean جداست، نه متدهای داخل {@link LoyaltyAccountService} -- طبق مستندات رسمی اسپرینگ،
 * {@code @Transactional(REQUIRES_NEW)} فقط وقتی از طریق Proxy اسپرینگ فراخوانی شود اثر دارد؛
 * Self-Invocation (متدی از همان کلاس که مستقیم {@code this.method()} را صدا بزند) کاملاً نادیده
 * گرفته می‌شود -- بدون هیچ خطایی، به‌شکلی کاملاً بی‌صدا. جزئیات کامل کشف این نقص واقعی (با یک تست
 * هم‌زمانی واقعی در Phase 16) در {@code ir.sepahan.app.users.JitInsertHelper} مستند شده -- همان
 * نقص، این‌جا هم وجود داشت (هم برای {@code createAccountSafely} هم {@code tryInsertTransaction}،
 * هر دو از داخل همین کلاس Self-Invoke می‌شدند).
 */
@Component
class LoyaltyJitInsertHelper {

    private final LoyaltyAccountRepository accountRepository;
    private final LoyaltyTransactionRepository transactionRepository;

    LoyaltyJitInsertHelper(LoyaltyAccountRepository accountRepository, LoyaltyTransactionRepository transactionRepository) {
        this.accountRepository = accountRepository;
        this.transactionRepository = transactionRepository;
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    LoyaltyAccount insertAccount(UUID userId) {
        return accountRepository.saveAndFlush(new LoyaltyAccount(userId));
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    LoyaltyTransaction insertTransaction(LoyaltyAccount account, LoyaltyTransactionType type, int points,
                                          String sourceType, UUID sourceReferenceId, String description) {
        return transactionRepository.saveAndFlush(
                new LoyaltyTransaction(account, type, points, sourceType, sourceReferenceId, description));
    }
}
