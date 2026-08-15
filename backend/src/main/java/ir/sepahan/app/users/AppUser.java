package ir.sepahan.app.users;

import ir.sepahan.app.common.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Table;
import java.util.UUID;

/**
 * هویت فنی/امنیتی کاربر — نگاشت به Keycloak Subject (ADR-0003).
 * طبق docs/database/erd-users-fan.md: عمداً از fan.FanProfile (داده‌ی محصولی) جداست.
 */
@Entity
@Table(name = "app_user", schema = "users")
public class AppUser extends BaseEntity {

    @Column(name = "keycloak_subject", nullable = false)
    private UUID keycloakSubject;

    @Column(name = "national_code", nullable = false, length = 10)
    private String nationalCode;

    @Column(name = "phone_number", nullable = false, length = 15)
    private String phoneNumber;

    @Column(name = "display_name", length = 150)
    private String displayName;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 20)
    private AppUserStatus status = AppUserStatus.active;

    @Enumerated(EnumType.STRING)
    @Column(name = "source", nullable = false, length = 20)
    private AppUserSource source = AppUserSource.mobile_app;

    protected AppUser() {
        // JPA
    }

    public AppUser(UUID keycloakSubject, String nationalCode, String phoneNumber) {
        this.keycloakSubject = keycloakSubject;
        this.nationalCode = nationalCode;
        this.phoneNumber = phoneNumber;
    }

    public UUID getKeycloakSubject() {
        return keycloakSubject;
    }

    public String getNationalCode() {
        return nationalCode;
    }

    public String getPhoneNumber() {
        return phoneNumber;
    }

    public String getDisplayName() {
        return displayName;
    }

    public void setDisplayName(String displayName) {
        this.displayName = displayName;
    }

    public AppUserStatus getStatus() {
        return status;
    }

    public AppUserSource getSource() {
        return source;
    }
}
