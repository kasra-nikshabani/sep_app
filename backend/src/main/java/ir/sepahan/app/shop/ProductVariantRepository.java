package ir.sepahan.app.shop;

import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface ProductVariantRepository extends JpaRepository<ProductVariant, UUID> {

    List<ProductVariant> findByProductIdAndDeletedAtIsNull(UUID productId);

    Optional<ProductVariant> findByIdAndDeletedAtIsNull(UUID id);

    /**
     * کاهش اتمی موجودی در سطح دیتابیس -- شرط {@code stockQuantity >= :qty} در همان
     * UPDATE تضمین می‌کند دو خرید هم‌زمان هرگز موجودی را منفی نکنند (بدون نیاز به
     * قفل Redis؛ برخلاف صندلی تئاتر، این‌جا یک Pool تعداد است نه یک منبع منحصربه‌فرد
     * -- طبق ADR-0012). اگر ۰ ردیف تغییر کند یعنی موجودی کافی نبوده.
     *
     * {@code clearAutomatically}: پیشگیرانه (Phase 12) -- بدون آن، اگر همین Entity قبلاً در
     * همین Transaction بارگذاری شده باشد، یک save() بعدی روی آن مقدار صحیحِ همین UPDATE را با
     * مقدار قدیمی از Persistence Context بازنویسی می‌کند (باگ دقیقاً همین‌شکلی، نه در این
     * کلاس بلکه در الگوی مشابه loyalty.LoyaltyAccountRepository، با تست واقعی کشف شد).
     *
     * {@code flushAutomatically = true} همراهش اجباری است: بدون آن، clearAutomatically حافظه‌ی
     * Entityهای هنوز Flush-نشده (مثل Order تازه‌ساخته‌شده‌ی همین Transaction در checkout) را
     * بدون نوشتن روی دیتابیس دور می‌ریزد -- دقیقاً همین سناریو با اجرای واقعی تست این ماژول
     * بلافاصله به یک خطای FK Constraint واقعی برخورد کرد (order_item بدون shop_order والدش).
     */
    @Modifying(flushAutomatically = true, clearAutomatically = true)
    @Query("UPDATE ProductVariant v SET v.stockQuantity = v.stockQuantity - :qty "
            + "WHERE v.id = :id AND v.stockQuantity >= :qty")
    int decrementStock(@Param("id") UUID id, @Param("qty") int qty);

    @Modifying(flushAutomatically = true, clearAutomatically = true)
    @Query("UPDATE ProductVariant v SET v.stockQuantity = v.stockQuantity + :qty WHERE v.id = :id")
    void incrementStock(@Param("id") UUID id, @Param("qty") int qty);
}
