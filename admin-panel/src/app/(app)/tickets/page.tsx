import { backendFetch } from "@/lib/backend";
import { TicketsAdmin, type Event } from "./TicketsAdmin";

export default async function TicketsPage() {
  const events = await backendFetch<Event[]>("/api/v1/ticketing/events");
  return (
    <>
      <h2 style={{ marginBottom: 16 }}>بلیط (تئاتر داخلی)</h2>
      <TicketsAdmin events={events} />
    </>
  );
}
