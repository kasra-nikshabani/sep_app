import { useState } from "react";
import { View, ActivityIndicator, Image, ScrollView } from "react-native";
import { useLocalSearchParams, Stack, router } from "expo-router";
import { Screen } from "@/components/Screen";
import { ThemedText } from "@/components/ThemedText";
import { Button } from "@/components/Button";
import { Pill } from "@/components/Pill";
import { spacing } from "@/theme";
import { useTheme } from "@/hooks/useTheme";
import { useProduct, useAddToCart } from "@/features/shop/api";
import { formatRial } from "@/lib/format";

export default function ProductDetailScreen() {
  const { id } = useLocalSearchParams<{ id: string }>();
  const { colors } = useTheme();
  const { data: product, isLoading } = useProduct(id);
  const addToCart = useAddToCart();
  const [selectedVariantId, setSelectedVariantId] = useState<string | null>(null);

  if (isLoading || !product) {
    return (
      <Screen>
        <ActivityIndicator color={colors.accent} />
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

      <ScrollView horizontal showsHorizontalScrollIndicator={false} style={{ flexDirection: "row-reverse" }}>
        <View style={{ flexDirection: "row-reverse", gap: spacing.sm }}>
          {product.variants.map((v) => (
            <Pill
              key={v.id}
              tone={v.id === variant?.id ? "gold" : "info"}
              label={Object.values(v.attributes ?? {}).join(" / ") || v.sku}
            />
          ))}
        </View>
      </ScrollView>

      {variant && (
        <>
          <ThemedText variant="numeric" color={colors.goldText}>
            {formatRial(variant.price)}
          </ThemedText>
          <ThemedText variant="caption" muted>
            موجودی: {variant.stockQuantity}
          </ThemedText>
          <Button
            title={addToCart.isPending ? "در حال افزودن..." : "افزودن به سبد خرید"}
            variant="gold"
            disabled={variant.stockQuantity <= 0}
            loading={addToCart.isPending}
            onPress={() =>
              addToCart.mutate(
                { productVariantId: variant.id, quantity: 1 },
                { onSuccess: () => router.push("/cart") },
              )
            }
          />
        </>
      )}
    </Screen>
  );
}
