import { useState } from "react";
import { View, FlatList, ActivityIndicator } from "react-native";
import { Stack } from "expo-router";
import { Screen, Card } from "@/components/Screen";
import { ThemedText } from "@/components/ThemedText";
import { Button } from "@/components/Button";
import { Pill } from "@/components/Pill";
import { ErrorState } from "@/components/ErrorState";
import { ConfirmDialog } from "@/components/ConfirmDialog";
import { spacing, statusMeta } from "@/theme";
import { useTheme } from "@/hooks/useTheme";
import {
  useLoyaltyAccount,
  useLoyaltyTransactions,
  useRewards,
  useRedeemReward,
  useMyRedemptions,
  type Redemption,
  type Reward,
} from "@/features/loyalty/api";
import { describeError } from "@/lib/api";
import { formatDate, formatNumber } from "@/lib/format";

const redemptionStatusMeta = {
  requested: { label: "درخواست‌شده", tone: "warning" as const },
  fulfilled: { label: "انجام‌شده", tone: "success" as const },
  cancelled: { label: "لغوشده", tone: "danger" as const },
};

export default function LoyaltyScreen() {
  const { colors } = useTheme();
  const accountQuery = useLoyaltyAccount();
  const transactionsQuery = useLoyaltyTransactions();
  const rewardsQuery = useRewards();
  const redemptionsQuery = useMyRedemptions();
  const { data: account } = accountQuery;
  const { data: transactions } = transactionsQuery;
  const { data: rewards, isLoading: rewardsLoading } = rewardsQuery;
  const { data: redemptions } = redemptionsQuery;
  const redeem = useRedeemReward();
  // قبلاً «دریافت» با یک لمس و بدون تأیید امتیاز را خرج می‌کرد و هیچ بازخوردی (حتی کد پیگیری
  // که Backend برمی‌گرداند) نشان داده نمی‌شد.
  const [pendingReward, setPendingReward] = useState<Reward | null>(null);
  const [redeemError, setRedeemError] = useState<string>();
  const [lastRedemption, setLastRedemption] = useState<Redemption | null>(null);

  // هر چهار بخش این صفحه به هم وابسته‌اند (موجودی ↔ جوایز قابل دریافت) -- با شکست هر کدام،
  // یک خطای واحد به‌جای نمایش «ثبت نشده»های گمراه‌کننده در بقیه‌ی بخش‌ها.
  const failedQueries = [accountQuery, transactionsQuery, rewardsQuery, redemptionsQuery].filter((q) => q.isError && q.data === undefined);
  if (failedQueries.length > 0) {
    return (
      <Screen>
        <Stack.Screen options={{ headerShown: true, title: "باشگاه امتیاز" }} />
        <ErrorState onRetry={() => failedQueries.forEach((q) => q.refetch())} />
      </Screen>
    );
  }

  const balance = account?.pointsBalance ?? 0;

  function confirmRedeem() {
    if (!pendingReward) return;
    setRedeemError(undefined);
    redeem.mutate(pendingReward.id, {
      onSuccess: (redemption) => {
        setLastRedemption(redemption);
        setPendingReward(null);
      },
      onError: (e) =>
        setRedeemError(describeError(e, "دریافت این جایزه ممکن نشد؛ ممکن است موجودی جایزه تمام شده یا امتیاز شما کافی نباشد.")),
    });
  }

  return (
    <Screen>
      <Stack.Screen options={{ headerShown: true, title: "باشگاه امتیاز" }} />

      <Card style={{ backgroundColor: colors.accent, alignItems: "center" }}>
        <ThemedText variant="display" color={colors.gold}>
          {formatNumber(account?.pointsBalance)}
        </ThemedText>
        <ThemedText color={colors.accentContrast}>سطح: {account?.levelName ?? "—"}</ThemedText>
        <ThemedText variant="caption" color={colors.accentContrast}>
          مجموع امتیاز کسب‌شده: {formatNumber(account?.lifetimePoints)}
        </ThemedText>
      </Card>

      {lastRedemption && (
        <Card style={{ backgroundColor: statusMeta.success.bg, borderColor: statusMeta.success.bg }}>
          <ThemedText style={{ fontFamily: "Vazirmatn-Medium" }}>
            درخواست «{lastRedemption.rewardName}» ثبت شد.
          </ThemedText>
          <ThemedText variant="caption">کد پیگیری: {lastRedemption.redemptionCode}</ThemedText>
        </Card>
      )}

      <ThemedText variant="h2">جوایز قابل دریافت</ThemedText>
      <FlatList
        data={rewards ?? []}
        scrollEnabled={false}
        keyExtractor={(r) => r.id}
        ItemSeparatorComponent={() => <View style={{ height: spacing.sm }} />}
        ListEmptyComponent={rewardsLoading ? <ActivityIndicator color={colors.accent} /> : <ThemedText muted>جایزه‌ای موجود نیست</ThemedText>}
        renderItem={({ item }) => {
          const outOfStock = item.stockQuantity === 0;
          const shortfall = item.pointsCost - balance;
          return (
            <Card style={{ flexDirection: "row", alignItems: "center", justifyContent: "space-between" }}>
              <View style={{ flex: 1 }}>
                <ThemedText style={{ fontFamily: "Vazirmatn-Medium" }}>{item.name}</ThemedText>
                <ThemedText variant="caption" muted>
                  {formatNumber(item.pointsCost)} امتیاز
                </ThemedText>
                {outOfStock ? (
                  <ThemedText variant="caption" muted>
                    ناموجود
                  </ThemedText>
                ) : shortfall > 0 ? (
                  <ThemedText variant="caption" muted>
                    {formatNumber(shortfall)} امتیاز دیگر لازم است
                  </ThemedText>
                ) : null}
              </View>
              <Button
                title="دریافت"
                variant="gold"
                disabled={outOfStock || shortfall > 0 || redeem.isPending}
                onPress={() => {
                  setRedeemError(undefined);
                  setPendingReward(item);
                }}
              />
            </Card>
          );
        }}
      />

      <ThemedText variant="h2">درخواست‌های من</ThemedText>
      <FlatList
        data={redemptions ?? []}
        scrollEnabled={false}
        keyExtractor={(r) => r.id}
        ItemSeparatorComponent={() => <View style={{ height: spacing.sm }} />}
        ListEmptyComponent={<ThemedText muted>درخواستی ثبت نشده</ThemedText>}
        renderItem={({ item }) => (
          <Card style={{ flexDirection: "row", alignItems: "center", justifyContent: "space-between" }}>
            <View style={{ flex: 1 }}>
              <ThemedText>{item.rewardName}</ThemedText>
              <ThemedText variant="caption" muted>
                کد پیگیری: {item.redemptionCode}
              </ThemedText>
            </View>
            <Pill label={redemptionStatusMeta[item.status].label} tone={redemptionStatusMeta[item.status].tone} />
          </Card>
        )}
      />

      <ThemedText variant="h2">تراکنش‌ها</ThemedText>
      <FlatList
        data={transactions ?? []}
        scrollEnabled={false}
        keyExtractor={(t) => t.id}
        ItemSeparatorComponent={() => <View style={{ height: spacing.sm }} />}
        ListEmptyComponent={<ThemedText muted>تراکنشی ثبت نشده</ThemedText>}
        renderItem={({ item }) => (
          <Card style={{ flexDirection: "row", justifyContent: "space-between" }}>
            <View>
              <ThemedText variant="caption">{item.description ?? item.sourceType ?? item.type}</ThemedText>
              <ThemedText variant="caption" muted>
                {formatDate(item.createdAt)}
              </ThemedText>
            </View>
            {/* gold500 روی سفید فقط ۲.۱:۱ بود -- goldText برای متن روی زمینه‌ی روشن */}
            <ThemedText color={item.points >= 0 ? colors.goldText : colors.textMuted}>
              {item.points >= 0 ? "+" : "−"}
              {formatNumber(Math.abs(item.points))}
            </ThemedText>
          </Card>
        )}
      />

      <ConfirmDialog
        visible={!!pendingReward}
        title="دریافت جایزه"
        message={
          pendingReward
            ? `«${pendingReward.name}» در ازای ${formatNumber(pendingReward.pointsCost)} امتیاز. این امتیاز از موجودی شما کسر می‌شود.`
            : ""
        }
        confirmTitle="تأیید و دریافت"
        loading={redeem.isPending}
        error={redeemError}
        onConfirm={confirmRedeem}
        onCancel={() => setPendingReward(null)}
      />
    </Screen>
  );
}
