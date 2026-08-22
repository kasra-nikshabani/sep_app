package ir.sepahan.app.loyalty;

import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface LoyaltyRewardRepository extends JpaRepository<LoyaltyReward, UUID> {

    List<LoyaltyReward> findByActiveTrueAndDeletedAtIsNull();

    Optional<LoyaltyReward> findByIdAndDeletedAtIsNull(UUID id);

    /**
     * فقط وقتی stockQuantity محدود است اثر دارد -- کاهش اتمی، هم‌الگوی decrementStock در Shop.
     * {@code clearAutomatically}: بدون آن، یک save() بعدی روی همان Entity در همین Transaction
     * می‌تواند این کاهش را با مقدار قدیمیِ در حافظه بازنویسی کند. {@code flushAutomatically}
     * همراهش اجباری است: بدون آن، clearAutomatically حافظه‌ی Entityهای هنوز Flush-نشده‌ی همین
     * Transaction (مثلاً یک RewardRedemption تازه‌ساخته‌شده) را بدون نوشتن روی دیتابیس دور
     * می‌ریزد. هر دو باگ واقعی، اول در loyalty.LoyaltyAccountRepository و بعد در
     * shop.ProductVariantRepository، با تست واقعی همین فاز کشف و همه‌جا یک‌جا اصلاح شدند.
     */
    @Modifying(flushAutomatically = true, clearAutomatically = true)
    @Query("UPDATE LoyaltyReward r SET r.stockQuantity = r.stockQuantity - 1 "
            + "WHERE r.id = :id AND r.stockQuantity >= 1")
    int decrementStock(@Param("id") UUID id);

    /** بازگرداندن موجودی بعد از لغو یک مبادله. */
    @Modifying(flushAutomatically = true, clearAutomatically = true)
    @Query("UPDATE LoyaltyReward r SET r.stockQuantity = r.stockQuantity + 1 WHERE r.id = :id")
    void incrementStock(@Param("id") UUID id);
}
