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
     */
    @Modifying
    @Query("UPDATE ProductVariant v SET v.stockQuantity = v.stockQuantity - :qty "
            + "WHERE v.id = :id AND v.stockQuantity >= :qty")
    int decrementStock(@Param("id") UUID id, @Param("qty") int qty);

    @Modifying
    @Query("UPDATE ProductVariant v SET v.stockQuantity = v.stockQuantity + :qty WHERE v.id = :id")
    void incrementStock(@Param("id") UUID id, @Param("qty") int qty);
}
