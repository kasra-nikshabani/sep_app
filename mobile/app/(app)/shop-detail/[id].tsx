import { useState } from "react";
import { View, ActivityIndicator, Image, ScrollView, Pressable } from "react-native";
import { useLocalSearchParams, Stack, router } from "expo-router";
import { Screen } from "@/components/Screen";
import { ThemedText } from "@/components/ThemedText";
import { Button } from "@/components/Button";
import { Pill } from "@/components/Pill";
import { ErrorState } from "@/components/ErrorState";
import { spacing, palette, statusMeta } from "@/theme";
import { useTheme } from "@/hooks/useTheme";
import { useProduct, useAddToCart } from "@/features/shop/api";
import { describeError } from "@/lib/api";
import { formatRial, toPersianDigits } from "@/lib/format";

export default function ProductDetailScreen() {
  const { id } = useLocalSearchParams<{ id: string }>();
  const { colors } = useTheme();
  const { data: product, isLoading, isError, refetch } = useProduct(id);
  const addToCart = useAddToCart();
  const [selectedVariantId, setSelectedVariantId] = useState<string | null>(null);

  if (isLoading || !product) {
    return (
      <Screen>
        {isError ? <ErrorState onRetry={refetch} /> : <ActivityIndicator color={colors.accent} />}
      </Screen>
    );
  }

  const variant = product.variants.find((v) => v.id === selectedVariantId) ?? product.variants[0];

  return (
    <Screen>
      <Stack.Screen options={{ headerShown: true, title: product.name }} />
      {product.imageUrl ? <Image source={{ uri: product.imageUrl }} style={{ width: "100%", height: 220, borderRadius: 14 }} /> : null}
      <ThemedText variant="h1">{product.name}</ThemedText>
      {product.description ? <ThemedText muted>{product.description}</ThemedText> : null}

      <ScrollView horizontal showsHorizontalScrollIndicator={false}>
        <View style={{ flexDirection: "row", gap: spacing.sm }}>
          {product.variants.map((v) => {
            const label = Object.values(v.attributes ?? {}).join(" / ") || v.sku;
            const selected = v.id === variant?.id;
            const outOfStock = v.stockQuantity <= 0;
            return (
              // قبلاً فقط Pill نمایشی بود (setSelectedVariantId هیچ‌جا صدا زده نمی‌شد) --
              // همیشه اولین Variant به سبد اضافه می‌شد، مستقل از انتخاب کاربر.
              <Pressable
                key={v.id}
                onPress={() => {
                  setSelectedVariantId(v.id);
                  addToCart.reset();
                }}
                accessibilityRole="radio"
                accessibilityState={{ checked: selected }}
                accessibilityLabel={outOfStock ? `${label}، ناموجود` : label}
                hitSlop={6}
                style={{ opacity: outOfStock && !selected ? 0.45 : 1 }}
              >
                <Pill tone={selected ? "gold" : "info"} label={label} />
              </Pressable>
            );
          })}
        </View>
      </ScrollView>

      {variant && (
        <>
          <ThemedText variant="numeric" color={colors.goldText}>
            {formatRial(variant.price)}
          </ThemedText>
          <ThemedText variant="caption" muted>
            {variant.stockQuantity > 0 ? `موجودی: ${toPersianDigits(variant.stockQuantity)}` : "ناموجود"}
          </ThemedText>
          <Button
            title={addToCart.isPending ? "در حال افزودن..." : "افزودن به سبد خرید"}
            variant="gold"
            disabled={variant.stockQuantity <= 0}
            loading={addToCart.isPending}
            onPress={() => addToCart.mutate({ productVariantId: variant.id, quantity: 1 })}
          />
          {/* قبلاً بعد از افزودن، کاربر اجباراً به سبد خرید پرتاب می‌شد و خطا هم هیچ‌جا نمایش
              داده نمی‌شد -- حالا در همین صفحه می‌ماند و خودش تصمیم می‌گیرد. */}
          {addToCart.isSuccess && (
            <View style={{ gap: spacing.sm, padding: spacing.md, borderRadius: 10, backgroundColor: statusMeta.success.bg }}>
              <ThemedText>به سبد خرید اضافه شد.</ThemedText>
              <Button title="مشاهده‌ی سبد خرید" variant="secondary" onPress={() => router.push("/cart")} />
            </View>
          )}
          {addToCart.isError && (
            <ThemedText color={palette.danger}>
              {describeError(addToCart.error, "افزودن به سبد ممکن نشد؛ ممکن است موجودی این کالا تمام شده باشد.")}
            </ThemedText>
          )}
        </>
      )}
    </Screen>
  );
}
