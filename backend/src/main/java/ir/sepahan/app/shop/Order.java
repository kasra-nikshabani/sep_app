package ir.sepahan.app.shop;

import ir.sepahan.app.common.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.UUID;

/**
 * نام کلاس Order است (واژگان دامنه)، ولی جدول shop_order -- چون order یک کلمه‌ی
 * رزروشده‌ی SQL است (طبق کامنت V5__create_shop_schema.sql).
 */
@Entity
@Table(name = "shop_order", schema = "shop")
public class Order extends BaseEntity {

    @Column(name = "user_id", nullable = false)
    private UUID userId;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private OrderStatus status = OrderStatus.pending_payment;

    @Column(name = "subtotal_amount", nullable = false, precision = 12, scale = 0)
    private BigDecimal subtotalAmount;

    @Column(name = "discount_amount", nullable = false, precision = 12, scale = 0)
    private BigDecimal discountAmount = BigDecimal.ZERO;

    @Column(name = "shipping_amount", nullable = false, precision = 12, scale = 0)
    private BigDecimal shippingAmount = BigDecimal.ZERO;

    @Column(name = "total_amount", nullable = false, precision = 12, scale = 0)
    private BigDecimal totalAmount;

    @ManyToOne
    private Coupon coupon;

    @ManyToOne(optional = false)
    private ShippingMethod shippingMethod;

    @Column(name = "shipping_recipient_name", nullable = false, length = 150)
    private String shippingRecipientName;

    @Column(name = "shipping_phone", nullable = false, length = 20)
    private String shippingPhone;

    @Column(name = "shipping_province", nullable = false, length = 100)
    private String shippingProvince;

    @Column(name = "shipping_city", nullable = false, length = 100)
    private String shippingCity;

    @Column(name = "shipping_address_line", nullable = false)
    private String shippingAddressLine;

    @Column(name = "shipping_postal_code", length = 20)
    private String shippingPostalCode;

    @Column(name = "expires_at")
    private OffsetDateTime expiresAt;

    /**
     * فقط true می‌شود در یک حالت لبه‌ای واقعی: پرداخت دیر (بعد از Expiry/Cleanup سفارش)
     * تأیید شود در حالی که موجودی قبلاً به‌خاطر انقضا آزاد شده. طبق ADR-0012 عمداً
     * هرگز به‌صورت خودکار paid نمی‌شود در این حالت -- نیاز به بررسی دستی ادمین دارد
     * (چون پول واقعی گرفته شده، بدون تضمین موجودی).
     */
    @Column(name = "requires_manual_review", nullable = false)
    private boolean requiresManualReview = false;

    protected Order() {
        // JPA
    }

    public Order(UUID userId, BigDecimal subtotalAmount, BigDecimal discountAmount, BigDecimal shippingAmount,
                 BigDecimal totalAmount, Coupon coupon, ShippingMethod shippingMethod,
                 String shippingRecipientName, String shippingPhone, String shippingProvince,
                 String shippingCity, String shippingAddressLine, String shippingPostalCode,
                 OffsetDateTime expiresAt) {
        this.userId = userId;
        this.subtotalAmount = subtotalAmount;
        this.discountAmount = discountAmount;
        this.shippingAmount = shippingAmount;
        this.totalAmount = totalAmount;
        this.coupon = coupon;
        this.shippingMethod = shippingMethod;
        this.shippingRecipientName = shippingRecipientName;
        this.shippingPhone = shippingPhone;
        this.shippingProvince = shippingProvince;
        this.shippingCity = shippingCity;
        this.shippingAddressLine = shippingAddressLine;
        this.shippingPostalCode = shippingPostalCode;
        this.expiresAt = expiresAt;
    }

    public UUID getUserId() {
        return userId;
    }

    public OrderStatus getStatus() {
        return status;
    }

    public void setStatus(OrderStatus status) {
        this.status = status;
    }

    public BigDecimal getSubtotalAmount() {
        return subtotalAmount;
    }

    public BigDecimal getDiscountAmount() {
        return discountAmount;
    }

    public BigDecimal getShippingAmount() {
        return shippingAmount;
    }

    public BigDecimal getTotalAmount() {
        return totalAmount;
    }

    public Coupon getCoupon() {
        return coupon;
    }

    public ShippingMethod getShippingMethod() {
        return shippingMethod;
    }

    public String getShippingRecipientName() {
        return shippingRecipientName;
    }

    public String getShippingPhone() {
        return shippingPhone;
    }

    public String getShippingProvince() {
        return shippingProvince;
    }

    public String getShippingCity() {
        return shippingCity;
    }

    public String getShippingAddressLine() {
        return shippingAddressLine;
    }

    public String getShippingPostalCode() {
        return shippingPostalCode;
    }

    public OffsetDateTime getExpiresAt() {
        return expiresAt;
    }

    public boolean isRequiresManualReview() {
        return requiresManualReview;
    }

    public void setRequiresManualReview(boolean requiresManualReview) {
        this.requiresManualReview = requiresManualReview;
    }
}
