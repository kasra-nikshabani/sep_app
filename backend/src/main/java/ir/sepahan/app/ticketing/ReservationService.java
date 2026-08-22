package ir.sepahan.app.ticketing;

import ir.sepahan.app.payments.Payment;
import ir.sepahan.app.payments.PaymentPurpose;
import ir.sepahan.app.payments.PaymentService;
import java.math.BigDecimal;
import java.time.Duration;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.ThreadLocalRandom;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

/**
 * قفل هم‌زمانی رزرو صندلی طبق ADR-0010: Redis {@code SET NX EX} به‌عنوان
 * دروازه‌ی سریع رد/قبول (بدون فشار به دیتابیس)، دیتابیس به‌عنوان Source of
 * Truth گزارش‌گیری + یک بررسی دفاعی دوم (اگر Redis به هر دلیلی -- مثلاً
 * Restart -- خالی از قفل‌های معتبر شد، وضعیت واقعی صندلی در دیتابیس همچنان
 * درست اعمال می‌شود).
 */
@Service
public class ReservationService {

    private final StringRedisTemplate redisTemplate;
    private final EventSeatRepository eventSeatRepository;
    private final ReservationRepository reservationRepository;
    private final TicketRepository ticketRepository;
    private final PaymentService paymentService;
    private final Duration reservationTtl;

    public ReservationService(
            StringRedisTemplate redisTemplate,
            EventSeatRepository eventSeatRepository,
            ReservationRepository reservationRepository,
            TicketRepository ticketRepository,
            PaymentService paymentService,
            @Value("${sepahan.ticketing.reservation-ttl-seconds:600}") long reservationTtlSeconds) {
        this.redisTemplate = redisTemplate;
        this.eventSeatRepository = eventSeatRepository;
        this.reservationRepository = reservationRepository;
        this.ticketRepository = ticketRepository;
        this.paymentService = paymentService;
        this.reservationTtl = Duration.ofSeconds(reservationTtlSeconds);
    }

    private String lockKey(UUID eventSeatId) {
        return "seat-lock:" + eventSeatId;
    }

    @Transactional
    public Reservation reserveSeat(UUID eventSeatId, UUID userId) {
        String key = lockKey(eventSeatId);
        boolean locked = Boolean.TRUE.equals(
                redisTemplate.opsForValue().setIfAbsent(key, userId.toString(), reservationTtl));
        if (!locked) {
            throw new SeatUnavailableException("این صندلی همین الان توسط کاربر دیگری در حال رزرو یا رزروشده است");
        }

        try {
            EventSeat seat = eventSeatRepository.findById(eventSeatId)
                    .orElseThrow(() -> new SeatUnavailableException("صندلی یافت نشد"));

            if (seat.getStatus() != EventSeatStatus.available) {
                // Redis قفل را داد ولی دیتابیس چیز دیگری می‌گوید (مثلاً بعد از Restart بدون
                // قفل‌های قبلی در Redis) -- دیتابیس نهایی‌ترین منبع حقیقت است
                throw new SeatUnavailableException("این صندلی دیگر در دسترس نیست");
            }

            seat.setStatus(EventSeatStatus.held);
            OffsetDateTime expiresAt = OffsetDateTime.now().plus(reservationTtl);
            Reservation reservation = new Reservation(seat, userId, expiresAt);
            return reservationRepository.save(reservation);
        } catch (RuntimeException ex) {
            redisTemplate.delete(key);
            throw ex;
        }
    }

    @Transactional
    public void cancelReservation(UUID reservationId, UUID userId) {
        Reservation reservation = reservationRepository.findById(reservationId)
                .orElseThrow(() -> new SeatUnavailableException("رزروی با این شناسه یافت نشد"));
        if (!reservation.getUserId().equals(userId)) {
            throw new SeatUnavailableException("این رزرو متعلق به شما نیست");
        }
        releaseSeat(reservation);
    }

