package ir.sepahan.app.shop;

import ir.sepahan.app.payments.Payment;
import ir.sepahan.app.payments.PaymentPurpose;
import ir.sepahan.app.payments.PaymentService;
import java.math.BigDecimal;
import java.time.Duration;
import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

/**
 * چرخه‌ی عمر سفارش: pending_payment (موجودی همین‌جا کم می‌شود) -> paid (فقط با
 * PaymentSucceededEvent) -> shipped -> delivered، یا pending_payment -> cancelled/expired
 * (موجودی برمی‌گردد) -- دقیقاً هم‌خانواده‌ی ReservationService در ticketing (Phase 8/9)
 * برای یکسانی سبک بین دو ماژول (ADR-0012).
 */
@Service
public class OrderService {

    private final CartRepository cartRepository;
    private final CartItemRepository cartItemRepository;
    private final OrderRepository orderRepository;
    private final OrderItemRepository orderItemRepository;
    private final ProductVariantRepository productVariantRepository;
    private final ShippingService shippingService;
    private final CouponService couponService;
    private final PaymentService paymentService;
    private final Duration orderTtl;

    public OrderService(CartRepository cartRepository, CartItemRepository cartItemRepository,
                         OrderRepository orderRepository, OrderItemRepository orderItemRepository,
                         ProductVariantRepository productVariantRepository, ShippingService shippingService,
                         CouponService couponService, PaymentService paymentService,
                         @Value("${sepahan.shop.order-ttl-seconds:1800}") long orderTtlSeconds) {
        this.cartRepository = cartRepository;
        this.cartItemRepository = cartItemRepository;
        this.orderRepository = orderRepository;
        this.orderItemRepository = orderItemRepository;
        this.productVariantRepository = productVariantRepository;
        this.shippingService = shippingService;
        this.couponService = couponService;
        this.paymentService = paymentService;
        this.orderTtl = Duration.ofSeconds(orderTtlSeconds);
    }

    private record LineSpec(ProductVariant variant, String productName, String sku, BigDecimal unitPrice,
                             int quantity, BigDecimal subtotal) {
    }

    /**
     * سبد -> سفارش. موجودی همین‌جا (نه در لحظه‌ی پرداخت) اتمی کم می‌شود -- دقیقاً مثل
     * held شدن صندلی تئاتر در reserveSeat. اگر موجودی کافی نباشد، کل Transaction
     * Rollback می‌شود (هیچ کاهش جزئی نمی‌ماند).
     */
    @Transactional
    public Order checkout(UUID userId, CheckoutRequest request) {
        Cart cart = cartRepository.findByUserId(userId).orElseThrow(CartEmptyException::new);
        List<CartItem> items = cartItemRepository.findByCartId(cart.getId());
        if (items.isEmpty()) {
            throw new CartEmptyException();
        }

        ShippingMethod method = shippingService.requireActiveMethod(request.shippingMethodId());

        BigDecimal subtotal = BigDecimal.ZERO;
        int totalWeightGrams = 0;
        List<LineSpec> specs = new ArrayList<>();
        for (CartItem cartItem : items) {
            ProductVariant variant = cartItem.getProductVariant();
            BigDecimal unitPrice = variant.getEffectivePrice();
            BigDecimal lineSubtotal = unitPrice.multiply(BigDecimal.valueOf(cartItem.getQuantity()));
            subtotal = subtotal.add(lineSubtotal);
            totalWeightGrams += variant.getWeightGrams() * cartItem.getQuantity();
            specs.add(new LineSpec(variant, variant.getProduct().getName(), variant.getSku(), unitPrice,
                    cartItem.getQuantity(), lineSubtotal));
        }

        Coupon coupon = null;
        BigDecimal discount = BigDecimal.ZERO;
        if (request.couponCode() != null && !request.couponCode().isBlank()) {
            coupon = couponService.validateForRedemption(request.couponCode(), userId, subtotal);
            discount = couponService.calculateDiscount(coupon, subtotal);
        }

        BigDecimal shippingCost = shippingService.calculateCost(method, totalWeightGrams);
        BigDecimal total = subtotal.subtract(discount).add(shippingCost);
        if (total.compareTo(BigDecimal.ZERO) < 0) {
            total = BigDecimal.ZERO;
        }

        Order order = new Order(userId, subtotal, discount, shippingCost, total, coupon, method,
                request.recipientName(), request.phone(), request.province(), request.city(),
                request.addressLine(), request.postalCode(), OffsetDateTime.now().plus(orderTtl));
        order = orderRepository.save(order);

        for (LineSpec spec : specs) {
            int affected = productVariantRepository.decrementStock(spec.variant().getId(), spec.quantity());
            if (affected == 0) {
                throw new InsufficientStockException(spec.sku());
            }
            orderItemRepository.save(new OrderItem(order, spec.variant(), spec.productName(), spec.sku(),
                    spec.unitPrice(), spec.quantity(), spec.subtotal()));
        }

        if (coupon != null) {
            couponService.recordRedemption(coupon, order, userId);
        }

        cartItemRepository.deleteByCartId(cart.getId());
        return order;
    }

