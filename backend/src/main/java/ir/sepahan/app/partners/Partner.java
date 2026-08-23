package ir.sepahan.app.partners;

import ir.sepahan.app.common.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;

/**
 * طبق rbac-matrix.md: «مدیریت سیستم Partner» فقط admin (🛠)؛ خودِ Partner فقط از طریق یک
 * API/Webhook مجزا (نه Admin Panel، نه Keycloak) به داده‌ی خودش دسترسی دارد -- طراحی
 * دقیق‌تر API/داده‌ی هر Partner واقعی، وقتی یک Partner واقعی امضا شود، موضوع فاز خودش
 * است؛ این‌جا فقط اسکلت ساختاری (ADR-0015).
 */
@Entity
@Table(name = "partner", schema = "partners")
public class Partner extends BaseEntity {

    @Column(nullable = false, length = 200)
    private String name;

    @Column(nullable = false, length = 200)
    private String slug;

    @Column(name = "contact_email")
    private String contactEmail;

    @Column(name = "contact_phone", length = 20)
    private String contactPhone;

    @Column(name = "api_key_hash", nullable = false, length = 64)
    private String apiKeyHash;

    @Column(name = "is_active", nullable = false)
    private boolean active = true;

    protected Partner() {
        // JPA
    }

    public Partner(String name, String slug, String contactEmail, String contactPhone, String apiKeyHash) {
        this.name = name;
        this.slug = slug;
        this.contactEmail = contactEmail;
        this.contactPhone = contactPhone;
        this.apiKeyHash = apiKeyHash;
    }

    public String getName() {
        return name;
    }

    public String getSlug() {
        return slug;
    }

    public String getContactEmail() {
        return contactEmail;
    }

    public String getContactPhone() {
        return contactPhone;
    }

    public String getApiKeyHash() {
        return apiKeyHash;
    }

    public boolean isActive() {
        return active;
    }

    public void setActive(boolean active) {
        this.active = active;
    }
}
