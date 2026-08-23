"use server";

import { revalidatePath } from "next/cache";
import { backendFetch, BackendError } from "@/lib/backend";

export type CreateVenueState = { error?: string; venueId?: string };

export async function createVenueAction(_prev: CreateVenueState, formData: FormData): Promise<CreateVenueState> {
  try {
    const venueId = await backendFetch<string>("/api/v1/ticketing/admin/venues", {
      method: "POST",
      body: JSON.stringify({
        name: String(formData.get("name") ?? ""),
        city: String(formData.get("city") ?? "") || null,
        address: String(formData.get("address") ?? "") || null,
      }),
    });
    return { venueId };
  } catch (e) {
    return { error: e instanceof BackendError ? e.message : "خطای غیرمنتظره" };
  }
}

export type ActionState = { error?: string; success?: boolean };

export async function addSeatsAction(_prev: ActionState, formData: FormData): Promise<ActionState> {
  try {
    const venueId = String(formData.get("venueId") ?? "");
    const section = String(formData.get("section") ?? "");
    const rows = Number(formData.get("rows") ?? 0);
    const seatsPerRow = Number(formData.get("seatsPerRow") ?? 0);

    const seats = [];
    for (let r = 1; r <= rows; r++) {
      for (let s = 1; s <= seatsPerRow; s++) {
        seats.push({ section, rowLabel: String(r), seatNumber: String(s) });
      }
    }

    await backendFetch(`/api/v1/ticketing/admin/venues/${venueId}/seats`, {
      method: "POST",
      body: JSON.stringify({ seats }),
    });
    return { success: true };
  } catch (e) {
    return { error: e instanceof BackendError ? e.message : "خطای غیرمنتظره" };
  }
}

export async function createEventAction(_prev: ActionState, formData: FormData): Promise<ActionState> {
  try {
    const durationMinutes = String(formData.get("durationMinutes") ?? "").trim();
    await backendFetch("/api/v1/ticketing/admin/events", {
      method: "POST",
      body: JSON.stringify({
        venueId: String(formData.get("venueId") ?? ""),
        title: String(formData.get("title") ?? ""),
        description: String(formData.get("description") ?? "") || null,
        startsAt: new Date(String(formData.get("startsAt") ?? "")).toISOString(),
        durationMinutes: durationMinutes ? Number(durationMinutes) : null,
        basePrice: String(formData.get("basePrice") ?? "0"),
      }),
    });
    revalidatePath("/tickets");
    return { success: true };
  } catch (e) {
    return { error: e instanceof BackendError ? e.message : "خطای غیرمنتظره" };
  }
}