    @Transactional
    public Payment initiatePayment(UUID orderId, UUID userId) {
        Order order = orderRepository.findByIdAndUserIdAndDeletedAtIsNull(orderId, userId)
                .orElseThrow(() -> new OrderNotFoundException(orderId));
        if (order.getStatus() != OrderStatus.pending_payment) {
            throw new OrderStateException("این سفارش در وضعیت قابل پرداخت نیست");
        }
        if (order.getExpiresAt() != null && order.getExpiresAt().isBefore(OffsetDateTime.now())) {
            releaseOrder(order, OrderStatus.expired);
            throw new OrderStateException("مهلت پرداخت این سفارش تمام شده -- دوباره سفارش دهید");
        }
        return paymentService.createPayment(PaymentPurpose.shop_order, orderId, userId, order.getTotalAmount(),
                "خرید از فروشگاه سپاهان");
    }

    @Transactional
    public void cancelOrder(UUID orderId, UUID userId) {
        Order order = orderRepository.findByIdAndUserIdAndDeletedAtIsNull(orderId, userId)
                .orElseThrow(() -> new OrderNotFoundException(orderId));
        if (order.getStatus() != OrderStatus.pending_payment) {
            throw new OrderStateException("فقط سفارش‌های در انتظار پرداخت قابل لغو هستند");
        }
        releaseOrder(order, OrderStatus.cancelled);
    }

    private void releaseOrder(Order order, OrderStatus newStatus) {
        for (OrderItem item : orderItemRepository.findByOrderId(order.getId())) {
            productVariantRepository.incrementStock(item.getProductVariant().getId(), item.getQuantity());
        }
        order.setStatus(newStatus);
        orderRepository.save(order);
    }

    /**
     * فقط توسط OrderPaymentListener بعد از PaymentSucceededEvent صدا زده می‌شود.
     * REQUIRES_NEW طبق همان دلیل ReservationService.issueTicket در Phase 9 -- این متد
     * از داخل یک Listener در فاز AFTER_COMMIT صدا زده می‌شود، نه یک Transaction عادی.
     */
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void markOrderPaid(UUID orderId) {
        Order order = orderRepository.findByIdAndDeletedAtIsNull(orderId).orElse(null);
        if (order == null || order.getStatus() == OrderStatus.paid) {
            return; // Idempotent
        }
        if (order.getStatus() != OrderStatus.pending_payment) {
            // پرداخت دیرهنگام بعد از این‌که سفارش قبلاً منقضی/لغو شده و موجودی آزاد شده --
            // پول واقعی گرفته شده اما موجودی دیگر تضمین نیست؛ هرگز خودکار paid نمی‌شود
            // (طبق ADR-0012) -- باید دستی توسط ادمین بررسی شود.
            order.setRequiresManualReview(true);
            orderRepository.save(order);
            return;
        }
        order.setStatus(OrderStatus.paid);
        orderRepository.save(order);
    }

    @Transactional
    public void markShipped(UUID orderId) {
        Order order = orderRepository.findByIdAndDeletedAtIsNull(orderId)
                .orElseThrow(() -> new OrderNotFoundException(orderId));
        if (order.getStatus() != OrderStatus.paid) {
            throw new OrderStateException("فقط سفارش پرداخت‌شده قابل ارسال است");
        }
        order.setStatus(OrderStatus.shipped);
        orderRepository.save(order);
    }

    @Transactional
    public void markDelivered(UUID orderId) {
        Order order = orderRepository.findByIdAndDeletedAtIsNull(orderId)
                .orElseThrow(() -> new OrderNotFoundException(orderId));
        if (order.getStatus() != OrderStatus.shipped) {
            throw new OrderStateException("فقط سفارش ارسال‌شده قابل تحویل است");
        }
        order.setStatus(OrderStatus.delivered);
        orderRepository.save(order);
    }

    /** پاک‌سازی دوره‌ای سفارش‌های منقضی -- دقیقاً همان ایده‌ی releaseExpiredReservations در ticketing. */
    @Scheduled(fixedDelayString = "${sepahan.shop.cleanup-interval-ms:60000}")
    @Transactional
    public void releaseExpiredOrders() {
        List<Order> expired = orderRepository.findByStatusAndExpiresAtBefore(OrderStatus.pending_payment, OffsetDateTime.now());
        for (Order order : expired) {
            releaseOrder(order, OrderStatus.expired);
        }
    }
}
