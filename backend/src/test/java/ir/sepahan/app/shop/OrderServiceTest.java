package ir.sepahan.app.shop;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import ir.sepahan.app.payments.Payment;
import ir.sepahan.app.payments.PaymentRepository;
import ir.sepahan.app.payments.PaymentService;
import ir.sepahan.app.payments.PaymentStatus;
import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

/**
 * این تست‌ها به Postgres واقعی نیاز دارند (طبق backend/README.md) -- عمداً بدون
 * Mock، دقیقاً هم‌سبک ReservationServiceTest (Phase 8/9) چون هدف اصلی اثبات رفتار
 * واقعی کاهش اتمی موجودی و جریان کامل Event-driven پرداخت است (ADR-0012).
 */
@SpringBootTest
class OrderServiceTest {

    @Autowired
    private CategoryRepository categoryRepository;
    @Autowired
    private ProductRepository productRepository;
    @Autowired
    private ProductVariantRepository productVariantRepository;
    @Autowired
    private ShippingMethodRepository shippingMethodRepository;
    @Autowired
    private CouponRepository couponRepository;
    @Autowired
    private CouponRedemptionRepository couponRedemptionRepository;
    @Autowired
    private CartRepository cartRepository;
    @Autowired
    private CartItemRepository cartItemRepository;
    @Autowired
    private OrderRepository orderRepository;
    @Autowired
    private OrderItemRepository orderItemRepository;
    @Autowired
    private ReturnRequestRepository returnRequestRepository;
    @Autowired
    private PaymentRepository paymentRepository;
    @Autowired
    private CartService cartService;
    @Autowired
    private OrderService orderService;
    @Autowired
    private CouponService couponService;
    @Autowired
    private ReturnService returnService;
    @Autowired
    private PaymentService paymentService;

    private Category category;
    private Product product;
    private ShippingMethod shippingMethod;

    @BeforeEach
    void setUp() {
        category = categoryRepository.save(new Category("پوشاک", "clothing-" + System.nanoTime(), null));
        product = productRepository.save(new Product(category, "پیراهن تست فاز ۱۰", "jersey-" + System.nanoTime(),
                new BigDecimal("1200000")));
        shippingMethod = shippingMethodRepository.save(new ShippingMethod("پست پیشتاز", new BigDecimal("50000"), new BigDecimal("20000")));
    }

    @AfterEach
    void tearDown() {
        paymentRepository.deleteAll();
        returnRequestRepository.deleteAll();
        couponRedemptionRepository.deleteAll();
        orderItemRepository.deleteAll();
        orderRepository.deleteAll();
        cartItemRepository.deleteAll();
        cartRepository.deleteAll();
        couponRepository.deleteAll();
        productVariantRepository.deleteAll();
        productRepository.deleteAll();
        shippingMethodRepository.deleteAll();
        categoryRepository.deleteAll();
    }

    private ProductVariant newVariant(int stock) {
        return productVariantRepository.save(
                new ProductVariant(product, "SKU-" + System.nanoTime(), java.util.Map.of("size", "L"), stock));
    }

    private CheckoutRequest checkoutRequest(UUID shippingMethodId, String couponCode) {
        return new CheckoutRequest(shippingMethodId, couponCode, "کاربر تست", "09120000000",
                "اصفهان", "اصفهان", "خیابان تست، پلاک ۱", "8100000000");
    }

    @Test
    void checkout_decrementsStock_andCreatesOrderWithCorrectTotals() {
        ProductVariant variant = newVariant(5);
        UUID userId = UUID.randomUUID();
        cartService.addItem(userId, variant.getId(), 2);

        Order order = orderService.checkout(userId, checkoutRequest(shippingMethod.getId(), null));

        assertThat(order.getStatus()).isEqualTo(OrderStatus.pending_payment);
        assertThat(order.getSubtotalAmount()).isEqualByComparingTo("2400000");
        // shippingAmount = baseRate(50000) + perKgRate(20000) * weightKg(0.5*2=1.0) = 70000
        assertThat(order.getShippingAmount()).isEqualByComparingTo("70000");
        assertThat(order.getTotalAmount()).isEqualByComparingTo("2470000");
        assertThat(productVariantRepository.findById(variant.getId()).orElseThrow().getStockQuantity()).isEqualTo(3);
        assertThat(cartItemRepository.findByCartId(cartService.getOrCreateCart(userId).getId())).isEmpty();
    }

    @Test
    void checkout_rejectsEmptyCart() {
        UUID userId = UUID.randomUUID();
        assertThatThrownBy(() -> orderService.checkout(userId, checkoutRequest(shippingMethod.getId(), null)))
                .isInstanceOf(CartEmptyException.class);
    }

