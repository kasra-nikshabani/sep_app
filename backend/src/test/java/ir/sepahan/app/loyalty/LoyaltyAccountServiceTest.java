package ir.sepahan.app.loyalty;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.math.BigDecimal;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

/**
 * این تست‌ها به Postgres واقعی نیاز دارند (طبق backend/README.md)، هم‌سبک
 * OrderServiceTest/ArticleServiceTest (Phase 10/11).
 */
@SpringBootTest
class LoyaltyAccountServiceTest {

    @Autowired
    private LoyaltyLevelRepository levelRepository;
    @Autowired
    private PointsEarningRuleRepository earningRuleRepository;
    @Autowired
    private LoyaltyAccountRepository accountRepository;
    @Autowired
    private LoyaltyTransactionRepository transactionRepository;
    @Autowired
    private LoyaltyRewardRepository rewardRepository;
    @Autowired
    private RewardRedemptionRepository redemptionRepository;
    @Autowired
    private LoyaltyAccountService loyaltyAccountService;
    @Autowired
    private RewardService rewardService;

    @BeforeEach
    void setUp() {
        earningRuleRepository.save(new PointsEarningRule(EarningSourceType.shop_order, new BigDecimal("10000")));
    }

    @AfterEach
    void tearDown() {
        redemptionRepository.deleteAll();
        transactionRepository.deleteAll();
        rewardRepository.deleteAll();
        accountRepository.deleteAll();
        earningRuleRepository.deleteAll();
        levelRepository.deleteAll();
    }

    @Test
    void earnPoints_createsAccountAndCreditsPoints_basedOnActiveRule() {
        UUID userId = UUID.randomUUID();

        LoyaltyTransaction txn = loyaltyAccountService.earnPoints(userId, EarningSourceType.shop_order,
                UUID.randomUUID(), new BigDecimal("125000"));

        assertThat(txn).isNotNull();
        assertThat(txn.getPoints()).isEqualTo(12); // floor(125000 / 10000)
        LoyaltyAccount account = accountRepository.findByUserId(userId).orElseThrow();
        assertThat(account.getPointsBalance()).isEqualTo(12);
        assertThat(account.getLifetimePoints()).isEqualTo(12);
    }

    @Test
    void earnPoints_isIdempotent_sameSourceReferenceNeverCreditsTwice() {
        UUID userId = UUID.randomUUID();
        UUID orderId = UUID.randomUUID();

        loyaltyAccountService.earnPoints(userId, EarningSourceType.shop_order, orderId, new BigDecimal("100000"));
        loyaltyAccountService.earnPoints(userId, EarningSourceType.shop_order, orderId, new BigDecimal("100000"));

        LoyaltyAccount account = accountRepository.findByUserId(userId).orElseThrow();
        assertThat(account.getPointsBalance()).isEqualTo(10); // نه ۲۰
    }

    @Test
    void earnPoints_withoutActiveRule_creditsNothing() {
        UUID userId = UUID.randomUUID();

        LoyaltyTransaction txn = loyaltyAccountService.earnPoints(userId, EarningSourceType.ticket_purchase,
                UUID.randomUUID(), new BigDecimal("500000"));

        assertThat(txn).isNull();
        assertThat(accountRepository.findByUserId(userId)).isEmpty();
    }

    @Test
    void earnPoints_crossingThreshold_upgradesLevel() {
        levelRepository.save(new LoyaltyLevel("برنزی", 0, null));
        LoyaltyLevel silver = levelRepository.save(new LoyaltyLevel("نقره‌ای", 10, null));
        UUID userId = UUID.randomUUID();

        loyaltyAccountService.earnPoints(userId, EarningSourceType.shop_order, UUID.randomUUID(), new BigDecimal("150000"));

        LoyaltyAccount account = accountRepository.findByUserId(userId).orElseThrow();
        assertThat(account.getLevel().getId()).isEqualTo(silver.getId());
    }

