package ir.sepahan.app.ticketing;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import ir.sepahan.app.TestcontainersConfig;
import ir.sepahan.app.payments.Payment;
import ir.sepahan.app.payments.PaymentRepository;
import ir.sepahan.app.payments.PaymentService;
import ir.sepahan.app.payments.PaymentStatus;
import java.math.BigDecimal;
import java.time.OffsetDateTime;
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
import org.springframework.context.annotation.Import;
import org.springframework.test.context.ActiveProfiles;

/**
 * به Postgres/Redis واقعی نیاز دارد تا رفتار واقعی قفل Redis تحت هم‌زمانی واقعی اثبات شود
 * (ADR-0010) -- نه Mock. از Phase 18 (ADR-0020)، این Postgres/Redis واقعی از طریق
 * Testcontainers موقت و ایزوله ساخته می‌شود (همان Image واقعی، نه Fake) -- دیگر نیازی به
 * روشن‌بودن {@code infra/} یا Export دستی Env Var نیست.
 */
@SpringBootTest
@Import(TestcontainersConfig.class)
@ActiveProfiles("test")
class ReservationServiceTest {

    @Autowired
    private ReservationService reservationService;
    @Autowired
    private VenueRepository venueRepository;
    @Autowired
    private VenueSeatRepository venueSeatRepository;
    @Autowired
    private EventRepository eventRepository;
    @Autowired
    private EventSeatRepository eventSeatRepository;
    @Autowired
    private ReservationRepository reservationRepository;
    @Autowired
    private TicketRepository ticketRepository;
    @Autowired
    private PaymentRepository paymentRepository;
    @Autowired
    private PaymentService paymentService;

    private Venue venue;
    private Event event;

    @BeforeEach
    void setUp() {
        venue = venueRepository.save(new Venue("سالن تست Phase 8", "اصفهان", null));
        event = eventRepository.save(new Event(venue, "نمایش تست", OffsetDateTime.now().plusDays(1), new BigDecimal("380000")));
    }

    @AfterEach
    void tearDown() {
        // داده‌ی هر تست را کامل پاک می‌کنیم تا دیتابیس Dev مشترک آلوده نماند
        paymentRepository.deleteAll();
        ticketRepository.deleteAll();
        reservationRepository.deleteAll();
        eventSeatRepository.deleteAll();
        eventRepository.deleteAll();
        venueSeatRepository.deleteAll();
        venueRepository.deleteAll();
    }

    private EventSeat newEventSeat() {
        VenueSeat venueSeat = venueSeatRepository.save(new VenueSeat(venue, "اصلی", "7", String.valueOf(System.nanoTime())));
        return eventSeatRepository.save(new EventSeat(event, venueSeat, new BigDecimal("380000")));
    }

    @Test
    void reserveSeat_succeeds_for_available_seat() {
        EventSeat seat = newEventSeat();
        UUID userId = UUID.randomUUID();

        Reservation reservation = reservationService.reserveSeat(seat.getId(), userId);

        assertThat(reservation.getEventSeat().getId()).isEqualTo(seat.getId());
        assertThat(eventSeatRepository.findById(seat.getId()).orElseThrow().getStatus()).isEqualTo(EventSeatStatus.held);
    }

    @Test
    void reserveSeat_rejects_second_attempt_on_same_seat() {
        EventSeat seat = newEventSeat();
        reservationService.reserveSeat(seat.getId(), UUID.randomUUID());

        assertThatThrownBy(() -> reservationService.reserveSeat(seat.getId(), UUID.randomUUID()))
                .isInstanceOf(SeatUnavailableException.class);
    }

    @Test
    void cancelReservation_releases_seat_back_to_available() {
        EventSeat seat = newEventSeat();
        UUID userId = UUID.randomUUID();
        Reservation reservation = reservationService.reserveSeat(seat.getId(), userId);

        reservationService.cancelReservation(reservation.getId(), userId);

        assertThat(eventSeatRepository.findById(seat.getId()).orElseThrow().getStatus()).isEqualTo(EventSeatStatus.available);
        assertThat(reservationRepository.findById(reservation.getId())).isEmpty();
        // بعد از لغو، صندلی باید دوباره قابل رزرو باشد (یعنی قفل Redis هم واقعاً آزاد شده)
        assertThat(reservationService.reserveSeat(seat.getId(), UUID.randomUUID())).isNotNull();
    }

    @Test
    void cancelReservation_rejects_other_users_reservation() {
        EventSeat seat = newEventSeat();
        Reservation reservation = reservationService.reserveSeat(seat.getId(), UUID.randomUUID());

        assertThatThrownBy(() -> reservationService.cancelReservation(reservation.getId(), UUID.randomUUID()))
                .isInstanceOf(SeatUnavailableException.class);
    }

    @Test
    void initiatePayment_rejects_other_users_reservation() {
        EventSeat seat = newEventSeat();
        Reservation reservation = reservationService.reserveSeat(seat.getId(), UUID.randomUUID());

        assertThatThrownBy(() -> reservationService.initiatePayment(reservation.getId(), UUID.randomUUID()))
                .isInstanceOf(SeatUnavailableException.class);
    }

