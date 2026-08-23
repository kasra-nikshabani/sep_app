import { useMutation, useQuery, useQueryClient } from "@tanstack/react-query";
import { useApi } from "@/lib/api";

export type EventItem = {
  id: string;
  title: string;
  description: string | null;
  venueName: string;
  startsAt: string;
  durationMinutes: number | null;
  status: "draft" | "published" | "cancelled" | "completed";
  basePrice: string;
};

export type EventSeat = {
  id: string;
  section: string;
  rowLabel: string;
  seatNumber: string;
  status: "available" | "held" | "sold";
  price: string;
};

export type Reservation = { id: string; eventSeatId: string; expiresAt: string };
export type Ticket = { id: string; ticketNumber: string; status: "valid" | "used" | "refunded"; issuedAt: string };

export function useEvents() {
  const { apiFetch } = useApi();
  return useQuery({ queryKey: ["tickets", "events"], queryFn: () => apiFetch<EventItem[]>("/api/v1/ticketing/events") });
}

export function useEventSeats(eventId: string) {
  const { apiFetch } = useApi();
  return useQuery({
    queryKey: ["tickets", "seats", eventId],
    queryFn: () => apiFetch<EventSeat[]>(`/api/v1/ticketing/events/${eventId}/seats`),
    enabled: !!eventId,
  });
}

export function useReserveSeat() {
  const { apiFetch } = useApi();
  const qc = useQueryClient();
  return useMutation({
    mutationFn: (eventSeatId: string) =>
      apiFetch<Reservation>("/api/v1/ticketing/reservations", { method: "POST", body: JSON.stringify({ eventSeatId }) }),
    onSuccess: () => qc.invalidateQueries({ queryKey: ["tickets", "seats"] }),
  });
}

export function useCancelReservation() {
  const { apiFetch } = useApi();
  const qc = useQueryClient();
  return useMutation({
    mutationFn: (reservationId: string) => apiFetch<void>(`/api/v1/ticketing/reservations/${reservationId}`, { method: "DELETE" }),
    onSuccess: () => qc.invalidateQueries({ queryKey: ["tickets", "seats"] }),
  });
}

export function usePayReservation() {
  const { apiFetch } = useApi();
  return useMutation({
    mutationFn: (reservationId: string) =>
      apiFetch<{ paymentId: string; redirectUrl: string }>(`/api/v1/ticketing/reservations/${reservationId}/pay`, {
        method: "POST",
      }),
  });
}

export function useMyTickets() {
  const { apiFetch } = useApi();
  return useQuery({ queryKey: ["tickets", "my"], queryFn: () => apiFetch<Ticket[]>("/api/v1/ticketing/tickets") });
}