    @Test
    void redeemReward_deductsPointsAndDecrementsLimitedStock() {
        UUID userId = UUID.randomUUID();
        loyaltyAccountService.earnPoints(userId, EarningSourceType.shop_order, UUID.randomUUID(), new BigDecimal("1000000"));
        LoyaltyReward reward = rewardRepository.save(new LoyaltyReward("شال و کلاه سپاهان", null, 50, 3));

        RewardRedemption redemption = rewardService.redeem(userId, reward.getId());

        assertThat(redemption.getStatus()).isEqualTo(RedemptionStatus.requested);
        assertThat(redemption.getRedemptionCode()).isNotBlank();
        LoyaltyAccount account = accountRepository.findByUserId(userId).orElseThrow();
        assertThat(account.getPointsBalance()).isEqualTo(50); // 100 - 50
        assertThat(account.getLifetimePoints()).isEqualTo(100); // دست‌نخورده
        assertThat(rewardRepository.findById(reward.getId()).orElseThrow().getStockQuantity()).isEqualTo(2);
    }

    @Test
    void redeemReward_rejectsWhenInsufficientPoints() {
        UUID userId = UUID.randomUUID();
        loyaltyAccountService.earnPoints(userId, EarningSourceType.shop_order, UUID.randomUUID(), new BigDecimal("50000"));
        LoyaltyReward reward = rewardRepository.save(new LoyaltyReward("جایزه‌ی گران", null, 999, null));

        assertThatThrownBy(() -> rewardService.redeem(userId, reward.getId()))
                .isInstanceOf(InsufficientPointsException.class);
    }

    @Test
    void cancelRedemption_refundsBalance_withoutTouchingLifetimePointsOrLevel() {
        levelRepository.save(new LoyaltyLevel("برنزی", 0, null));
        LoyaltyLevel silver = levelRepository.save(new LoyaltyLevel("نقره‌ای", 10, null));
        UUID userId = UUID.randomUUID();
        loyaltyAccountService.earnPoints(userId, EarningSourceType.shop_order, UUID.randomUUID(), new BigDecimal("200000")); // 20 امتیاز -> نقره‌ای
        LoyaltyReward reward = rewardRepository.save(new LoyaltyReward("جایزه", null, 15, 1));
        RewardRedemption redemption = rewardService.redeem(userId, reward.getId());

        rewardService.cancel(redemption.getId(), UUID.randomUUID());

        LoyaltyAccount account = accountRepository.findByUserId(userId).orElseThrow();
        assertThat(account.getPointsBalance()).isEqualTo(20); // برگشت کامل
        assertThat(account.getLifetimePoints()).isEqualTo(20);
        assertThat(account.getLevel().getId()).isEqualTo(silver.getId()); // سطح افت نکرده
        assertThat(rewardRepository.findById(reward.getId()).orElseThrow().getStockQuantity()).isEqualTo(1);
        assertThat(redemptionRepository.findById(redemption.getId()).orElseThrow().getStatus()).isEqualTo(RedemptionStatus.cancelled);
    }

    @Test
    void adjustPointsManually_negative_rejectsWhenInsufficient() {
        UUID userId = UUID.randomUUID();
        loyaltyAccountService.getOrCreateAccount(userId);

        assertThatThrownBy(() -> loyaltyAccountService.adjustPointsManually(userId, UUID.randomUUID(), -10, "اصلاح"))
                .isInstanceOf(InsufficientPointsException.class);
    }

    @Test
    void adjustPointsManually_positive_increasesBalanceAndLifetime() {
        UUID userId = UUID.randomUUID();

        loyaltyAccountService.adjustPointsManually(userId, UUID.randomUUID(), 30, "جبران خطای سیستمی");

        LoyaltyAccount account = accountRepository.findByUserId(userId).orElseThrow();
        assertThat(account.getPointsBalance()).isEqualTo(30);
        assertThat(account.getLifetimePoints()).isEqualTo(30);
    }

