import { FlatList, View, Pressable, ActivityIndicator, Image } from "react-native";
import { Link, Stack } from "expo-router";
import { Screen, Card } from "@/components/Screen";
import { ThemedText } from "@/components/ThemedText";
import { ErrorState } from "@/components/ErrorState";
import { ShopIcon } from "@/components/Icon";
import { spacing } from "@/theme";
import { useTheme } from "@/hooks/useTheme";
import { useProducts } from "@/features/shop/api";
import { formatRial } from "@/lib/format";

export default function ShopScreen() {
  const { colors } = useTheme();
  const { data, isLoading, isError, refetch } = useProducts();

  return (
    <Screen scroll={false}>
      <Stack.Screen
        options={{
          headerShown: true,
          title: "فروشگاه",
          headerRight: () => (
            <Link href="/cart" asChild>
              <Pressable accessibilityRole="button" accessibilityLabel="سبد خرید" hitSlop={8} style={{ padding: spacing.sm }}>
                <ShopIcon color={colors.accent} />
              </Pressable>
            </Link>
          ),
        }}
      />
      <FlatList
        contentContainerStyle={{ padding: spacing.lg, gap: spacing.md }}
        data={data ?? []}
        keyExtractor={(p) => p.id}
        numColumns={2}
        columnWrapperStyle={{ gap: spacing.md }}
        ItemSeparatorComponent={() => <View style={{ height: spacing.md }} />}
        ListEmptyComponent={
          isLoading ? (
            <ActivityIndicator color={colors.accent} />
          ) : isError ? (
            <ErrorState onRetry={refetch} />
          ) : (
            <ThemedText muted>محصولی موجود نیست</ThemedText>
          )
        }
        renderItem={({ item }) => (
          <Link href={`/shop-detail/${item.id}`} asChild>
            <Pressable style={{ flex: 1 }}>
              <Card style={{ gap: 6 }}>
                {item.imageUrl ? (
                  <Image source={{ uri: item.imageUrl }} style={{ width: "100%", height: 100, borderRadius: 10 }} />
                ) : (
                  <View style={{ width: "100%", height: 100, borderRadius: 10, backgroundColor: colors.bg }} />
                )}
                <ThemedText style={{ fontFamily: "Vazirmatn-Medium" }} numberOfLines={2}>
                  {item.name}
                </ThemedText>
                <ThemedText variant="numeric" color={colors.goldText}>
                  {formatRial(item.basePrice)}
                </ThemedText>
              </Card>
            </Pressable>
          </Link>
        )}
      />
    </Screen>
  );
}
