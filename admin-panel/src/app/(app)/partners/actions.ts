"use server";

import { revalidatePath } from "next/cache";
import { backendFetch, BackendError } from "@/lib/backend";

export type PartnerCreatedResponse = {
  id: string;
  name: string;
  slug: string;
  apiKey: string;
};

export type CreatePartnerState = {
  error?: string;
  created?: PartnerCreatedResponse;
};

export async function createPartnerAction(
  _prev: CreatePartnerState,
  formData: FormData,
): Promise<CreatePartnerState> {
  const name = String(formData.get("name") ?? "").trim();
  const slug = String(formData.get("slug") ?? "").trim();
  const contactEmail = String(formData.get("contactEmail") ?? "").trim() || undefined;
  const contactPhone = String(formData.get("contactPhone") ?? "").trim() || undefined;

  if (!name || !slug) {
    return { error: "نام و Slug اجباری هستند" };
  }

  try {
    const created = await backendFetch<PartnerCreatedResponse>("/api/v1/partners/admin", {
      method: "POST",
      body: JSON.stringify({ name, slug, contactEmail, contactPhone }),
    });
    revalidatePath("/partners");
    return { created };
  } catch (e) {
    const message = e instanceof BackendError ? e.message : "خطای غیرمنتظره در ساخت Partner";
    return { error: message };
  }
}
