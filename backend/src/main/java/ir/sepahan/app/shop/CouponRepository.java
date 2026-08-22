package ir.sepahan.app.shop;

import jakarta.persistence.LockModeType;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface CouponRepository extends JpaRepository<Coupon, UUID> {

    Optional<Coupon> findByCodeAndDeletedAtIsNull(String code);

    /**
     * قفل بدبینانه روی ردیف کد تخفیف حین اعتبارسنجی/مصرف -- تنها راه بستن کامل Race
     * روی maxUsesTotal وقتی دو کاربر هم‌زمان از یک کد استفاده می‌کنند (طبق ADR-0012؛
     * سربار قابل‌قبول چون فقط یک ردیف است، نه یک Table Lock).
     */
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT c FROM Coupon c WHERE c.code = :code AND c.deletedAt IS NULL")
    Optional<Coupon> findByCodeForUpdate(@Param("code") String code);
}
