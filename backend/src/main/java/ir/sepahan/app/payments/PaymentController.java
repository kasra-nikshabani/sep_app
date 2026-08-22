package ir.sepahan.app.payments;

import java.util.UUID;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * فقط شامل Endpoint عمومی Callback -- طبق ADR-0011، ساخت Payment از داخل ماژول‌های
 * دامنه (مثلاً ticketing) صدا زده می‌شود، نه از این Controller.
 *
 * این مسیر باید بدون Auth در دسترس باشد (طبق SecurityConfig) چون Zibal مرورگر کاربر را
 * این‌جا Redirect می‌کند، نه یک درخواست Bearer-احرازشده. امنیت واقعی از طریق Verify
 * سرور-به-سرور تأمین می‌شود، نه از طریق محافظت این مسیر.
 */
@RestController
@RequestMapping("/api/v1/payments")
public class PaymentController {

    private final PaymentService paymentService;

    public PaymentController(PaymentService paymentService) {
        this.paymentService = paymentService;
    }

    @GetMapping("/{paymentId}/callback")
    public PaymentCallbackResponse callback(@PathVariable UUID paymentId) {
        Payment payment = paymentService.handleCallback(paymentId);
        return PaymentCallbackResponse.of(payment);
    }
}
