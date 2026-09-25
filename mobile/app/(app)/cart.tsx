import { View, FlatList, ActivityIndicator } from "react-native";
import { Stack, router } from "expo-router";
import { Screen, Card } from "@/components/Screen";
import { ThemedText } from "@/components/ThemedText";
import { Button } from "@/components/Button";
import { ErrorState } from "@/components/ErrorState";
import { spacing, palette } from "@/theme";
import { useTheme } from "@/hooks/useTheme";
import { useCart, useRemoveCartItem, useUpdateCartItem } from "@/features/shop/api";
import { describeError } from "@/lib/api";
import { formatRial, toPersianDigits } from "@/lib/format";

export default function CartScreen() {
  const { colors } = useTheme();
  const { data, isLoading, isError, refetch } = useCart();
  const removeItem = useRemoveCartItem();
  const updateItem = useUpdateCartItem();

  const total = (data ?? []).reduce((sum, i) => sum + Number(i.lineSubtotal), 0);
  const busy = updateItem.isPending || removeItem.isPending;
  const mutationError = updateItem.isError
    ? describeError(updateItem.error, "تغییر تعداد ممکن نشد؛ ممکن است موجودی کالا کافی نباشد.")
    : removeItem.isError
      ? describeError(removeItem.error, "حذف کالا از سبد ممکن نشد.")
      : undefined;

  return (
    <Screen scroll={false}>
      <Stack.Screen options={{ headerShown: true, title: "سبد خرید" }} />
      <FlatList
        contentContainerStyle={{ padding: spacing.lg, gap: spacing.md }}
        data={data ?? []}
        keyExtractor={(i) => i.id}
        ListHeaderComponent={mutationError ? <ThemedText color={palette.danger}>{mutationError}</ThemedText> : null}
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
                {formatRial(item.unitPrice)} × {toPersianDigits(item.quantity)}
              </ThemedText>
            </View>
            <View style={{ flexDirection: "row", gap: spacing.sm, alignItems: "center" }}>
              {/* قبلاً «−» تعداد ۱ را به صفر (درخواست نامعتبر) می‌رساند؛ حذف فقط از دکمه‌ی «حذف».
                  در حین هر تغییر، همه‌ی دکمه‌ها غیرفعال‌اند تا لمس پشت‌سرهم دوبار ثبت نشود. */}
              <Button
                title="−"
                variant="secondary"
                disabled={busy || item.quantity <= 1}
                onPress={() => updateItem.mutate({ itemId: item.id, quantity: item.quantity - 1 })}
              />
              <ThemedText>{toPersianDigits(item.quantity)}</ThemedText>
              <Button title="+" variant="secondary" disabled={busy} onPress={() => updateItem.mutate({ itemId: item.id, quantity: item.quantity + 1 })} />
              <Button title="حذف" variant="ghost" disabled={busy} onPress={() => removeItem.mutate(item.id)} />
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
