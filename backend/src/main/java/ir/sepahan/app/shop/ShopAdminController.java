package ir.sepahan.app.shop;

import ir.sepahan.app.users.AppUser;
import ir.sepahan.app.users.UserProvisioningService;
import jakarta.validation.Valid;
import java.math.BigDecimal;
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
 * مدیریت کاتالوگ/کد تخفیف/روش ارسال و گردش‌کار سفارش/مرجوعی -- فقط نقش admin
 * (طبق docs/authentication/rbac-matrix.md). پیاده‌سازی حداقلی برای این فاز؛
 * Admin Panel واقعی موضوع Phase 14 است (همان الگوی TicketingAdminController، Phase 8).
 */
@RestController
@RequestMapping("/api/v1/shop/admin")
@PreAuthorize("hasRole('admin')")
public class ShopAdminController {

    private final CatalogService catalogService;
    private final ShippingMethodRepository shippingMethodRepository;
    private final CouponRepository couponRepository;
    private final OrderRepository orderRepository;
    private final OrderItemRepository orderItemRepository;
    private final OrderService orderService;
    private final ReturnService returnService;
    private final UserProvisioningService userProvisioningService;

    public ShopAdminController(CatalogService catalogService, ShippingMethodRepository shippingMethodRepository,
                                CouponRepository couponRepository, OrderRepository orderRepository,
                                OrderItemRepository orderItemRepository, OrderService orderService,
                                ReturnService returnService, UserProvisioningService userProvisioningService) {
        this.catalogService = catalogService;
        this.shippingMethodRepository = shippingMethodRepository;
        this.couponRepository = couponRepository;
        this.orderRepository = orderRepository;
        this.orderItemRepository = orderItemRepository;
        this.orderService = orderService;
        this.returnService = returnService;
        this.userProvisioningService = userProvisioningService;
    }

    @PostMapping("/categories")
    public ResponseEntity<CategoryResponse> createCategory(@Valid @RequestBody CreateCategoryRequest request) {
        Category category = catalogService.createCategory(request.name(), request.slug(), request.parentId());
        return ResponseEntity.status(HttpStatus.CREATED).body(CategoryResponse.of(category));
    }

    @PostMapping("/products")
    public ResponseEntity<UUID> createProduct(@Valid @RequestBody CreateProductRequest request) {
        Product product = catalogService.createProduct(request.categoryId(), request.name(), request.slug(),
                request.description(), request.imageUrl(), request.basePrice());
        return ResponseEntity.status(HttpStatus.CREATED).body(product.getId());
    }

    @PostMapping("/products/{productId}/variants")
    public ResponseEntity<ProductVariantResponse> addVariant(@PathVariable UUID productId,
                                                               @Valid @RequestBody AddVariantRequest request) {
        int weightGrams = request.weightGrams() != null ? request.weightGrams() : 500;
        ProductVariant variant = catalogService.addVariant(productId, request.sku(), request.attributes(),
                request.initialStock(), weightGrams, request.priceOverride());
        return ResponseEntity.status(HttpStatus.CREATED).body(ProductVariantResponse.of(variant));
    }

    @PostMapping("/variants/{variantId}/stock")
    public ResponseEntity<Void> adjustStock(@PathVariable UUID variantId, @RequestBody AdjustStockRequest request) {
        catalogService.adjustStock(variantId, request.delta());
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/shipping-methods")
    public ResponseEntity<ShippingMethodResponse> createShippingMethod(@Valid @RequestBody CreateShippingMethodRequest request) {
        ShippingMethod method = shippingMethodRepository.save(new ShippingMethod(request.name(), request.baseRate(),
                request.perKgRate() != null ? request.perKgRate() : BigDecimal.ZERO));
        return ResponseEntity.status(HttpStatus.CREATED).body(ShippingMethodResponse.of(method));
    }

    @PostMapping("/coupons")
    public ResponseEntity<CouponResponse> createCoupon(@Valid @RequestBody CreateCouponRequest request) {
        Coupon coupon = new Coupon(request.code(), request.discountType(), request.discountValue());
        coupon.setMinOrderAmount(request.minOrderAmount());
        coupon.setMaxUsesTotal(request.maxUsesTotal());
        coupon.setMaxUsesPerUser(request.maxUsesPerUser());
        coupon.setValidFrom(request.validFrom());
        coupon.setValidTo(request.validTo());
        coupon = couponRepository.save(coupon);
        return ResponseEntity.status(HttpStatus.CREATED).body(CouponResponse.of(coupon));
    }

    @GetMapping("/orders")
    public List<OrderResponse> allOrders() {
        return orderRepository.findByDeletedAtIsNullOrderByCreatedAtDesc().stream()
                .map(order -> OrderResponse.of(order, orderItemRepository.findByOrderId(order.getId())))
                .toList();
    }

    @PostMapping("/orders/{orderId}/ship")
    public ResponseEntity<Void> shipOrder(@PathVariable UUID orderId) {
        orderService.markShipped(orderId);
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/orders/{orderId}/deliver")
    public ResponseEntity<Void> deliverOrder(@PathVariable UUID orderId) {
        orderService.markDelivered(orderId);
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/returns/{returnId}/approve")
    public ReturnRequestResponse approveReturn(@PathVariable UUID returnId, @RequestBody ReviewReturnRequest request,
                                                @AuthenticationPrincipal Jwt jwt) {
        return ReturnRequestResponse.of(returnService.approve(returnId, currentUserId(jwt), request.adminNote()));
    }

    @PostMapping("/returns/{returnId}/reject")
    public ReturnRequestResponse rejectReturn(@PathVariable UUID returnId, @RequestBody ReviewReturnRequest request,
                                               @AuthenticationPrincipal Jwt jwt) {
        return ReturnRequestResponse.of(returnService.reject(returnId, currentUserId(jwt), request.adminNote()));
    }

    @PostMapping("/returns/{returnId}/complete")
    public ReturnRequestResponse completeReturn(@PathVariable UUID returnId, @RequestBody ReviewReturnRequest request,
                                                 @AuthenticationPrincipal Jwt jwt) {
        return ReturnRequestResponse.of(returnService.complete(returnId, currentUserId(jwt), request.adminNote()));
    }

    private UUID currentUserId(Jwt jwt) {
        AppUser user = userProvisioningService.ensureUserForToken(jwt);
        return user.getId();
    }
}
