package ir.sepahan.app.shop;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import java.time.OffsetDateTime;
import java.util.UUID;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;
import org.hibernate.annotations.UuidGenerator;

@Entity
@Table(name = "cart_item", schema = "shop")
public class CartItem {

    @Id
    @UuidGenerator
    @Column(nullable = false, updatable = false)
    private UUID id;

    @ManyToOne(optional = false)
    private Cart cart;

    @ManyToOne(optional = false)
    private ProductVariant productVariant;

    @Column(nullable = false)
    private int quantity;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private OffsetDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at", nullable = false)
    private OffsetDateTime updatedAt;

    protected CartItem() {
        // JPA
    }

    public CartItem(Cart cart, ProductVariant productVariant, int quantity) {
        this.cart = cart;
        this.productVariant = productVariant;
        this.quantity = quantity;
    }

    public UUID getId() {
        return id;
    }

    public Cart getCart() {
        return cart;
    }

    public ProductVariant getProductVariant() {
        return productVariant;
    }

    public int getQuantity() {
        return quantity;
    }

    public void setQuantity(int quantity) {
        this.quantity = quantity;
    }
}