    /**
     * جایگزین confirmPurchase ساده‌شده‌ی Phase 8. طبق ADR-0011، ticketing مستقیم بلیط
     * صادر نمی‌کند -- فقط از PaymentService یک Payment واقعی می‌خواهد و آدرس بازگشت به
     * درگاه را برمی‌گرداند. صدور واقعی بلیط در {@link #issueTicket(UUID)} است که فقط با
     * رویداد PaymentSucceededEvent فعال می‌شود (نه مستقیم از این‌جا).
     */
    @Transactional
    public Payment initiatePayment(UUID reservationId, UUID userId) {
        Reservation reservation = reservationRepository.findById(reservationId)
                .orElseThrow(() -> new SeatUnavailableException("رزروی با این شناسه یافت نشد"));
        if (!reservation.getUserId().equals(userId)) {
            throw new SeatUnavailableException("این رزرو متعلق به شما نیست");
        }
        if (reservation.getExpiresAt().isBefore(OffsetDateTime.now())) {
            releaseSeat(reservation);
            throw new SeatUnavailableException("مهلت این رزرو تمام شده -- دوباره تلاش کنید");
        }

        BigDecimal amount = reservation.getEventSeat().getPrice();
        return paymentService.createPayment(PaymentPurpose.ticket_purchase, reservationId, userId, amount, "خرید بلیط تئاتر");
    }

    /**
     * فقط توسط TicketIssuanceListener (بعد از PaymentSucceededEvent) صدا زده می‌شود.
     * Idempotent: اگر Reservation دیگر وجود نداشته باشد (مثلاً به‌خاطر تحویل تکراری
     * Event یا Race نامحتمل)، فرض بر این است که قبلاً پردازش شده -- خطا نمی‌دهد.
     */
    /**
     * REQUIRES_NEW عمداً است، نه REQUIRED: این متد از داخل TicketIssuanceListener و
     * دقیقاً در فاز AFTER_COMMIT یک تراکنش دیگر (PaymentService.handleCallback) صدا
     * زده می‌شود. تراکنش «بیرونی» در آن لحظه هنوز کاملاً از Thread جدا/آزاد نشده؛ اگر
     * این‌جا REQUIRED می‌بود، ممکن بود این تراکنش «بپیوندد» به منابع تراکنش قبلی که
     * دارد جمع‌آوری می‌شود و تغییرات هرگز واقعاً Commit نشوند (بدون هیچ Exception ای --
     * دقیقاً همین باگ در تست واقعی این فاز رخ داد و با REQUIRES_NEW رفع شد).
     */
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void issueTicket(UUID reservationId, UUID userId) {
        Reservation reservation = reservationRepository.findById(reservationId).orElse(null);
        if (reservation == null) {
            return;
        }

        EventSeat seat = reservation.getEventSeat();
        seat.setStatus(EventSeatStatus.sold);

        String ticketNumber = generateTicketNumber();
        String qrPayload = "TICKET:" + ticketNumber; // Placeholder -- امضای رمزنگاری‌شده‌ی واقعی در فاز بعدی
        Ticket ticket = new Ticket(seat, userId, ticketNumber, qrPayload);
        ticketRepository.save(ticket);

        reservationRepository.delete(reservation);
        redisTemplate.delete(lockKey(seat.getId()));
    }

    private void releaseSeat(Reservation reservation) {
        EventSeat seat = reservation.getEventSeat();
        seat.setStatus(EventSeatStatus.available);
        reservationRepository.delete(reservation);
        redisTemplate.delete(lockKey(seat.getId()));
    }

    /** پاک‌سازی دوره‌ای رزروهای منقضی -- همان ایده‌ای که سیستم فوتبال Django برای صندلی‌های رهاشده دارد. */
    @Scheduled(fixedDelayString = "${sepahan.ticketing.cleanup-interval-ms:60000}")
    @Transactional
    public void releaseExpiredReservations() {
        List<Reservation> expired = reservationRepository.findByExpiresAtBefore(OffsetDateTime.now());
        for (Reservation reservation : expired) {
            releaseSeat(reservation);
        }
    }

    private String generateTicketNumber() {
        // فقط برای تست زیرساخت Reservation در همین فاز -- الگوریتم نهاییِ ضدحدس‌زنی
        // شماره‌ی بلیط (مشابه آنچه سیستم فوتبال دارد) موضوع فاز بعدی است.
        return "TH-" + System.currentTimeMillis() + "-" + ThreadLocalRandom.current().nextInt(1000, 9999);
    }
}
