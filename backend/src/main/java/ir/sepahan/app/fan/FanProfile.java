package ir.sepahan.app.fan;

import ir.sepahan.app.common.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.UUID;

/**
 * داده‌ی محصولی/تجربه‌ی هوادار — عمداً جدا از AppUser (docs/database/erd-users-fan.md).
 * MembershipCard عمداً در Phase 6 به Entity تبدیل نشده (فقط جدول در Migration) — منطق صدور/QR هنوز تصمیم‌گیری نشده.
 */
@Entity
@Table(name = "fan_profile", schema = "fan")
public class FanProfile extends BaseEntity {

    @Column(name = "user_id", nullable = false, unique = true)
    private UUID userId;

    @Column(name = "membership_number", length = 20)
    private String membershipNumber;

    @Column(name = "city", length = 100)
    private String city;

    @Column(name = "birth_date")
    private LocalDate birthDate;

    @Column(name = "avatar_url", columnDefinition = "text")
    private String avatarUrl;

    @Column(name = "joined_at", nullable = false)
    private OffsetDateTime joinedAt = OffsetDateTime.now();

    protected FanProfile() {
        // JPA
    }

    public FanProfile(UUID userId) {
        this.userId = userId;
    }

    public UUID getUserId() {
        return userId;
    }

    public String getMembershipNumber() {
        return membershipNumber;
    }

    public String getCity() {
        return city;
    }

    public void setCity(String city) {
        this.city = city;
    }

    public LocalDate getBirthDate() {
        return birthDate;
    }

    public void setBirthDate(LocalDate birthDate) {
        this.birthDate = birthDate;
    }

    public String getAvatarUrl() {
        return avatarUrl;
    }

    public void setAvatarUrl(String avatarUrl) {
        this.avatarUrl = avatarUrl;
    }

    public OffsetDateTime getJoinedAt() {
        return joinedAt;
    }
}
