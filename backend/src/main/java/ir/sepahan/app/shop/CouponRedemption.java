package ir.sepahan.app.shop;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import java.time.OffsetDateTime;
import java.util.UUID;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UuidGenerator;

/**
 * در لحظه‌ی ساخت سفارش ثبت می‌شود و با انقضا/لغو سفارش برگردانده نمی‌شود --
 * تصمیم عمداً محافظه‌کارانه‌ی ADR-0012 برای جلوگیری از دور زدن سقف تعداد
 * استفاده با ساخت/رهاسازی مکرر سفارش.
 */
@Entity
@Table(name = "coupon_redemption", schema = "shop")
public class CouponRedemption {

    @Id
    @UuidGenerator
    @Column(nullable = false, updatable = false)
    private UUID id;

    @ManyToOne(optional = false)
    private Coupon coupon;

    @ManyToOne(optional = false)
    @JoinColumn(name = "shop_order_id")
    private Order order;

    @Column(name = "user_id", nullable = false)
    private UUID userId;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private OffsetDateTime createdAt;

    protected CouponRedemption() {
        // JPA
    }

    public CouponRedemption(Coupon coupon, Order order, UUID userId) {
        this.coupon = coupon;
        this.order = order;
        this.userId = userId;
    }

    public Coupon getCoupon() {
        return coupon;
    }

    public Order getOrder() {
        return order;
    }

    public UUID getUserId() {
        return userId;
    }
}
