import { useCallback } from "react";
import { useAuth } from "./auth";

const API_BASE_URL = process.env.EXPO_PUBLIC_API_BASE_URL as string;

export class ApiError extends Error {
  constructor(
    public status: number,
    message: string,
  ) {
    super(message);
  }
}

export function useApi() {
  const { getValidAccessToken, logout } = useAuth();

  const apiFetch = useCallback(
    async <T,>(path: string, init: RequestInit = {}): Promise<T> => {
      const token = await getValidAccessToken();
      if (!token) {
        await logout();
        throw new ApiError(401, "نشست منقضی شده -- دوباره وارد شوید");
      }

      const response = await fetch(`${API_BASE_URL}${path}`, {
        ...init,
        headers: {
          "Content-Type": "application/json",
          ...init.headers,
          Authorization: `Bearer ${token}`,
        },
      });

      if (!response.ok) {
        const text = await response.text().catch(() => "");
        throw new ApiError(response.status, text || response.statusText);
      }
      if (response.status === 204) {
        return undefined as T;
      }
      return response.json() as Promise<T>;
    },
    [getValidAccessToken, logout],
  );

  return { apiFetch };
}

// متن خامِ ApiError همان بدنه‌ی پاسخ سرور است (JSON/Stack انگلیسی) -- هرگز مستقیم به کاربر
// نشان داده نمی‌شود. خطای 4xx یعنی درخواست در این وضعیت ممکن نیست (پیام وابسته به صفحه)؛
// بقیه (5xx یا قطع شبکه که fetch آن را TypeError پرتاب می‌کند) یعنی مشکل ارتباط.
export function describeError(error: unknown, rejectedMessage: string): string {
  if (error instanceof ApiError && error.status >= 400 && error.status < 500) {
    return rejectedMessage;
  }
  return "ارتباط با سرور برقرار نشد. لطفاً دوباره تلاش کنید.";
}
