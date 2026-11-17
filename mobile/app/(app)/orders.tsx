import { View, FlatList, ActivityIndicator } from "react-native";
import { Stack } from "expo-router";
import { Screen, Card } from "@/components/Screen";
import { ThemedText } from "@/components/ThemedText";
import { Pill } from "@/components/Pill";
import { ErrorState } from "@/components/ErrorState";
import { spacing } from "@/theme";
import { useTheme } from "@/hooks/useTheme";
import { useMyOrders, ORDER_STATUS } from "@/features/shop/api";
import { formatDate, formatRial, toPersianDigits } from "@/lib/format";

export default function OrdersScreen() {
  const { colors } = useTheme();
  const { data, isLoading, isError, refetch } = useMyOrders();

  return (
    <Screen scroll={false}>
      <Stack.Screen options={{ headerShown: true, title: "سفارش‌های من" }} />
      <FlatList
        contentContainerStyle={{ padding: spacing.lg, gap: spacing.md }}
        data={data ?? []}
        keyExtractor={(o) => o.id}
        ListEmptyComponent={
          isLoading ? (
            <ActivityIndicator color={colors.accent} />
          ) : isError ? (
            <ErrorState onRetry={refetch} />
          ) : (
            <ThemedText muted>سفارشی ثبت نشده</ThemedText>
          )
        }
        renderItem={({ item }) => (
          <Card>
            <View style={{ flexDirection: "row", justifyContent: "space-between" }}>
              <ThemedText variant="caption" muted>
                {formatDate(item.createdAt)}
              </ThemedText>
              <Pill label={ORDER_STATUS[item.status]?.label ?? item.status} tone={ORDER_STATUS[item.status]?.tone ?? "info"} />
            </View>
            {item.items.map((i, idx) => (
              <ThemedText key={idx} variant="caption">
                {i.productName} × {toPersianDigits(i.quantity)}
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
