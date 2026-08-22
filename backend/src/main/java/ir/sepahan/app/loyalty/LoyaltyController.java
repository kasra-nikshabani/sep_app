package ir.sepahan.app.loyalty;

import ir.sepahan.app.users.AppUser;
import ir.sepahan.app.users.UserProvisioningService;
import java.util.List;
import java.util.UUID;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/** حساب/امتیاز/سطح/جوایز خودم -- برای هر کاربر احراز هویت‌شده (fan/vip، طبق rbac-matrix.md). */
@RestController
@RequestMapping("/api/v1/loyalty")
public class LoyaltyController {

    private final LoyaltyAccountService loyaltyAccountService;
    private final LoyaltyTransactionRepository transactionRepository;
    private final RewardService rewardService;
    private final RewardRedemptionRepository redemptionRepository;
    private final UserProvisioningService userProvisioningService;

    public LoyaltyController(LoyaltyAccountService loyaltyAccountService, LoyaltyTransactionRepository transactionRepository,
                              RewardService rewardService, RewardRedemptionRepository redemptionRepository,
                              UserProvisioningService userProvisioningService) {
        this.loyaltyAccountService = loyaltyAccountService;
        this.transactionRepository = transactionRepository;
        this.rewardService = rewardService;
        this.redemptionRepository = redemptionRepository;
        this.userProvisioningService = userProvisioningService;
    }

    @GetMapping("/account")
    public AccountResponse myAccount(@AuthenticationPrincipal Jwt jwt) {
        return AccountResponse.of(loyaltyAccountService.getOrCreateAccount(currentUserId(jwt)));
    }

    @GetMapping("/transactions")
    public List<TransactionResponse> myTransactions(@AuthenticationPrincipal Jwt jwt) {
        LoyaltyAccount account = loyaltyAccountService.getOrCreateAccount(currentUserId(jwt));
        return transactionRepository.findByAccountIdOrderByCreatedAtDesc(account.getId()).stream()
                .map(TransactionResponse::of)
                .toList();
    }

    @GetMapping("/rewards")
    public List<RewardResponse> rewards() {
        return rewardService.listActive().stream().map(RewardResponse::of).toList();
    }

    @PostMapping("/rewards/{rewardId}/redeem")
    public ResponseEntity<RedemptionResponse> redeem(@PathVariable UUID rewardId, @AuthenticationPrincipal Jwt jwt) {
        RewardRedemption redemption = rewardService.redeem(currentUserId(jwt), rewardId);
        return ResponseEntity.status(HttpStatus.CREATED).body(RedemptionResponse.of(redemption));
    }

    @GetMapping("/redemptions")
    public List<RedemptionResponse> myRedemptions(@AuthenticationPrincipal Jwt jwt) {
        LoyaltyAccount account = loyaltyAccountService.getOrCreateAccount(currentUserId(jwt));
        return redemptionRepository.findByAccountIdOrderByCreatedAtDesc(account.getId()).stream()
                .map(RedemptionResponse::of)
                .toList();
    }

    private UUID currentUserId(Jwt jwt) {
        AppUser user = userProvisioningService.ensureUserForToken(jwt);
        return user.getId();
    }
}
