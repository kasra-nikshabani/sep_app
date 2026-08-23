import { View, FlatList, ActivityIndicator } from "react-native";
import { Stack } from "expo-router";
import { Screen, Card } from "@/components/Screen";
import { ThemedText } from "@/components/ThemedText";
import { Button } from "@/components/Button";
import { Pill } from "@/components/Pill";
import { spacing } from "@/theme";
import { useTheme } from "@/hooks/useTheme";
import { useLoyaltyAccount, useLoyaltyTransactions, useRewards, useRedeemReward, useMyRedemptions } from "@/features/loyalty/api";
import { formatDate } from "@/lib/format";

const redemptionStatusMeta = {
  requested: { label: "درخواست‌شده", tone: "warning" as const },
  fulfilled: { label: "انجام‌شده", tone: "success" as const },
  cancelled: { label: "لغوشده", tone: "danger" as const },
};

export default function LoyaltyScreen() {
  const { colors } = useTheme();
  const { data: account } = useLoyaltyAccount();
  const { data: transactions } = useLoyaltyTransactions();
  const { data: rewards, isLoading: rewardsLoading } = useRewards();
  const { data: redemptions } = useMyRedemptions();
  const redeem = useRedeemReward();

  return (
    <Screen>
      <Stack.Screen options={{ headerShown: true, title: "باشگاه امتیاز" }} />

      <Card style={{ backgroundColor: colors.accent, alignItems: "center" }}>
        <ThemedText variant="display" color={colors.gold}>
          {account?.pointsBalance ?? "—"}
        </ThemedText>
        <ThemedText color={colors.accentContrast}>سطح: {account?.levelName ?? "—"}</ThemedText>
        <ThemedText variant="caption" color={colors.accentContrast}>
          مجموع امتیاز کسب‌شده: {account?.lifetimePoints ?? "—"}
        </ThemedText>
      </Card>

      <ThemedText variant="h2">جوایز قابل دریافت</ThemedText>
      <FlatList
        data={rewards ?? []}
        scrollEnabled={false}
        keyExtractor={(r) => r.id}
        ItemSeparatorComponent={() => <View style={{ height: spacing.sm }} />}
        ListEmptyComponent={rewardsLoading ? <ActivityIndicator color={colors.accent} /> : <ThemedText muted>جایزه‌ای موجود نیست</ThemedText>}
        renderItem={({ item }) => (
          <Card style={{ flexDirection: "row-reverse", alignItems: "center", justifyContent: "space-between" }}>
            <View style={{ flex: 1 }}>
              <ThemedText style={{ fontFamily: "Vazirmatn-Medium" }}>{item.name}</ThemedText>
              <ThemedText variant="caption" muted>
                {item.pointsCost} امتیاز
              </ThemedText>
            </View>
            <Button
              title="دریافت"
              variant="gold"
              disabled={(account?.pointsBalance ?? 0) < item.pointsCost}
              loading={redeem.isPending}
              onPress={() => redeem.mutate(item.id)}
            />
          </Card>
        )}
      />

      <ThemedText variant="h2">درخواست‌های من</ThemedText>
      <FlatList
        data={redemptions ?? []}
        scrollEnabled={false}
        keyExtractor={(r) => r.id}
        ItemSeparatorComponent={() => <View style={{ height: spacing.sm }} />}
        ListEmptyComponent={<ThemedText muted>درخواستی ثبت نشده</ThemedText>}
        renderItem={({ item }) => (
          <Card style={{ flexDirection: "row-reverse", justifyContent: "space-between" }}>
            <ThemedText>{item.rewardName}</ThemedText>
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
          <Card style={{ flexDirection: "row-reverse", justifyContent: "space-between" }}>
            <View>
              <ThemedText variant="caption">{item.description ?? item.sourceType ?? item.type}</ThemedText>
              <ThemedText variant="caption" muted>
                {formatDate(item.createdAt)}
              </ThemedText>
            </View>
            <ThemedText color={item.points >= 0 ? colors.gold : colors.textMuted}>
              {item.points >= 0 ? "+" : ""}
              {item.points}
            </ThemedText>
          </Card>
        )}
      />
    </Screen>
  );
}
