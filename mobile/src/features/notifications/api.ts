import { useMutation } from "@tanstack/react-query";
import { useApi } from "@/lib/api";

export function useRegisterDeviceToken() {
  const { apiFetch } = useApi();
  return useMutation({
    mutationFn: (body: { token: string; platform: "ios" | "android" | "web" }) =>
      apiFetch<void>("/api/v1/notifications/device-tokens", { method: "POST", body: JSON.stringify(body) }),
  });
}

export function useUnregisterDeviceToken() {
  const { apiFetch } = useApi();
  return useMutation({
    mutationFn: (token: string) => apiFetch<void>(`/api/v1/notifications/device-tokens/${token}`, { method: "DELETE" }),
  });
}