    @Test
    void payAndCallback_marksOrderPaid_viaPaymentSucceededEvent() {
        ProductVariant variant = newVariant(5);
        UUID userId = UUID.randomUUID();
        cartService.addItem(userId, variant.getId(), 1);
        Order order = orderService.checkout(userId, checkoutRequest(shippingMethod.getId(), null));

        Payment payment = orderService.initiatePayment(order.getId(), userId);
        assertThat(payment.getStatus()).isEqualTo(PaymentStatus.pending);

        paymentService.handleCallback(payment.getId());

        Order afterPayment = orderRepository.findById(order.getId()).orElseThrow();
        assertThat(afterPayment.getStatus()).isEqualTo(OrderStatus.paid);
        assertThat(afterPayment.isRequiresManualReview()).isFalse();
    }

    @Test
    void cancelOrder_onlyWhenPendingPayment_restoresStock() {
        ProductVariant variant = newVariant(5);
        UUID userId = UUID.randomUUID();
        cartService.addItem(userId, variant.getId(), 2);
        Order order = orderService.checkout(userId, checkoutRequest(shippingMethod.getId(), null));
        assertThat(productVariantRepository.findById(variant.getId()).orElseThrow().getStockQuantity()).isEqualTo(3);

        orderService.cancelOrder(order.getId(), userId);

        assertThat(orderRepository.findById(order.getId()).orElseThrow().getStatus()).isEqualTo(OrderStatus.cancelled);
        assertThat(productVariantRepository.findById(variant.getId()).orElseThrow().getStockQuantity()).isEqualTo(5);
    }

    @Test
    void cancelOrder_rejectsWhenAlreadyPaid() {
        ProductVariant variant = newVariant(5);
        UUID userId = UUID.randomUUID();
        cartService.addItem(userId, variant.getId(), 1);
        Order order = orderService.checkout(userId, checkoutRequest(shippingMethod.getId(), null));
        Payment payment = orderService.initiatePayment(order.getId(), userId);
        paymentService.handleCallback(payment.getId());

        assertThatThrownBy(() -> orderService.cancelOrder(order.getId(), userId))
                .isInstanceOf(OrderStateException.class);
    }

    @Test
    void releaseExpiredOrders_restoresStock_andMarksExpired() {
        ProductVariant variant = newVariant(5);
        UUID userId = UUID.randomUUID();
        cartService.addItem(userId, variant.getId(), 2);
        Order order = orderService.checkout(userId, checkoutRequest(shippingMethod.getId(), null));
        // مستقیم منقضی می‌کنیم (بدون صبر واقعی برای TTL) -- دقیقاً مثل releaseExpiredReservations test
        order.setStatus(OrderStatus.pending_payment);
        setExpired(order);

        orderService.releaseExpiredOrders();

        assertThat(orderRepository.findById(order.getId()).orElseThrow().getStatus()).isEqualTo(OrderStatus.expired);
        assertThat(productVariantRepository.findById(variant.getId()).orElseThrow().getStockQuantity()).isEqualTo(5);
    }

    private void setExpired(Order order) {
        // به‌جای صبر واقعی برای TTL، مستقیم روی دیتابیس expires_at را در گذشته می‌گذاریم
        orderRepository.findById(order.getId()).ifPresent(o -> {
            try {
                var field = Order.class.getDeclaredField("expiresAt");
                field.setAccessible(true);
                field.set(o, OffsetDateTime.now().minusMinutes(1));
                orderRepository.save(o);
            } catch (ReflectiveOperationException e) {
                throw new RuntimeException(e);
            }
        });
    }

    @Test
    void coupon_percentageDiscount_appliedCorrectly() {
        Coupon coupon = couponRepository.save(new Coupon("SEPAHAN10", CouponDiscountType.percentage, new BigDecimal("10")));
        ProductVariant variant = newVariant(5);
        UUID userId = UUID.randomUUID();
        cartService.addItem(userId, variant.getId(), 1);

        Order order = orderService.checkout(userId, checkoutRequest(shippingMethod.getId(), coupon.getCode()));

        assertThat(order.getDiscountAmount()).isEqualByComparingTo("120000"); // 10% از 1200000
        assertThat(couponRedemptionRepository.countByCouponId(coupon.getId())).isEqualTo(1);
    }

