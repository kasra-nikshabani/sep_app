package ir.sepahan.app.shop;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import java.time.OffsetDateTime;
import java.util.UUID;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;
import org.hibernate.annotations.UuidGenerator;

/**
 * بدون هیچ تسویه‌ی مالی خودکار (نه Refund API واقعی برای Zibal طبق ADR-0011، نه
 * ماژول Wallet که هنوز ساخته نشده) -- فقط گردش‌کار درخواست/تأیید/رد/تکمیل،
 * طبق تصمیم صریح کارفرما در همین فاز (ADR-0012).
 */
@Entity
@Table(name = "return_request", schema = "shop")
public class ReturnRequest {

    @Id
    @UuidGenerator
    @Column(nullable = false, updatable = false)
    private UUID id;

    @ManyToOne(optional = false)
    @JoinColumn(name = "shop_order_id")
    private Order order;

    @Column(name = "user_id", nullable = false)
    private UUID userId;

    @Column(nullable = false)
    private String reason;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private ReturnStatus status = ReturnStatus.requested;

    /** برای بازگرداندن دقیق وضعیت سفارش اگر درخواست رد شود. */
    @Enumerated(EnumType.STRING)
    @Column(name = "order_status_before_return", nullable = false, length = 20)
    private OrderStatus orderStatusBeforeReturn;

    @Column(name = "admin_note")
    private String adminNote;

    @Column(name = "resolved_by")
    private UUID resolvedBy;

    @Column(name = "resolved_at")
    private OffsetDateTime resolvedAt;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private OffsetDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at", nullable = false)
    private OffsetDateTime updatedAt;

    protected ReturnRequest() {
        // JPA
    }

    public ReturnRequest(Order order, UUID userId, String reason, OrderStatus orderStatusBeforeReturn) {
        this.order = order;
        this.userId = userId;
        this.reason = reason;
        this.orderStatusBeforeReturn = orderStatusBeforeReturn;
    }

    public UUID getId() {
        return id;
    }

    public Order getOrder() {
        return order;
    }

    public UUID getUserId() {
        return userId;
    }

    public String getReason() {
        return reason;
    }

    public ReturnStatus getStatus() {
        return status;
    }

    public void setStatus(ReturnStatus status) {
        this.status = status;
    }

    public OrderStatus getOrderStatusBeforeReturn() {
        return orderStatusBeforeReturn;
    }

    public OffsetDateTime getCreatedAt() {
        return createdAt;
    }

    public String getAdminNote() {
        return adminNote;
    }

    public void setAdminNote(String adminNote) {
        this.adminNote = adminNote;
    }

    public UUID getResolvedBy() {
        return resolvedBy;
    }

    public void setResolvedBy(UUID resolvedBy) {
        this.resolvedBy = resolvedBy;
    }

    public OffsetDateTime getResolvedAt() {
        return resolvedAt;
    }

    public void setResolvedAt(OffsetDateTime resolvedAt) {
        this.resolvedAt = resolvedAt;
    }
}
