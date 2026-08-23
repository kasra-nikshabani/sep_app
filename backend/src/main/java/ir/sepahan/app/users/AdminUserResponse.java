package ir.sepahan.app.users;

import ir.sepahan.app.fan.FanProfile;
import java.time.OffsetDateTime;
import java.util.UUID;

public record AdminUserResponse(
        UUID id,
        String nationalCode,
        String phoneNumber,
        String displayName,
        AppUserStatus status,
        AppUserSource source,
        String membershipNumber,
        String city,
        OffsetDateTime joinedAt,
        OffsetDateTime createdAt) {

    public static AdminUserResponse of(AppUser user, FanProfile fan) {
        return new AdminUserResponse(
                user.getId(),
                user.getNationalCode(),
                user.getPhoneNumber(),
                user.getDisplayName(),
                user.getStatus(),
                user.getSource(),
                fan != null ? fan.getMembershipNumber() : null,
                fan != null ? fan.getCity() : null,
                fan != null ? fan.getJoinedAt() : null,
                user.getCreatedAt());
    }
}
