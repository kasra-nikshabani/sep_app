package ir.sepahan.app.payments;

import java.util.List;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * فهرست Read-only پرداخت‌ها برای بخش Payments در Admin Panel (Phase 14) --
 * صرفاً Reconciliation/مشاهده؛ زیبال API استرداد مستند/رسمی ندارد، پس هیچ
 * عملیات Refund از این مسیر انجام نمی‌شود (طبق تصمیم Returns در ADR-0012:
 * گردش‌کار Returns فقط سطح کسب‌وکار است، نه استرداد مالی خودکار).
 */
@RestController
@RequestMapping("/api/v1/payments/admin")
@PreAuthorize("hasRole('admin')")
public class PaymentAdminController {

    private final PaymentRepository paymentRepository;

    public PaymentAdminController(PaymentRepository paymentRepository) {
        this.paymentRepository = paymentRepository;
    }

    @GetMapping
    public List<AdminPaymentResponse> allPayments() {
        return paymentRepository.findByDeletedAtIsNullOrderByCreatedAtDesc().stream()
                .map(AdminPaymentResponse::of)
                .toList();
    }
}
