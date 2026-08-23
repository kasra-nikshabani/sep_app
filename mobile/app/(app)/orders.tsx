import { View, FlatList, ActivityIndicator } from "react-native";
import { Stack } from "expo-router";
import { Screen, Card } from "@/components/Screen";
import { ThemedText } from "@/components/ThemedText";
import { Pill } from "@/components/Pill";
import { spacing } from "@/theme";
import { useTheme } from "@/hooks/useTheme";
import { useMyOrders } from "@/features/shop/api";
import { formatDate, formatRial } from "@/lib/format";

const statusMeta: Record<string, { label: string; tone: "success" | "warning" | "danger" | "info" }> = {
  pending_payment: { label: "در انتظار پرداخت", tone: "warning" },
  paid: { label: "پرداخت‌شده", tone: "info" },
  shipped: { label: "ارسال‌شده", tone: "info" },
  delivered: { label: "تحویل‌شده", tone: "success" },
  cancelled: { label: "لغوشده", tone: "danger" },
  expired: { label: "منقضی", tone: "danger" },
  return_requested: { label: "درخواست مرجوعی", tone: "warning" },
  returned: { label: "مرجوع‌شده", tone: "info" },
};

export default function OrdersScreen() {
  const { colors } = useTheme();
  const { data, isLoading } = useMyOrders();

  return (
    <Screen scroll={false}>
      <Stack.Screen options={{ headerShown: true, title: "سفارش‌های من" }} />
      <FlatList
        contentContainerStyle={{ padding: spacing.lg, gap: spacing.md }}
        data={data ?? []}
        keyExtractor={(o) => o.id}
        ListEmptyComponent={isLoading ? <ActivityIndicator color={colors.accent} /> : <ThemedText muted>سفارشی ثبت نشده</ThemedText>}
        renderItem={({ item }) => (
          <Card>
            <View style={{ flexDirection: "row-reverse", justifyContent: "space-between" }}>
              <ThemedText variant="caption" muted>
                {formatDate(item.createdAt)}
              </ThemedText>
              <Pill label={statusMeta[item.status]?.label ?? item.status} tone={statusMeta[item.status]?.tone ?? "info"} />
            </View>
            {item.items.map((i, idx) => (
              <ThemedText key={idx} variant="caption">
                {i.productName} × {i.quantity}
              </ThemedText>
            ))}
            <ThemedText variant="numeric" color={colors.goldText}>
              {formatRial(item.totalAmount)}
            </ThemedText>
          </Card>
        )}
      />
    </Screen>
  );
}
