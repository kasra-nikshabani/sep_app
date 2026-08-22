package ir.sepahan.app.ticketing;

import ir.sepahan.app.payments.PaymentPurpose;
import ir.sepahan.app.payments.PaymentSucceededEvent;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

/**
 * پل بین payments و ticketing طبق ADR-0011 -- payments از وجود ticketing بی‌خبر است،
 * فقط این‌جا (سمت مصرف‌کننده) گوش داده می‌شود.
 *
 * AFTER_COMMIT عمداً انتخاب شده: تا وقتی وضعیت paid واقعاً Commit نشده، بلیط صادر
 * نمی‌شود -- جلوگیری از سناریوی نامحتمل ولی خطرناکِ صدور بلیط برای پرداختی که در
 * آخرین لحظه Rollback می‌شود.
 */
@Component
public class TicketIssuanceListener {

    private final ReservationService reservationService;

    public TicketIssuanceListener(ReservationService reservationService) {
        this.reservationService = reservationService;
    }

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void onPaymentSucceeded(PaymentSucceededEvent event) {
        if (event.purpose() == PaymentPurpose.ticket_purchase) {
            reservationService.issueTicket(event.referenceId(), event.userId());
        }
    }
}
