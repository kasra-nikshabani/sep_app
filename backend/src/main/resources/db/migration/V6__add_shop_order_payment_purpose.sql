-- افزودن مقدار جدید shop_order به PaymentPurpose (Phase 10, ADR-0012) -- V4 قبلاً
-- Apply شده، پس اصلاح مستقیم آن مجاز نیست (طبق قاعده‌ی Flyway)؛ این Migration جدید
-- محدودیت قبلی را جایگزین می‌کند.

ALTER TABLE payments.payment DROP CONSTRAINT chk_payment_purpose;
ALTER TABLE payments.payment ADD CONSTRAINT chk_payment_purpose
    CHECK (purpose IN ('ticket_purchase', 'wallet_topup', 'shop_order'));