    @Test
    void initiatePayment_isIdempotent_returnsSamePaymentOnRetry() {
        EventSeat seat = newEventSeat();
        UUID userId = UUID.randomUUID();
        Reservation reservation = reservationService.reserveSeat(seat.getId(), userId);

        Payment first = reservationService.initiatePayment(reservation.getId(), userId);
        Payment second = reservationService.initiatePayment(reservation.getId(), userId);

        assertThat(second.getId()).isEqualTo(first.getId());
        assertThat(paymentRepository.count()).isEqualTo(1);
    }

    /**
     * جریان کامل ADR-0011 با FakePaymentProvider (پیش‌فرض تست): initiatePayment یک
     * Payment واقعی می‌سازد؛ handleCallback (شبیه‌سازی بازگشت از درگاه) با Verify
     * موفق، PaymentSucceededEvent منتشر می‌کند؛ TicketIssuanceListener گوش می‌دهد و
     * بلیط را صادر می‌کند -- بدون این‌که ticketing مستقیم Provider را دیده باشد.
     */
    @Test
    void payAndCallback_issuesTicket_viaPaymentSucceededEvent() {
        EventSeat seat = newEventSeat();
        UUID userId = UUID.randomUUID();
        Reservation reservation = reservationService.reserveSeat(seat.getId(), userId);

        Payment payment = reservationService.initiatePayment(reservation.getId(), userId);
        assertThat(payment.getStatus()).isEqualTo(PaymentStatus.pending);
        assertThat(payment.getRedirectUrl()).isNotBlank();

        Payment afterCallback = paymentService.handleCallback(payment.getId());

        assertThat(afterCallback.getStatus()).isEqualTo(PaymentStatus.paid);
        assertThat(eventSeatRepository.findById(seat.getId()).orElseThrow().getStatus()).isEqualTo(EventSeatStatus.sold);
        assertThat(reservationRepository.findById(reservation.getId())).isEmpty();
        assertThat(ticketRepository.findByUserIdAndDeletedAtIsNull(userId)).hasSize(1);
    }

    @Test
    void handleCallback_isIdempotent_doesNotIssueDuplicateTicket() {
        EventSeat seat = newEventSeat();
        UUID userId = UUID.randomUUID();
        Reservation reservation = reservationService.reserveSeat(seat.getId(), userId);
        Payment payment = reservationService.initiatePayment(reservation.getId(), userId);

        paymentService.handleCallback(payment.getId());
        paymentService.handleCallback(payment.getId()); // شبیه‌سازی بازگشت تکراری کاربر/درگاه

        assertThat(ticketRepository.findByUserIdAndDeletedAtIsNull(userId)).hasSize(1);
    }

    /**
     * هسته‌ی اصلی ADR-0010: ده‌ها تلاش واقعاً هم‌زمان (نه پشت‌سرهم) روی دقیقاً
     * یک صندلی -- باید فقط یکی موفق شود، نه صفر و نه بیشتر از یک.
     */
    @Test
    void concurrentReservationAttempts_onlyOneSucceeds() throws InterruptedException {
        EventSeat seat = newEventSeat();
        int attempts = 25;
        ExecutorService pool = Executors.newFixedThreadPool(attempts);
        CountDownLatch startLine = new CountDownLatch(1);
        CountDownLatch finishLine = new CountDownLatch(attempts);
        AtomicInteger succeeded = new AtomicInteger();
        AtomicInteger rejected = new AtomicInteger();

        for (int i = 0; i < attempts; i++) {
            pool.submit(() -> {
                try {
                    startLine.await();
                    reservationService.reserveSeat(seat.getId(), UUID.randomUUID());
                    succeeded.incrementAndGet();
                } catch (SeatUnavailableException expected) {
                    rejected.incrementAndGet();
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                } finally {
                    finishLine.countDown();
                }
            });
        }

        startLine.countDown(); // همه‌ی Threadها تقریباً هم‌زمان شروع کنند
        boolean finished = finishLine.await(30, TimeUnit.SECONDS);
        pool.shutdown();

        assertThat(finished).as("تمام تلاش‌ها باید در بازه‌ی زمانی معقول تمام شوند").isTrue();
        assertThat(succeeded.get()).as("دقیقاً یکی باید موفق شود").isEqualTo(1);
        assertThat(rejected.get()).isEqualTo(attempts - 1);
        assertThat(eventSeatRepository.findById(seat.getId()).orElseThrow().getStatus()).isEqualTo(EventSeatStatus.held);
    }

    @Test
    void releaseExpiredReservations_frees_up_expired_seats() {
        EventSeat seat = newEventSeat();
        // مستقیم یک Reservation منقضی می‌سازیم (بدون صبر واقعی برای TTL)
        Reservation expired = reservationRepository.save(
                new Reservation(seat, UUID.randomUUID(), OffsetDateTime.now().minusMinutes(1)));
        seat.setStatus(EventSeatStatus.held);
        eventSeatRepository.save(seat);

        reservationService.releaseExpiredReservations();

        assertThat(reservationRepository.findById(expired.getId())).isEmpty();
        assertThat(eventSeatRepository.findById(seat.getId()).orElseThrow().getStatus()).isEqualTo(EventSeatStatus.available);
    }
}
