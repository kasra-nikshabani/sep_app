package ir.sepahan.app.ticketing;

import ir.sepahan.app.users.AppUser;
import ir.sepahan.app.users.UserProvisioningService;
import jakarta.validation.Valid;
import java.util.List;
import java.util.UUID;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

/** مرور رویدادها و رزرو/لغو/تکمیل خرید صندلی -- برای هر کاربر احراز هویت‌شده (نقش fan/vip). */
@RestController
@RequestMapping("/api/v1/ticketing")
public class TicketingController {

    private final EventRepository eventRepository;
    private final EventSeatRepository eventSeatRepository;
    private final ReservationService reservationService;
    private final UserProvisioningService userProvisioningService;

    public TicketingController(EventRepository eventRepository, EventSeatRepository eventSeatRepository,
                                ReservationService reservationService, UserProvisioningService userProvisioningService) {
        this.eventRepository = eventRepository;
        this.eventSeatRepository = eventSeatRepository;
        this.reservationService = reservationService;
        this.userProvisioningService = userProvisioningService;
    }

    @GetMapping("/events")
    public List<EventResponse> listEvents() {
        return eventRepository.findByStatusAndDeletedAtIsNullOrderByStartsAtAsc(EventStatus.published).stream()
                .map(EventResponse::of)
                .toList();
    }

    @GetMapping("/events/{eventId}/seats")
    public List<EventSeatResponse> eventSeats(@PathVariable UUID eventId) {
        return eventSeatRepository.findByEventId(eventId).stream()
                .map(EventSeatResponse::of)
                .toList();
    }

    @PostMapping("/reservations")
    public ResponseEntity<ReservationResponse> reserve(@Valid @RequestBody ReserveSeatRequest request,
                                                         @AuthenticationPrincipal Jwt jwt) {
        UUID userId = currentUserId(jwt);
        Reservation reservation = reservationService.reserveSeat(request.eventSeatId(), userId);
        return ResponseEntity.status(HttpStatus.CREATED).body(ReservationResponse.of(reservation));
    }

    @DeleteMapping("/reservations/{reservationId}")
    public ResponseEntity<Void> cancel(@PathVariable UUID reservationId, @AuthenticationPrincipal Jwt jwt) {
        reservationService.cancelReservation(reservationId, currentUserId(jwt));
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/reservations/{reservationId}/confirm")
    public TicketResponse confirm(@PathVariable UUID reservationId, @AuthenticationPrincipal Jwt jwt) {
        Ticket ticket = reservationService.confirmPurchase(reservationId, currentUserId(jwt));
        return TicketResponse.of(ticket);
    }

    private UUID currentUserId(Jwt jwt) {
        AppUser user = userProvisioningService.ensureUserForToken(jwt);
        return user.getId();
    }
}
