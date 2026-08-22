package ir.sepahan.app.shop;

import ir.sepahan.app.payments.PaymentPurpose;
import ir.sepahan.app.payments.PaymentSucceededEvent;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

/** پل بین payments و shop، دقیقاً هم‌الگوی TicketIssuanceListener در ticketing (ADR-0011). */
@Component
public class OrderPaymentListener {

    private final OrderService orderService;

    public OrderPaymentListener(OrderService orderService) {
        this.orderService = orderService;
    }

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void onPaymentSucceeded(PaymentSucceededEvent event) {
        if (event.purpose() == PaymentPurpose.shop_order) {
            orderService.markOrderPaid(event.referenceId());
        }
    }
}
