package ir.sepahan.app.loyalty;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.ThreadLocalRandom;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

/**
 * مبادله‌ی امتیاز با جایزه. بدون هیچ تحویل خودکار واقعی (پستی/دیجیتال) -- فقط یک کد
 * یکتا صادر می‌شود و ادمین دستی تحویل را طی می‌کند (هم‌الگوی shop.ReturnService؛ هیچ
 * سیستم ارسال/تحویل واقعی در بریف این فاز ذکر نشده).
 */
@Service
public class RewardService {

    private final LoyaltyRewardRepository rewardRepository;
    private final RewardRedemptionRepository redemptionRepository;
    private final LoyaltyAccountRepository accountRepository;
    private final LoyaltyTransactionRepository transactionRepository;
    private final LoyaltyAccountService loyaltyAccountService;

    public RewardService(LoyaltyRewardRepository rewardRepository, RewardRedemptionRepository redemptionRepository,
                          LoyaltyAccountRepository accountRepository, LoyaltyTransactionRepository transactionRepository,
                          LoyaltyAccountService loyaltyAccountService) {
        this.rewardRepository = rewardRepository;
        this.redemptionRepository = redemptionRepository;
        this.accountRepository = accountRepository;
        this.transactionRepository = transactionRepository;
        this.loyaltyAccountService = loyaltyAccountService;
    }

    @Transactional
    public RewardRedemption redeem(UUID userId, UUID rewardId) {
        LoyaltyReward reward = rewardRepository.findByIdAndDeletedAtIsNull(rewardId)
                .filter(LoyaltyReward::isActive)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "جایزه یافت نشد"));

        LoyaltyAccount account = loyaltyAccountService.getOrCreateAccount(userId);

        if (reward.getStockQuantity() != null) {
            int affected = rewardRepository.decrementStock(reward.getId());
            if (affected == 0) {
                throw new ResponseStatusException(HttpStatus.CONFLICT, "موجودی این جایزه تمام شده است");
            }
        }

        int decremented = accountRepository.decrementSpentPoints(account.getId(), reward.getPointsCost());
        if (decremented == 0) {
            throw new InsufficientPointsException();
        }

        RewardRedemption redemption = new RewardRedemption(account, reward, reward.getPointsCost(), generateCode());
        redemption = redemptionRepository.save(redemption);

        transactionRepository.save(new LoyaltyTransaction(account, LoyaltyTransactionType.redeem,
                -reward.getPointsCost(), "reward_redemption", null, "مبادله با جایزه: " + reward.getName()));

        return redemption;
    }

    @Transactional
    public RewardRedemption fulfill(UUID redemptionId, UUID adminId) {
        RewardRedemption redemption = requireRequested(redemptionId);
        redemption.setStatus(RedemptionStatus.fulfilled);
        redemption.setFulfilledBy(adminId);
        redemption.setFulfilledAt(OffsetDateTime.now());
        return redemptionRepository.save(redemption);
    }

    /** بازگرداندن امتیاز به کاربر (lifetimePoints دست‌نخورده می‌ماند -- فقط balance برمی‌گردد). */
    @Transactional
    public RewardRedemption cancel(UUID redemptionId, UUID adminId) {
        RewardRedemption redemption = requireRequested(redemptionId);
        redemption.setStatus(RedemptionStatus.cancelled);
        redemption.setFulfilledBy(adminId);
        redemption.setFulfilledAt(OffsetDateTime.now());
        redemptionRepository.save(redemption);

        accountRepository.incrementEarnedPointsBalanceOnly(redemption.getAccount().getId(), redemption.getPointsSpent());
        if (redemption.getReward().getStockQuantity() != null) {
            rewardRepository.incrementStock(redemption.getReward().getId());
        }
        transactionRepository.save(new LoyaltyTransaction(redemption.getAccount(), LoyaltyTransactionType.adjust,
                redemption.getPointsSpent(), null, null, "بازگشت امتیاز -- لغو مبادله‌ی " + redemption.getRedemptionCode()));
        return redemption;
    }

    public List<LoyaltyReward> listActive() {
        return rewardRepository.findByActiveTrueAndDeletedAtIsNull();
    }

    private RewardRedemption requireRequested(UUID redemptionId) {
        RewardRedemption redemption = redemptionRepository.findById(redemptionId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "درخواست مبادله یافت نشد"));
        if (redemption.getStatus() != RedemptionStatus.requested) {
            throw new RedemptionStateException("این درخواست مبادله قبلاً بررسی شده است");
        }
        return redemption;
    }

    private String generateCode() {
        return "RW-" + System.currentTimeMillis() + "-" + ThreadLocalRandom.current().nextInt(1000, 9999);
    }
}
