package ir.sepahan.app.users;

import ir.sepahan.app.fan.FanProfileRepository;
import java.util.List;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * فهرست کاربران/هواداران برای بخش‌های Users/Fans در Admin Panel (Phase 14) --
 * فقط Read، بدون مدیریت نقش (تغییر نقش از طریق Keycloak Admin Console انجام
 * می‌شود، نه این API؛ طبق rbac-matrix.md مسئولیت «مدیریت کاربران/نقش‌ها» فقط admin است).
 */
@RestController
@RequestMapping("/api/v1/users/admin")
@PreAuthorize("hasRole('admin')")
public class UserAdminController {

    private final AppUserRepository appUserRepository;
    private final FanProfileRepository fanProfileRepository;

    public UserAdminController(AppUserRepository appUserRepository, FanProfileRepository fanProfileRepository) {
        this.appUserRepository = appUserRepository;
        this.fanProfileRepository = fanProfileRepository;
    }

    @GetMapping
    public List<AdminUserResponse> allUsers() {
        return appUserRepository.findByDeletedAtIsNullOrderByCreatedAtDesc().stream()
                .map(user -> AdminUserResponse.of(user,
                        fanProfileRepository.findByUserIdAndDeletedAtIsNull(user.getId()).orElse(null)))
                .toList();
    }
}
