package ir.sepahan.app.ticketing;

import jakarta.validation.Valid;
import java.util.List;
import java.util.UUID;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

/**
 * مدیریت سالن/رویداد -- فقط نقش admin (طبق docs/authentication/rbac-matrix.md).
 * پیاده‌سازی حداقلی برای Seed کردن داده‌ی تست این فاز؛ Admin Panel واقعی موضوع Phase 14 است.
 */
@RestController
@RequestMapping("/api/v1/ticketing/admin")
@PreAuthorize("hasRole('admin')")
public class TicketingAdminController {

    private final VenueRepository venueRepository;
    private final VenueSeatRepository venueSeatRepository;
    private final EventRepository eventRepository;
    private final EventSeatRepository eventSeatRepository;

    public TicketingAdminController(VenueRepository venueRepository, VenueSeatRepository venueSeatRepository,
                                     EventRepository eventRepository, EventSeatRepository eventSeatRepository) {
        this.venueRepository = venueRepository;
        this.venueSeatRepository = venueSeatRepository;
        this.eventRepository = eventRepository;
        this.eventSeatRepository = eventSeatRepository;
    }

    @PostMapping("/venues")
    public ResponseEntity<UUID> createVenue(@Valid @RequestBody CreateVenueRequest request) {
        Venue venue = venueRepository.save(new Venue(request.name(), request.city(), request.address()));
        return ResponseEntity.status(HttpStatus.CREATED).body(venue.getId());
    }

    @PostMapping("/venues/{venueId}/seats")
    public ResponseEntity<Integer> addSeats(@PathVariable UUID venueId, @Valid @RequestBody CreateVenueSeatsRequest request) {
        Venue venue = venueRepository.findById(venueId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "سالن یافت نشد"));

        List<VenueSeat> seats = request.seats().stream()
                .map(spec -> new VenueSeat(venue, spec.section(), spec.rowLabel(), spec.seatNumber()))
                .toList();
        venueSeatRepository.saveAll(seats);
        return ResponseEntity.status(HttpStatus.CREATED).body(seats.size());
    }

    /** ساخت Event و نمونه‌سازی خودکار EventSeat برای هر VenueSeat سالن (طبق قیمت پایه). */
    @PostMapping("/events")
    public ResponseEntity<EventResponse> createEvent(@Valid @RequestBody CreateEventRequest request) {
        Venue venue = venueRepository.findById(request.venueId())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "سالن یافت نشد"));

        Event event = new Event(venue, request.title(), request.startsAt(), request.basePrice());
        event.setDescription(request.description());
        if (request.durationMinutes() != null) {
            event.setDurationMinutes(request.durationMinutes());
        }
        event.setStatus(EventStatus.published);
        eventRepository.save(event);

        List<VenueSeat> venueSeats = venueSeatRepository.findByVenueIdAndDeletedAtIsNull(venue.getId());
        List<EventSeat> eventSeats = venueSeats.stream()
                .map(seat -> new EventSeat(event, seat, request.basePrice()))
                .toList();
        eventSeatRepository.saveAll(eventSeats);

        return ResponseEntity.status(HttpStatus.CREATED).body(EventResponse.of(event));
    }
}
