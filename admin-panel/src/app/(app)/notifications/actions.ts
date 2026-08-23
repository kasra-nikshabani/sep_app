"use server";

import { revalidatePath } from "next/cache";
import { backendFetch, BackendError } from "@/lib/backend";

export type SendState = { error?: string; success?: boolean };

async function send(path: string, body: Record<string, string>): Promise<SendState> {
  try {
    await backendFetch(path, { method: "POST", body: JSON.stringify(body) });
    revalidatePath("/notifications");
    return { success: true };
  } catch (e) {
    return { error: e instanceof BackendError ? e.message : "خطای غیرمنتظره" };
  }
}

export async function sendSmsAction(_prev: SendState, formData: FormData): Promise<SendState> {
  return send("/api/v1/notifications/admin/sms", {
    mobile: String(formData.get("mobile") ?? ""),
    message: String(formData.get("message") ?? ""),
  });
}

export async function sendEmailAction(_prev: SendState, formData: FormData): Promise<SendState> {
  return send("/api/v1/notifications/admin/email", {
    to: String(formData.get("to") ?? ""),
    subject: String(formData.get("subject") ?? ""),
    body: String(formData.get("body") ?? ""),
  });
}

export async function sendPushAction(_prev: SendState, formData: FormData): Promise<SendState> {
  return send("/api/v1/notifications/admin/push", {
    deviceToken: String(formData.get("deviceToken") ?? ""),
    title: String(formData.get("title") ?? ""),
    body: String(formData.get("body") ?? ""),
  });
}