    /**
     * دقیقاً همان بار مسابقه‌ای که در UserProvisioningServiceTest برای JIT کاربر تست شد -- این‌جا
     * برای JIT حساب وفاداری (ADR-0018): چند فراخوانی هم‌زمان {@code getOrCreateAccount} برای همان
     * userId. تعداد Thread عمداً محدود (نه ده‌ها) چون هر Thread حین این متد تا دو Connection
     * هم‌زمان لازم دارد (یکی Transaction فراخوان، یکی LoyaltyJitInsertHelper.insertAccount با
     * REQUIRES_NEW واقعی)؛ عدد بیشتر خودِ تست را به Timeout پول پیش‌فرض HikariCP می‌رساند، نه کد
     * Production را.
     */
    @Test
    void getOrCreateAccount_concurrentFirstAccess_allCallsSucceedWithSameAccount() throws InterruptedException {
        UUID userId = UUID.randomUUID();
        int attempts = 4;
        ExecutorService pool = Executors.newFixedThreadPool(attempts);
        CountDownLatch startLine = new CountDownLatch(1);
        CountDownLatch finishLine = new CountDownLatch(attempts);
        List<LoyaltyAccount> results = new CopyOnWriteArrayList<>();
        List<Throwable> failures = new CopyOnWriteArrayList<>();

        for (int i = 0; i < attempts; i++) {
            pool.submit(() -> {
                try {
                    startLine.await();
                    results.add(loyaltyAccountService.getOrCreateAccount(userId));
                } catch (Throwable t) {
                    failures.add(t);
                } finally {
                    finishLine.countDown();
                }
            });
        }

        startLine.countDown();
        boolean finished = finishLine.await(30, TimeUnit.SECONDS);
        pool.shutdown();

        assertThat(finished).isTrue();
        assertThat(failures).as("هیچ فراخوانی نباید Exception پرتاب کند -- دقیقاً همان نقصی که در Phase 16 کشف و رفع شد").isEmpty();
        Set<UUID> distinctAccountIds = results.stream().map(LoyaltyAccount::getId).collect(Collectors.toSet());
        assertThat(distinctAccountIds).as("همه باید دقیقاً همان رکورد برنده‌ی مسابقه را برگردانند").hasSize(1);
    }

    /**
     * هسته‌ی اصلی ADR-0014 برای جوایز: ده‌ها مبادله‌ی واقعاً هم‌زمان روی آخرین واحد
     * موجودی یک جایزه -- باید فقط یکی موفق شود (هم‌الگوی تست هم‌زمانی موجودی Shop، Phase 10).
     *
     * <p>ساخت حساب/بذر امتیاز عمداً *قبل* از خط شروع هم‌زمانی انجام می‌شود -- چون خودِ
     * {@code adjustPointsManually} (از طریق getOrCreateAccount) از REQUIRES_NEW واقعی استفاده
     * می‌کند (ADR-0018) و به یک Connection دوم نیاز دارد؛ چیزی که این تست واقعاً می‌سنجد رقابت
     * روی آخرین واحد موجودی جایزه است، نه ساخت هم‌زمان حساب -- بار Connection غیرمرتبط با هدف
     * تست را از پنجره‌ی زمان‌بندی‌شده‌ی هم‌زمانی حذف می‌کند.
     */
    @Test
    void concurrentRedemptions_onLastUnitOfStock_onlyOneSucceeds() throws InterruptedException {
        LoyaltyReward reward = rewardRepository.save(new LoyaltyReward("جایزه‌ی کمیاب", null, 10, 1));
        int attempts = 20;
        List<UUID> userIds = new java.util.ArrayList<>();
        for (int i = 0; i < attempts; i++) {
            UUID userId = UUID.randomUUID();
            loyaltyAccountService.adjustPointsManually(userId, UUID.randomUUID(), 100, "بذر تست");
            userIds.add(userId);
        }

        ExecutorService pool = Executors.newFixedThreadPool(attempts);
        CountDownLatch startLine = new CountDownLatch(1);
        CountDownLatch finishLine = new CountDownLatch(attempts);
        AtomicInteger succeeded = new AtomicInteger();
        AtomicInteger rejected = new AtomicInteger();

        for (int i = 0; i < attempts; i++) {
            UUID userId = userIds.get(i);
            pool.submit(() -> {
                try {
                    startLine.await();
                    rewardService.redeem(userId, reward.getId());
                    succeeded.incrementAndGet();
                } catch (RuntimeException expected) {
                    rejected.incrementAndGet();
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                } finally {
                    finishLine.countDown();
                }
            });
        }

        startLine.countDown();
        boolean finished = finishLine.await(30, TimeUnit.SECONDS);
        pool.shutdown();

        assertThat(finished).isTrue();
        assertThat(succeeded.get()).as("دقیقاً یکی باید موفق شود").isEqualTo(1);
        assertThat(rewardRepository.findById(reward.getId()).orElseThrow().getStockQuantity()).isEqualTo(0);
    }
}
