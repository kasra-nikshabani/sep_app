import { View, FlatList, ActivityIndicator } from "react-native";
import { Stack, router } from "expo-router";
import { Screen, Card } from "@/components/Screen";
import { ThemedText } from "@/components/ThemedText";
import { Button } from "@/components/Button";
import { ErrorState } from "@/components/ErrorState";
import { spacing } from "@/theme";
import { useTheme } from "@/hooks/useTheme";
import { useCart, useRemoveCartItem, useUpdateCartItem } from "@/features/shop/api";
import { formatRial } from "@/lib/format";

export default function CartScreen() {
  const { colors } = useTheme();
  const { data, isLoading, isError, refetch } = useCart();
  const removeItem = useRemoveCartItem();
  const updateItem = useUpdateCartItem();

  const total = (data ?? []).reduce((sum, i) => sum + Number(i.lineSubtotal), 0);

  return (
    <Screen scroll={false}>
      <Stack.Screen options={{ headerShown: true, title: "سبد خرید" }} />
      <FlatList
        contentContainerStyle={{ padding: spacing.lg, gap: spacing.md }}
        data={data ?? []}
        keyExtractor={(i) => i.id}
        ListEmptyComponent={
          isLoading ? (
            <ActivityIndicator color={colors.accent} />
          ) : isError ? (
            <ErrorState onRetry={refetch} />
          ) : (
            <ThemedText muted>سبد خرید خالی است</ThemedText>
          )
        }
        renderItem={({ item }) => (
          <Card style={{ flexDirection: "row", alignItems: "center", justifyContent: "space-between" }}>
            <View style={{ flex: 1 }}>
              <ThemedText style={{ fontFamily: "Vazirmatn-Medium" }}>{item.productName}</ThemedText>
              <ThemedText variant="caption" muted>
                {formatRial(item.unitPrice)} × {item.quantity}
              </ThemedText>
            </View>
            <View style={{ flexDirection: "row", gap: spacing.sm, alignItems: "center" }}>
              <Button title="−" variant="secondary" onPress={() => updateItem.mutate({ itemId: item.id, quantity: Math.max(0, item.quantity - 1) })} />
              <ThemedText>{item.quantity}</ThemedText>
              <Button title="+" variant="secondary" onPress={() => updateItem.mutate({ itemId: item.id, quantity: item.quantity + 1 })} />
              <Button title="حذف" variant="ghost" onPress={() => removeItem.mutate(item.id)} />
            </View>
          </Card>
        )}
        ListFooterComponent={
          data && data.length > 0 ? (
            <View style={{ gap: spacing.md, marginTop: spacing.md }}>
              <ThemedText variant="h2">جمع: {formatRial(total)}</ThemedText>
              <Button title="ادامه‌ی خرید" variant="gold" onPress={() => router.push("/checkout")} />
            </View>
          ) : null
        }
      />
    </Screen>
  );
}
