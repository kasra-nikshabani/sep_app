package ir.sepahan.app.loyalty;

import ir.sepahan.app.common.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;

/** {@code stockQuantity} برابر NULL یعنی نامحدود؛ عدد یعنی موجودی محدود (کاهش اتمی، هم‌الگوی ProductVariant در Shop). */
@Entity
@Table(name = "reward", schema = "loyalty")
public class LoyaltyReward extends BaseEntity {

    @Column(nullable = false, length = 200)
    private String name;

    @Column
    private String description;

    @Column(name = "points_cost", nullable = false)
    private int pointsCost;

    @Column(name = "stock_quantity")
    private Integer stockQuantity;

    @Column(name = "is_active", nullable = false)
    private boolean active = true;

    protected LoyaltyReward() {
        // JPA
    }

    public LoyaltyReward(String name, String description, int pointsCost, Integer stockQuantity) {
        this.name = name;
        this.description = description;
        this.pointsCost = pointsCost;
        this.stockQuantity = stockQuantity;
    }

    public String getName() {
        return name;
    }

    public String getDescription() {
        return description;
    }

    public int getPointsCost() {
        return pointsCost;
    }

    public Integer getStockQuantity() {
        return stockQuantity;
    }

    public boolean isActive() {
        return active;
    }

    public void setActive(boolean active) {
        this.active = active;
    }
}
