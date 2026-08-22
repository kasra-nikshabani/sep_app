package ir.sepahan.app.shop;

import java.time.OffsetDateTime;
import java.util.EnumSet;
import java.util.Set;
import java.util.UUID;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

/**
 * فقط گردش‌کار درخواست/تأیید/رد/تکمیل -- بدون هیچ تسویه‌ی مالی خودکار (نه Refund
 * API واقعی برای Zibal طبق ADR-0011، نه ماژول Wallet که هنوز ساخته نشده). تسویه‌ی
 * واقعی مالی فعلاً دستی/خارج از اپ توسط ادمین انجام می‌شود -- تصمیم صریح کارفرما
 * در همین فاز (ADR-0012).
 */
@Service
public class ReturnService {

    private static final Set<OrderStatus> RETURNABLE_STATUSES = EnumSet.of(
            OrderStatus.paid, OrderStatus.shipped, OrderStatus.delivered);

    private final OrderRepository orderRepository;
    private final ReturnRequestRepository returnRequestRepository;

    public ReturnService(OrderRepository orderRepository, ReturnRequestRepository returnRequestRepository) {
        this.orderRepository = orderRepository;
        this.returnRequestRepository = returnRequestRepository;
    }

    @Transactional
    public ReturnRequest requestReturn(UUID orderId, UUID userId, String reason) {
        Order order = orderRepository.findByIdAndUserIdAndDeletedAtIsNull(orderId, userId)
                .orElseThrow(() -> new OrderNotFoundException(orderId));
        if (!RETURNABLE_STATUSES.contains(order.getStatus())) {
            throw new OrderStateException("این سفارش در وضعیتی نیست که بتوان درخواست مرجوعی برایش ثبت کرد");
        }
        if (returnRequestRepository.existsByOrderIdAndStatus(orderId, ReturnStatus.requested)
                || returnRequestRepository.existsByOrderIdAndStatus(orderId, ReturnStatus.approved)) {
            throw new OrderStateException("یک درخواست مرجوعی باز برای این سفارش از قبل وجود دارد");
        }

        ReturnRequest request = new ReturnRequest(order, userId, reason, order.getStatus());
        order.setStatus(OrderStatus.return_requested);
        orderRepository.save(order);
        return returnRequestRepository.save(request);
    }

    @Transactional
    public ReturnRequest approve(UUID returnId, UUID adminUserId, String adminNote) {
        ReturnRequest request = requireRequestedReturn(returnId);
        request.setStatus(ReturnStatus.approved);
        request.setAdminNote(adminNote);
        request.setResolvedBy(adminUserId);
        request.setResolvedAt(OffsetDateTime.now());
        return returnRequestRepository.save(request);
    }

    @Transactional
    public ReturnRequest reject(UUID returnId, UUID adminUserId, String adminNote) {
        ReturnRequest request = requireRequestedReturn(returnId);
        request.setStatus(ReturnStatus.rejected);
        request.setAdminNote(adminNote);
        request.setResolvedBy(adminUserId);
        request.setResolvedAt(OffsetDateTime.now());
        returnRequestRepository.save(request);

        Order order = request.getOrder();
        order.setStatus(request.getOrderStatusBeforeReturn());
        orderRepository.save(order);
        return request;
    }

    /** ادمین تأیید می‌کند کالا فیزیکی برگشته و تسویه‌ی مالی (دستی/خارج از اپ) انجام شده. */
    @Transactional
    public ReturnRequest complete(UUID returnId, UUID adminUserId, String adminNote) {
        ReturnRequest request = returnRequestRepository.findById(returnId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "درخواست مرجوعی یافت نشد"));
        if (request.getStatus() != ReturnStatus.approved) {
            throw new OrderStateException("فقط درخواست تأییدشده قابل تکمیل است");
        }
        request.setStatus(ReturnStatus.completed);
        request.setAdminNote(adminNote);
        request.setResolvedBy(adminUserId);
        request.setResolvedAt(OffsetDateTime.now());
        returnRequestRepository.save(request);

        Order order = request.getOrder();
        order.setStatus(OrderStatus.returned);
        orderRepository.save(order);
        return request;
    }

    private ReturnRequest requireRequestedReturn(UUID returnId) {
        ReturnRequest request = returnRequestRepository.findById(returnId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "درخواست مرجوعی یافت نشد"));
        if (request.getStatus() != ReturnStatus.requested) {
            throw new OrderStateException("این درخواست مرجوعی قبلاً بررسی شده است");
        }
        return request;
    }
}
