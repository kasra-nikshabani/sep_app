package ir.sepahan.app.shop;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.UUID;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UuidGenerator;

/**
 * نام/قیمت این‌جا Snapshot می‌شوند -- گزارش مالی سفارش‌های قدیمی نباید با تغییر
 * بعدی نام/قیمت محصول عوض شود (همان انضباطی که Payment.amount دارد).
 */
@Entity
@Table(name = "order_item", schema = "shop")
public class OrderItem {

    @Id
    @UuidGenerator
    @Column(nullable = false, updatable = false)
    private UUID id;

    // نام ستون shop_order_id، نه order_id -- چون جدول shop_order است، نه order (کلمه‌ی رزروشده‌ی SQL)
    @ManyToOne(optional = false)
    @JoinColumn(name = "shop_order_id")
    private Order order;

    @ManyToOne(optional = false)
    private ProductVariant productVariant;

    @Column(name = "product_name", nullable = false, length = 200)
    private String productName;

    @Column(name = "variant_sku", nullable = false, length = 60)
    private String variantSku;

    @Column(name = "unit_price", nullable = false, precision = 12, scale = 0)
    private BigDecimal unitPrice;

    @Column(nullable = false)
    private int quantity;

    @Column(nullable = false, precision = 12, scale = 0)
    private BigDecimal subtotal;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private OffsetDateTime createdAt;

    protected OrderItem() {
        // JPA
    }

    public OrderItem(Order order, ProductVariant productVariant, String productName, String variantSku,
                      BigDecimal unitPrice, int quantity, BigDecimal subtotal) {
        this.order = order;
        this.productVariant = productVariant;
        this.productName = productName;
        this.variantSku = variantSku;
        this.unitPrice = unitPrice;
        this.quantity = quantity;
        this.subtotal = subtotal;
    }

    public UUID getId() {
        return id;
    }

    public Order getOrder() {
        return order;
    }

    public ProductVariant getProductVariant() {
        return productVariant;
    }

    public String getProductName() {
        return productName;
    }

    public String getVariantSku() {
        return variantSku;
    }

    public BigDecimal getUnitPrice() {
        return unitPrice;
    }

    public int getQuantity() {
        return quantity;
    }

    public BigDecimal getSubtotal() {
        return subtotal;
    }
}
