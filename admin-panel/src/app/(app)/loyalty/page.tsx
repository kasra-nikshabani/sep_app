import { backendFetch } from "@/lib/backend";
import { LoyaltyTabs, type Level, type EarningRule, type Reward, type Redemption } from "./LoyaltyTabs";

export default async function LoyaltyPage() {
  const [levels, rules, rewards, redemptions] = await Promise.all([
    backendFetch<Level[]>("/api/v1/loyalty/admin/levels"),
    backendFetch<EarningRule[]>("/api/v1/loyalty/admin/earning-rules"),
    backendFetch<Reward[]>("/api/v1/loyalty/rewards"),
    backendFetch<Redemption[]>("/api/v1/loyalty/admin/redemptions"),
  ]);

  return (
    <>
      <h2 style={{ marginBottom: 16 }}>باشگاه امتیاز</h2>
      <LoyaltyTabs levels={levels} rules={rules} rewards={rewards} redemptions={redemptions} />
    </>
  );
}
