import { useMutation, useQuery, useQueryClient } from "@tanstack/react-query";
import { useApi } from "@/lib/api";

export type Account = { id: string; pointsBalance: number; lifetimePoints: number; levelName: string | null };
export type Transaction = {
  id: string;
  type: "earn" | "redeem" | "adjust";
  points: number;
  sourceType: string | null;
  description: string | null;
  createdAt: string;
};
export type Reward = {
  id: string;
  name: string;
  description: string | null;
  pointsCost: number;
  stockQuantity: number | null;
  active: boolean;
};
export type Redemption = {
  id: string;
  rewardName: string;
  pointsSpent: number;
  redemptionCode: string;
  status: "requested" | "fulfilled" | "cancelled";
  createdAt: string;
  fulfilledAt: string | null;
};

export function useLoyaltyAccount() {
  const { apiFetch } = useApi();
  return useQuery({ queryKey: ["loyalty", "account"], queryFn: () => apiFetch<Account>("/api/v1/loyalty/account") });
}

export function useLoyaltyTransactions() {
  const { apiFetch } = useApi();
  return useQuery({
    queryKey: ["loyalty", "transactions"],
    queryFn: () => apiFetch<Transaction[]>("/api/v1/loyalty/transactions"),
  });
}

export function useRewards() {
  const { apiFetch } = useApi();
  return useQuery({ queryKey: ["loyalty", "rewards"], queryFn: () => apiFetch<Reward[]>("/api/v1/loyalty/rewards") });
}

export function useRedeemReward() {
  const { apiFetch } = useApi();
  const qc = useQueryClient();
  return useMutation({
    mutationFn: (rewardId: string) =>
      apiFetch<Redemption>(`/api/v1/loyalty/rewards/${rewardId}/redeem`, { method: "POST" }),
    onSuccess: () => {
      qc.invalidateQueries({ queryKey: ["loyalty"] });
    },
  });
}

export function useMyRedemptions() {
  const { apiFetch } = useApi();
  return useQuery({
    queryKey: ["loyalty", "redemptions"],
    queryFn: () => apiFetch<Redemption[]>("/api/v1/loyalty/redemptions"),
  });
}
