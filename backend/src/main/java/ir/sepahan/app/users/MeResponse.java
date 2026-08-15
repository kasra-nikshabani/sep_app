package ir.sepahan.app.users;

import ir.sepahan.app.fan.FanProfile;
import java.time.OffsetDateTime;
import java.util.UUID;

public record MeResponse(
        UUID id,
        String nationalCode,
        String phoneNumber,
        String displayName,
        AppUserStatus status,
        String membershipNumber,
        String city,
        OffsetDateTime joinedAt) {

    public static MeResponse of(AppUser user, FanProfile fan) {
        return new MeResponse(
                user.getId(),
                user.getNationalCode(),
                user.getPhoneNumber(),
                user.getDisplayName(),
                user.getStatus(),
                fan.getMembershipNumber(),
                fan.getCity(),
                fan.getJoinedAt());
    }
}
