package ir.sepahan.app.notifications;

import ir.sepahan.app.common.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Table;
import java.util.UUID;

/**
 * فقط زیرساخت ثبت/لغو Token -- هیچ Provider واقعی Push (FCM/APNs) در این فاز متصل نیست
 * (بدون اپ موبایل واقعی هنوز، Phase 15). یک کاربر می‌تواند چند دستگاه فعال داشته باشد.
 */
@Entity
@Table(name = "device_token", schema = "notifications")
public class DeviceToken extends BaseEntity {

    @Column(name = "user_id", nullable = false)
    private UUID userId;

    @Column(nullable = false, length = 500)
    private String token;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private DevicePlatform platform;

    @Column(name = "is_active", nullable = false)
    private boolean active = true;

    protected DeviceToken() {
        // JPA
    }

    public DeviceToken(UUID userId, String token, DevicePlatform platform) {
        this.userId = userId;
        this.token = token;
        this.platform = platform;
    }

    public UUID getUserId() {
        return userId;
    }

    public String getToken() {
        return token;
    }

    public DevicePlatform getPlatform() {
        return platform;
    }

    public boolean isActive() {
        return active;
    }

    public void setActive(boolean active) {
        this.active = active;
    }
}
