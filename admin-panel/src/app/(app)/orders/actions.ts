"use server";

import { revalidatePath } from "next/cache";
import { backendFetch, BackendError } from "@/lib/backend";

export type ActionState = { error?: string; success?: boolean };
const ok: ActionState = { success: true };
function fail(e: unknown): ActionState {
  return { error: e instanceof BackendError ? e.message : "خطای غیرمنتظره" };
}

export async function shipOrderAction(orderId: string): Promise<ActionState> {
  try {
    await backendFetch(`/api/v1/shop/admin/orders/${orderId}/ship`, { method: "POST" });
    revalidatePath("/orders");
    return ok;
  } catch (e) {
    return fail(e);
  }
}

export async function deliverOrderAction(orderId: string): Promise<ActionState> {
  try {
    await backendFetch(`/api/v1/shop/admin/orders/${orderId}/deliver`, { method: "POST" });
    revalidatePath("/orders");
    return ok;
  } catch (e) {
    return fail(e);
  }
}

export async function reviewReturnAction(
  returnId: string,
  action: "approve" | "reject" | "complete",
  _prev: ActionState,
  formData: FormData,
): Promise<ActionState> {
  try {
    await backendFetch(`/api/v1/shop/admin/returns/${returnId}/${action}`, {
      method: "POST",
      body: JSON.stringify({ adminNote: String(formData.get("adminNote") ?? "") || null }),
    });
    revalidatePath("/orders");
    return ok;
  } catch (e) {
    return fail(e);
  }
}
