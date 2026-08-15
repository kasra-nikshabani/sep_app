package ir.sepahan.app.users;

import ir.sepahan.app.fan.FanProfile;
import ir.sepahan.app.fan.FanProfileRepository;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/users")
public class UserController {

    private final UserProvisioningService provisioningService;
    private final FanProfileRepository fanProfileRepository;

    public UserController(UserProvisioningService provisioningService, FanProfileRepository fanProfileRepository) {
        this.provisioningService = provisioningService;
        this.fanProfileRepository = fanProfileRepository;
    }

    @GetMapping("/me")
    public MeResponse me(@AuthenticationPrincipal Jwt jwt) {
        AppUser user = provisioningService.ensureUserForToken(jwt);
        FanProfile fan = fanProfileRepository.findByUserIdAndDeletedAtIsNull(user.getId())
                .orElseThrow(() -> new IllegalStateException("fan profile باید تا این نقطه توسط provisioning ساخته شده باشد"));
        return MeResponse.of(user, fan);
    }
}
