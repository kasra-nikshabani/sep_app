package ir.sepahan.app.shop;

import ir.sepahan.app.users.AppUser;
import ir.sepahan.app.users.UserProvisioningService;
import jakarta.validation.Valid;
import java.util.List;
import java.util.UUID;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

/** مرور کاتالوگ، سبد خرید، Checkout/پرداخت و مرجوعی -- برای هر کاربر احراز هویت‌شده (fan/vip). */
@RestController
@RequestMapping("/api/v1/shop")
public class ShopController {

    private final CategoryRepository categoryRepository;
    private final ProductRepository productRepository;
    private final ProductVariantRepository productVariantRepository;
    private final ShippingService shippingService;
    private final CartService cartService;
    private final OrderService orderService;
    private final OrderRepository orderRepository;
    private final OrderItemRepository orderItemRepository;
    private final ReturnService returnService;
    private final ReturnRequestRepository returnRequestRepository;
    private final UserProvisioningService userProvisioningService;

    public ShopController(CategoryRepository categoryRepository, ProductRepository productRepository,
                           ProductVariantRepository productVariantRepository, ShippingService shippingService,
                           CartService cartService, OrderService orderService, OrderRepository orderRepository,
                           OrderItemRepository orderItemRepository, ReturnService returnService,
                           ReturnRequestRepository returnRequestRepository,
                           UserProvisioningService userProvisioningService) {
        this.categoryRepository = categoryRepository;
        this.productRepository = productRepository;
        this.productVariantRepository = productVariantRepository;
        this.shippingService = shippingService;
        this.cartService = cartService;
        this.orderService = orderService;
        this.orderRepository = orderRepository;
        this.orderItemRepository = orderItemRepository;
        this.returnService = returnService;
        this.returnRequestRepository = returnRequestRepository;
        this.userProvisioningService = userProvisioningService;
    }

    @GetMapping("/categories")
    public List<CategoryResponse> categories() {
        return categoryRepository.findByDeletedAtIsNull().stream().map(CategoryResponse::of).toList();
    }

    @GetMapping("/products")
    public List<ProductResponse> products(@RequestParam(required = false) UUID categoryId) {
        List<Product> products = categoryId != null
                ? productRepository.findByCategoryIdAndActiveTrueAndDeletedAtIsNull(categoryId)
                : productRepository.findByActiveTrueAndDeletedAtIsNull();
        return products.stream()
                .map(p -> ProductResponse.of(p, productVariantRepository.findByProductIdAndDeletedAtIsNull(p.getId())))
                .toList();
    }

    @GetMapping("/products/{productId}")
    public ProductResponse product(@PathVariable UUID productId) {
        Product product = productRepository.findByIdAndDeletedAtIsNull(productId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "محصول یافت نشد"));
        return ProductResponse.of(product, productVariantRepository.findByProductIdAndDeletedAtIsNull(productId));
    }

    @GetMapping("/shipping-methods")
    public List<ShippingMethodResponse> shippingMethods() {
        return shippingService.listActiveMethods().stream().map(ShippingMethodResponse::of).toList();
    }

    @GetMapping("/cart")
    public List<CartItemResponse> viewCart(@AuthenticationPrincipal Jwt jwt) {
        return cartService.viewCart(currentUserId(jwt)).stream().map(CartItemResponse::of).toList();
    }

    @PostMapping("/cart/items")
    public ResponseEntity<CartItemResponse> addToCart(@Valid @RequestBody AddCartItemRequest request,
                                                        @AuthenticationPrincipal Jwt jwt) {
        CartItem item = cartService.addItem(currentUserId(jwt), request.productVariantId(), request.quantity());
        return ResponseEntity.status(HttpStatus.CREATED).body(CartItemResponse.of(item));
    }

    @PutMapping("/cart/items/{itemId}")
    public ResponseEntity<Void> updateCartItem(@PathVariable UUID itemId, @Valid @RequestBody UpdateCartItemRequest request,
                                                @AuthenticationPrincipal Jwt jwt) {
        cartService.updateItemQuantity(currentUserId(jwt), itemId, request.quantity());
        return ResponseEntity.noContent().build();
    }

    @DeleteMapping("/cart/items/{itemId}")
    public ResponseEntity<Void> removeCartItem(@PathVariable UUID itemId, @AuthenticationPrincipal Jwt jwt) {
        cartService.removeItem(currentUserId(jwt), itemId);
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/checkout")
    public ResponseEntity<OrderResponse> checkout(@Valid @RequestBody CheckoutRequest request,
                                                    @AuthenticationPrincipal Jwt jwt) {
        Order order = orderService.checkout(currentUserId(jwt), request);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(OrderResponse.of(order, orderItemRepository.findByOrderId(order.getId())));
    }

    @PostMapping("/orders/{orderId}/pay")
    public PayOrderResponse pay(@PathVariable UUID orderId, @AuthenticationPrincipal Jwt jwt) {
        return PayOrderResponse.of(orderService.initiatePayment(orderId, currentUserId(jwt)));
    }

    @DeleteMapping("/orders/{orderId}")
    public ResponseEntity<Void> cancelOrder(@PathVariable UUID orderId, @AuthenticationPrincipal Jwt jwt) {
        orderService.cancelOrder(orderId, currentUserId(jwt));
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/orders")
    public List<OrderResponse> myOrders(@AuthenticationPrincipal Jwt jwt) {
        return orderRepository.findByUserIdAndDeletedAtIsNullOrderByCreatedAtDesc(currentUserId(jwt)).stream()
                .map(order -> OrderResponse.of(order, orderItemRepository.findByOrderId(order.getId())))
                .toList();
    }

    @GetMapping("/orders/{orderId}")
    public OrderResponse orderDetail(@PathVariable UUID orderId, @AuthenticationPrincipal Jwt jwt) {
        Order order = orderRepository.findByIdAndUserIdAndDeletedAtIsNull(orderId, currentUserId(jwt))
                .orElseThrow(() -> new OrderNotFoundException(orderId));
        return OrderResponse.of(order, orderItemRepository.findByOrderId(orderId));
    }

    @PostMapping("/orders/{orderId}/returns")
    public ResponseEntity<ReturnRequestResponse> requestReturn(@PathVariable UUID orderId,
                                                                 @Valid @RequestBody RequestReturnRequest request,
                                                                 @AuthenticationPrincipal Jwt jwt) {
        ReturnRequest returnRequest = returnService.requestReturn(orderId, currentUserId(jwt), request.reason());
        return ResponseEntity.status(HttpStatus.CREATED).body(ReturnRequestResponse.of(returnRequest));
    }

    @GetMapping("/returns")
    public List<ReturnRequestResponse> myReturns(@AuthenticationPrincipal Jwt jwt) {
        return returnRequestRepository.findByUserId(currentUserId(jwt)).stream()
                .map(ReturnRequestResponse::of)
                .toList();
    }

    private UUID currentUserId(Jwt jwt) {
        AppUser user = userProvisioningService.ensureUserForToken(jwt);
        return user.getId();
    }
}
