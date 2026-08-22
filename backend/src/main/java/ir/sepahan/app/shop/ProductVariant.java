package ir.sepahan.app.shop;

import ir.sepahan.app.common.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import java.math.BigDecimal;
import java.util.Map;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

/**
 * هر Product حداقل یک Variant دارد (حتی بدون گزینه‌ی واقعی -- یک Variant
 * «استاندارد» پیش‌فرض) -- طبق ADR-0012 تا Cart/Order همیشه فقط با Variant کار کنند.
 */
@Entity
@Table(name = "product_variant", schema = "shop")
public class ProductVariant extends BaseEntity {

    @ManyToOne(optional = false)
    private Product product;

    @Column(nullable = false, length = 60)
    private String sku;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column
    private Map<String, String> attributes;

    @Column(name = "price_override", precision = 12, scale = 0)
    private BigDecimal priceOverride;

    @Column(name = "stock_quantity", nullable = false)
    private int stockQuantity;

    @Column(name = "weight_grams", nullable = false)
    private int weightGrams = 500;

    @Column(name = "is_active", nullable = false)
    private boolean active = true;

    protected ProductVariant() {
        // JPA
    }

    public ProductVariant(Product product, String sku, Map<String, String> attributes, int stockQuantity) {
        this.product = product;
        this.sku = sku;
        this.attributes = attributes;
        this.stockQuantity = stockQuantity;
    }

    public Product getProduct() {
        return product;
    }

    public String getSku() {
        return sku;
    }

    public Map<String, String> getAttributes() {
        return attributes;
    }

    public BigDecimal getPriceOverride() {
        return priceOverride;
    }

    public void setPriceOverride(BigDecimal priceOverride) {
        this.priceOverride = priceOverride;
    }

    /** قیمت مؤثر برای Cart/Checkout -- priceOverride اگر ست شده باشد، وگرنه قیمت پایه‌ی Product. */
    public BigDecimal getEffectivePrice() {
        return priceOverride != null ? priceOverride : product.getBasePrice();
    }

    public int getStockQuantity() {
        return stockQuantity;
    }

    public int getWeightGrams() {
        return weightGrams;
    }

    public void setWeightGrams(int weightGrams) {
        this.weightGrams = weightGrams;
    }

    public boolean isActive() {
        return active;
    }

    public void setActive(boolean active) {
        this.active = active;
    }
}
