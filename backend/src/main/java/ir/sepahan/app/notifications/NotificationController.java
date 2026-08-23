package ir.sepahan.app.notifications;

import ir.sepahan.app.users.AppUser;
import ir.sepahan.app.users.UserProvisioningService;
import jakarta.validation.Valid;
import java.util.UUID;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/** ثبت/لغو Device Token برای دریافت Push -- طبق rbac-matrix.md، دریافت Notification حق هر کاربر احراز هویت‌شده است. */
@RestController
@RequestMapping("/api/v1/notifications")
public class NotificationController {

    private final DeviceTokenRepository deviceTokenRepository;
    private final UserProvisioningService userProvisioningService;

    public NotificationController(DeviceTokenRepository deviceTokenRepository, UserProvisioningService userProvisioningService) {
        this.deviceTokenRepository = deviceTokenRepository;
        this.userProvisioningService = userProvisioningService;
    }

    @PostMapping("/device-tokens")
    public ResponseEntity<Void> register(@Valid @RequestBody RegisterDeviceTokenRequest request, @AuthenticationPrincipal Jwt jwt) {
        UUID userId = currentUserId(jwt);
        DeviceToken deviceToken = deviceTokenRepository.findByTokenAndDeletedAtIsNull(request.token())
                .orElseGet(() -> new DeviceToken(userId, request.token(), request.platform()));
        deviceToken.setActive(true);
        deviceTokenRepository.save(deviceToken);
        return ResponseEntity.status(HttpStatus.CREATED).build();
    }

    @DeleteMapping("/device-tokens/{token}")
    public ResponseEntity<Void> unregister(@PathVariable String token) {
        deviceTokenRepository.findByTokenAndDeletedAtIsNull(token).ifPresent(deviceToken -> {
            deviceToken.setActive(false);
            deviceTokenRepository.save(deviceToken);
        });
        return ResponseEntity.noContent().build();
    }

    private UUID currentUserId(Jwt jwt) {
        AppUser user = userProvisioningService.ensureUserForToken(jwt);
        return user.getId();
    }
}
