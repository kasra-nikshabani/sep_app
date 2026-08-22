package ir.sepahan.app.loyalty;

import ir.sepahan.app.users.AppUser;
import ir.sepahan.app.users.UserProvisioningService;
import jakarta.validation.Valid;
import java.util.List;
import java.util.UUID;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * تعریف برنامه‌ی وفاداری (سطوح/نرخ امتیاز/جوایز) + گردش‌کار مبادله -- فقط نقش admin
 * (طبق rbac-matrix.md). پیاده‌سازی حداقلی برای این فاز؛ Admin Panel واقعی Phase 14 است.
 */
@RestController
@RequestMapping("/api/v1/loyalty/admin")
@PreAuthorize("hasRole('admin')")
public class LoyaltyAdminController {

    private final LoyaltyLevelRepository levelRepository;
    private final PointsEarningRuleRepository earningRuleRepository;
    private final LoyaltyRewardRepository rewardRepository;
    private final RewardRedemptionRepository redemptionRepository;
    private final LoyaltyAccountService loyaltyAccountService;
    private final RewardService rewardService;
    private final UserProvisioningService userProvisioningService;

    public LoyaltyAdminController(LoyaltyLevelRepository levelRepository, PointsEarningRuleRepository earningRuleRepository,
                                   LoyaltyRewardRepository rewardRepository, RewardRedemptionRepository redemptionRepository,
                                   LoyaltyAccountService loyaltyAccountService, RewardService rewardService,
                                   UserProvisioningService userProvisioningService) {
        this.levelRepository = levelRepository;
        this.earningRuleRepository = earningRuleRepository;
        this.rewardRepository = rewardRepository;
        this.redemptionRepository = redemptionRepository;
        this.loyaltyAccountService = loyaltyAccountService;
        this.rewardService = rewardService;
        this.userProvisioningService = userProvisioningService;
    }

    @GetMapping("/levels")
    public List<LevelResponse> levels() {
        return levelRepository.findByDeletedAtIsNullOrderByMinPointsAsc().stream().map(LevelResponse::of).toList();
    }

    @PostMapping("/levels")
    public ResponseEntity<LevelResponse> createLevel(@Valid @RequestBody CreateLevelRequest request) {
        LoyaltyLevel level = levelRepository.save(new LoyaltyLevel(request.name(), request.minPoints(), request.benefits()));
        return ResponseEntity.status(HttpStatus.CREATED).body(LevelResponse.of(level));
    }

    @GetMapping("/earning-rules")
    public List<EarningRuleResponse> earningRules() {
        return earningRuleRepository.findByDeletedAtIsNull().stream().map(EarningRuleResponse::of).toList();
    }

    @PostMapping("/earning-rules")
    public ResponseEntity<EarningRuleResponse> createEarningRule(@Valid @RequestBody CreateEarningRuleRequest request) {
        PointsEarningRule rule = earningRuleRepository.save(
                new PointsEarningRule(request.sourceType(), request.pointsPerAmount()));
        return ResponseEntity.status(HttpStatus.CREATED).body(EarningRuleResponse.of(rule));
    }

    @PostMapping("/rewards")
    public ResponseEntity<RewardResponse> createReward(@Valid @RequestBody CreateRewardRequest request) {
        LoyaltyReward reward = rewardRepository.save(
                new LoyaltyReward(request.name(), request.description(), request.pointsCost(), request.stockQuantity()));
        return ResponseEntity.status(HttpStatus.CREATED).body(RewardResponse.of(reward));
    }

    @GetMapping("/redemptions")
    public List<RedemptionResponse> allRedemptions() {
        return redemptionRepository.findByOrderByCreatedAtDesc().stream().map(RedemptionResponse::of).toList();
    }

    @PostMapping("/redemptions/{redemptionId}/fulfill")
    public RedemptionResponse fulfill(@PathVariable UUID redemptionId, @AuthenticationPrincipal Jwt jwt) {
        return RedemptionResponse.of(rewardService.fulfill(redemptionId, currentUserId(jwt)));
    }

    @PostMapping("/redemptions/{redemptionId}/cancel")
    public RedemptionResponse cancel(@PathVariable UUID redemptionId, @AuthenticationPrincipal Jwt jwt) {
        return RedemptionResponse.of(rewardService.cancel(redemptionId, currentUserId(jwt)));
    }

    @PostMapping("/accounts/{userId}/adjust")
    public TransactionResponse adjustPoints(@PathVariable UUID userId, @Valid @RequestBody AdjustPointsRequest request,
                                             @AuthenticationPrincipal Jwt jwt) {
        return TransactionResponse.of(
                loyaltyAccountService.adjustPointsManually(userId, currentUserId(jwt), request.delta(), request.reason()));
    }

    private UUID currentUserId(Jwt jwt) {
        AppUser user = userProvisioningService.ensureUserForToken(jwt);
        return user.getId();
    }
}
