import { useQuery } from "@tanstack/react-query";
import { useApi } from "@/lib/api";

export type MeResponse = {
  id: string;
  nationalCode: string;
  phoneNumber: string;
  displayName: string | null;
  status: "active" | "suspended";
  membershipNumber: string | null;
  city: string | null;
  joinedAt: string | null;
};

export function useMe() {
  const { apiFetch } = useApi();
  return useQuery({
    queryKey: ["me"],
    queryFn: () => apiFetch<MeResponse>("/api/v1/users/me"),
  });
}
