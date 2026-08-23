import { auth } from "@/auth";

export class BackendError extends Error {
  constructor(
    public status: number,
    message: string,
  ) {
    super(message);
  }
}

/**
 * فراخوانی مستقیم Backend از Server Component/Server Action -- طبق ADR-0003،
 * مرورگر هرگز مستقیم به Spring Boot وصل نمی‌شود (بدون CORS روی آن عمداً).
 * برای فراخوانی از Client Component به‌جای این، از src/app/api/backend/[...path] استفاده شود.
 */
export async function backendFetch<T>(path: string, init: RequestInit = {}): Promise<T> {
  const session = await auth();
  if (!session?.accessToken) {
    throw new BackendError(401, "Session معتبر یا accessToken موجود نیست");
  }

  const response = await fetch(`${process.env.BACKEND_API_BASE_URL}${path}`, {
    ...init,
    headers: {
      "Content-Type": "application/json",
      ...init.headers,
      Authorization: `Bearer ${session.accessToken}`,
    },
    cache: "no-store",
  });

  if (!response.ok) {
    const text = await response.text().catch(() => "");
    throw new BackendError(response.status, text || response.statusText);
  }
  if (response.status === 204) {
    return undefined as T;
  }
  return response.json() as Promise<T>;
}

/** برای Upload چندبخشی (مثل رسانه‌ی خبر) -- بدون Content-Type دستی، چون Boundary باید خودکار تنظیم شود. */
export async function backendUpload<T>(path: string, formData: FormData): Promise<T> {
  const session = await auth();
  if (!session?.accessToken) {
    throw new BackendError(401, "Session معتبر یا accessToken موجود نیست");
  }

  const response = await fetch(`${process.env.BACKEND_API_BASE_URL}${path}`, {
    method: "POST",
    headers: { Authorization: `Bearer ${session.accessToken}` },
    body: formData,
    cache: "no-store",
  });

  if (!response.ok) {
    const text = await response.text().catch(() => "");
    throw new BackendError(response.status, text || response.statusText);
  }
  return response.json() as Promise<T>;
}