    @Test
    void coupon_rejectsWhenMaxUsesTotalExceeded() {
        Coupon newCoupon = new Coupon("LIMITED1", CouponDiscountType.fixed_amount, new BigDecimal("100000"));
        newCoupon.setMaxUsesTotal(1);
        final Coupon coupon = couponRepository.save(newCoupon);
        ProductVariant variant1 = newVariant(5);
        UUID firstUser = UUID.randomUUID();
        cartService.addItem(firstUser, variant1.getId(), 1);
        orderService.checkout(firstUser, checkoutRequest(shippingMethod.getId(), coupon.getCode()));

        ProductVariant variant2 = newVariant(5);
        UUID secondUser = UUID.randomUUID();
        cartService.addItem(secondUser, variant2.getId(), 1);
        assertThatThrownBy(() -> orderService.checkout(secondUser, checkoutRequest(shippingMethod.getId(), coupon.getCode())))
                .isInstanceOf(CouponInvalidException.class);
    }

    @Test
    void returnWorkflow_requestApproveComplete_updatesOrderStatus_withNoAutomaticRefund() {
        ProductVariant variant = newVariant(5);
        UUID userId = UUID.randomUUID();
        cartService.addItem(userId, variant.getId(), 1);
        Order order = orderService.checkout(userId, checkoutRequest(shippingMethod.getId(), null));
        Payment payment = orderService.initiatePayment(order.getId(), userId);
        paymentService.handleCallback(payment.getId());

        ReturnRequest request = returnService.requestReturn(order.getId(), userId, "سایز مناسب نبود");
        assertThat(orderRepository.findById(order.getId()).orElseThrow().getStatus()).isEqualTo(OrderStatus.return_requested);

        returnService.approve(request.getId(), UUID.randomUUID(), "تأیید شد");
        assertThat(returnRequestRepository.findById(request.getId()).orElseThrow().getStatus()).isEqualTo(ReturnStatus.approved);

        returnService.complete(request.getId(), UUID.randomUUID(), "کالا دریافت و به‌صورت دستی تسویه شد");
        assertThat(orderRepository.findById(order.getId()).orElseThrow().getStatus()).isEqualTo(OrderStatus.returned);
    }

    @Test
    void returnWorkflow_reject_revertsOrderToPreviousStatus() {
        ProductVariant variant = newVariant(5);
        UUID userId = UUID.randomUUID();
        cartService.addItem(userId, variant.getId(), 1);
        Order order = orderService.checkout(userId, checkoutRequest(shippingMethod.getId(), null));
        Payment payment = orderService.initiatePayment(order.getId(), userId);
        paymentService.handleCallback(payment.getId());

        ReturnRequest request = returnService.requestReturn(order.getId(), userId, "منصرف شدم");
        returnService.reject(request.getId(), UUID.randomUUID(), "خارج از مهلت مرجوعی");

        assertThat(orderRepository.findById(order.getId()).orElseThrow().getStatus()).isEqualTo(OrderStatus.paid);
    }

    /**
     * هسته‌ی اصلی ADR-0012: ده‌ها خرید واقعاً هم‌زمان روی یک Variant با موجودی ۱ --
     * باید فقط یکی موفق شود. برخلاف قفل Redis صندلی تئاتر (ADR-0010)، این‌جا تضمین
     * فقط از UPDATE اتمی دیتابیس می‌آید (بدون Redis)، چون موجودی یک Pool تعداد است.
     */
    @Test
    void concurrentCheckouts_onLastUnitOfStock_onlyOneSucceeds() throws InterruptedException {
        ProductVariant variant = newVariant(1);
        int attempts = 20;
        ExecutorService pool = Executors.newFixedThreadPool(attempts);
        CountDownLatch startLine = new CountDownLatch(1);
        CountDownLatch finishLine = new CountDownLatch(attempts);
        AtomicInteger succeeded = new AtomicInteger();
        AtomicInteger rejected = new AtomicInteger();

        for (int i = 0; i < attempts; i++) {
            pool.submit(() -> {
                UUID userId = UUID.randomUUID();
                try {
                    startLine.await();
                    cartService.addItem(userId, variant.getId(), 1);
                    orderService.checkout(userId, checkoutRequest(shippingMethod.getId(), null));
                    succeeded.incrementAndGet();
                } catch (InsufficientStockException expected) {
                    rejected.incrementAndGet();
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                } finally {
                    finishLine.countDown();
                }
            });
        }

        startLine.countDown();
        boolean finished = finishLine.await(30, TimeUnit.SECONDS);
        pool.shutdown();

        assertThat(finished).as("تمام تلاش‌ها باید در بازه‌ی زمانی معقول تمام شوند").isTrue();
        assertThat(succeeded.get()).as("دقیقاً یکی باید موفق شود").isEqualTo(1);
        assertThat(productVariantRepository.findById(variant.getId()).orElseThrow().getStockQuantity()).isEqualTo(0);
        List<Order> orders = orderRepository.findAll();
        assertThat(orders).hasSize(1);
    }
}
