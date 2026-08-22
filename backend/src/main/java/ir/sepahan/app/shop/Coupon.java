package ir.sepahan.app.shop;

import ir.sepahan.app.common.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Table;
import java.math.BigDecimal;
import java.time.OffsetDateTime;

@Entity
@Table(name = "coupon", schema = "shop")
public class Coupon extends BaseEntity {

    @Column(nullable = false, length = 40)
    private String code;

    @Enumerated(EnumType.STRING)
    @Column(name = "discount_type", nullable = false, length = 20)
    private CouponDiscountType discountType;

    @Column(name = "discount_value", nullable = false, precision = 12, scale = 0)
    private BigDecimal discountValue;

    @Column(name = "min_order_amount", precision = 12, scale = 0)
    private BigDecimal minOrderAmount;

    @Column(name = "max_uses_total")
    private Integer maxUsesTotal;

    @Column(name = "max_uses_per_user")
    private Integer maxUsesPerUser;

    @Column(name = "valid_from")
    private OffsetDateTime validFrom;

    @Column(name = "valid_to")
    private OffsetDateTime validTo;

    @Column(name = "is_active", nullable = false)
    private boolean active = true;

    protected Coupon() {
        // JPA
    }

    public Coupon(String code, CouponDiscountType discountType, BigDecimal discountValue) {
        this.code = code;
        this.discountType = discountType;
        this.discountValue = discountValue;
    }

    public String getCode() {
        return code;
    }

    public CouponDiscountType getDiscountType() {
        return discountType;
    }

    public BigDecimal getDiscountValue() {
        return discountValue;
    }

    public BigDecimal getMinOrderAmount() {
        return minOrderAmount;
    }

    public void setMinOrderAmount(BigDecimal minOrderAmount) {
        this.minOrderAmount = minOrderAmount;
    }

    public Integer getMaxUsesTotal() {
        return maxUsesTotal;
    }

    public void setMaxUsesTotal(Integer maxUsesTotal) {
        this.maxUsesTotal = maxUsesTotal;
    }

    public Integer getMaxUsesPerUser() {
        return maxUsesPerUser;
    }

    public void setMaxUsesPerUser(Integer maxUsesPerUser) {
        this.maxUsesPerUser = maxUsesPerUser;
    }

    public OffsetDateTime getValidFrom() {
        return validFrom;
    }

    public void setValidFrom(OffsetDateTime validFrom) {
        this.validFrom = validFrom;
    }

    public OffsetDateTime getValidTo() {
        return validTo;
    }

    public void setValidTo(OffsetDateTime validTo) {
        this.validTo = validTo;
    }

    public boolean isActive() {
        return active;
    }

    public void setActive(boolean active) {
        this.active = active;
    }
}
