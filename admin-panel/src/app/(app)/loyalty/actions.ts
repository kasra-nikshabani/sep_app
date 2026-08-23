"use server";

import { revalidatePath } from "next/cache";
import { backendFetch, BackendError } from "@/lib/backend";

export type ActionState = { error?: string; success?: boolean };
const ok: ActionState = { success: true };

function fail(e: unknown): ActionState {
  return { error: e instanceof BackendError ? e.message : "خطای غیرمنتظره" };
}

export async function createLevelAction(_prev: ActionState, formData: FormData): Promise<ActionState> {
  try {
    await backendFetch("/api/v1/loyalty/admin/levels", {
      method: "POST",
      body: JSON.stringify({
        name: String(formData.get("name") ?? ""),
        minPoints: Number(formData.get("minPoints") ?? 0),
        benefits: String(formData.get("benefits") ?? ""),
      }),
    });
    revalidatePath("/loyalty");
    return ok;
  } catch (e) {
    return fail(e);
  }
}

export async function createEarningRuleAction(_prev: ActionState, formData: FormData): Promise<ActionState> {
  try {
    await backendFetch("/api/v1/loyalty/admin/earning-rules", {
      method: "POST",
      body: JSON.stringify({
        sourceType: String(formData.get("sourceType") ?? ""),
        pointsPerAmount: String(formData.get("pointsPerAmount") ?? "0"),
      }),
    });
    revalidatePath("/loyalty");
    return ok;
  } catch (e) {
    return fail(e);
  }
}

export async function createRewardAction(_prev: ActionState, formData: FormData): Promise<ActionState> {
  try {
    const stock = String(formData.get("stockQuantity") ?? "").trim();
    await backendFetch("/api/v1/loyalty/admin/rewards", {
      method: "POST",
      body: JSON.stringify({
        name: String(formData.get("name") ?? ""),
        description: String(formData.get("description") ?? ""),
        pointsCost: Number(formData.get("pointsCost") ?? 0),
        stockQuantity: stock ? Number(stock) : null,
      }),
    });
    revalidatePath("/loyalty");
    return ok;
  } catch (e) {
    return fail(e);
  }
}

export async function fulfillRedemptionAction(redemptionId: string): Promise<ActionState> {
  try {
    await backendFetch(`/api/v1/loyalty/admin/redemptions/${redemptionId}/fulfill`, { method: "POST" });
    revalidatePath("/loyalty");
    return ok;
  } catch (e) {
    return fail(e);
  }
}

export async function cancelRedemptionAction(redemptionId: string): Promise<ActionState> {
  try {
    await backendFetch(`/api/v1/loyalty/admin/redemptions/${redemptionId}/cancel`, { method: "POST" });
    revalidatePath("/loyalty");
    return ok;
  } catch (e) {
    return fail(e);
  }
}
