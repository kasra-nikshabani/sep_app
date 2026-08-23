"use server";

import { revalidatePath } from "next/cache";
import { backendFetch, BackendError } from "@/lib/backend";

export type ActionState = { error?: string; success?: boolean };
const ok: ActionState = { success: true };
function fail(e: unknown): ActionState {
  return { error: e instanceof BackendError ? e.message : "خطای غیرمنتظره" };
}

function parseAttributes(raw: string): Record<string, string> {
  const result: Record<string, string> = {};
  raw
    .split(",")
    .map((p) => p.trim())
    .filter(Boolean)
    .forEach((pair) => {
      const [k, v] = pair.split(":").map((s) => s.trim());
      if (k) result[k] = v ?? "";
    });
  return result;
}

export async function createCategoryAction(_prev: ActionState, formData: FormData): Promise<ActionState> {
  try {
    const parentId = String(formData.get("parentId") ?? "").trim();
    await backendFetch("/api/v1/shop/admin/categories", {
      method: "POST",
      body: JSON.stringify({
        name: String(formData.get("name") ?? ""),
        slug: String(formData.get("slug") ?? ""),
        parentId: parentId || null,
      }),
    });
    revalidatePath("/shop");
    return ok;
  } catch (e) {
    return fail(e);
  }
}

export async function createProductAction(_prev: ActionState, formData: FormData): Promise<ActionState> {
  try {
    await backendFetch("/api/v1/shop/admin/products", {
      method: "POST",
      body: JSON.stringify({
        categoryId: String(formData.get("categoryId") ?? ""),
        name: String(formData.get("name") ?? ""),
        slug: String(formData.get("slug") ?? ""),
        description: String(formData.get("description") ?? "") || null,
        imageUrl: String(formData.get("imageUrl") ?? "") || null,
        basePrice: String(formData.get("basePrice") ?? "0"),
      }),
    });
    revalidatePath("/shop");
    return ok;
  } catch (e) {
    return fail(e);
  }
}

export async function addVariantAction(productId: string, _prev: ActionState, formData: FormData): Promise<ActionState> {
  try {
    const priceOverride = String(formData.get("priceOverride") ?? "").trim();
    await backendFetch(`/api/v1/shop/admin/products/${productId}/variants`, {
      method: "POST",
      body: JSON.stringify({
        sku: String(formData.get("sku") ?? ""),
        attributes: parseAttributes(String(formData.get("attributes") ?? "")),
        initialStock: Number(formData.get("initialStock") ?? 0),
        weightGrams: Number(formData.get("weightGrams") ?? 500),
        priceOverride: priceOverride || null,
      }),
    });
    revalidatePath("/shop");
    return ok;
  } catch (e) {
    return fail(e);
  }
}

export async function adjustStockAction(variantId: string, delta: number): Promise<ActionState> {
  try {
    await backendFetch(`/api/v1/shop/admin/variants/${variantId}/stock`, {
      method: "POST",
      body: JSON.stringify({ delta }),
    });
    revalidatePath("/shop");
    return ok;
  } catch (e) {
    return fail(e);
  }
}

export async function createShippingMethodAction(_prev: ActionState, formData: FormData): Promise<ActionState> {
  try {
    await backendFetch("/api/v1/shop/admin/shipping-methods", {
      method: "POST",
      body: JSON.stringify({
        name: String(formData.get("name") ?? ""),
        baseRate: String(formData.get("baseRate") ?? "0"),
        perKgRate: String(formData.get("perKgRate") ?? "0"),
      }),
    });
    revalidatePath("/shop");
    return ok;
  } catch (e) {
    return fail(e);
  }
}

export async function createCouponAction(_prev: ActionState, formData: FormData): Promise<ActionState> {
  try {
    await backendFetch("/api/v1/shop/admin/coupons", {
      method: "POST",
      body: JSON.stringify({
        code: String(formData.get("code") ?? ""),
        discountType: String(formData.get("discountType") ?? ""),
        discountValue: String(formData.get("discountValue") ?? "0"),
        minOrderAmount: String(formData.get("minOrderAmount") ?? "").trim() || null,
        maxUsesTotal: String(formData.get("maxUsesTotal") ?? "").trim() || null,
        maxUsesPerUser: String(formData.get("maxUsesPerUser") ?? "").trim() || null,
      }),
    });
    revalidatePath("/shop");
    return ok;
  } catch (e) {
    return fail(e);
  }
}
