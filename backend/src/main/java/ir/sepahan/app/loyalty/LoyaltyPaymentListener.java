package ir.sepahan.app.loyalty;

import ir.sepahan.app.payments.PaymentPurpose;
import ir.sepahan.app.payments.PaymentSucceededEvent;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

/**
 * منبع امتیاز برای خرید Shop/تئاتر -- طبق ADR-0002: «منبع رویداد: خرید Shop، خرید بلیط تئاتر».
 * هم‌الگوی OrderPaymentListener/TicketIssuanceListener؛ payments از وجود loyalty بی‌خبر است.
 */
@Component
public class LoyaltyPaymentListener {

    private final LoyaltyAccountService loyaltyAccountService;

    public LoyaltyPaymentListener(LoyaltyAccountService loyaltyAccountService) {
        this.loyaltyAccountService = loyaltyAccountService;
    }

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void onPaymentSucceeded(PaymentSucceededEvent event) {
        EarningSourceType sourceType = switch (event.purpose()) {
            case shop_order -> EarningSourceType.shop_order;
            case ticket_purchase -> EarningSourceType.ticket_purchase;
            case wallet_topup -> null; // شارژ کیف‌پول خودش خرید نیست -- امتیازی ندارد
        };
        if (sourceType != null) {
            loyaltyAccountService.earnPoints(event.userId(), sourceType, event.paymentId(), event.amount());
        }
    }
}
