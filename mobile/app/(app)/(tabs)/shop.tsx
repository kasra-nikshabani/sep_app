import { FlatList, View, Pressable, ActivityIndicator, Image } from "react-native";
import { Link } from "expo-router";
import { Screen, Card } from "@/components/Screen";
import { ThemedText } from "@/components/ThemedText";
import { ShopIcon } from "@/components/Icon";
import { spacing } from "@/theme";
import { useTheme } from "@/hooks/useTheme";
import { useProducts } from "@/features/shop/api";
import { formatRial } from "@/lib/format";

export default function ShopScreen() {
  const { colors } = useTheme();
  const { data, isLoading } = useProducts();

  return (
    <Screen scroll={false}>
      <FlatList
        contentContainerStyle={{ padding: spacing.lg, gap: spacing.md }}
        data={data ?? []}
        keyExtractor={(p) => p.id}
        numColumns={2}
        columnWrapperStyle={{ gap: spacing.md }}
        ListHeaderComponent={
          <View style={{ flexDirection: "row-reverse", justifyContent: "space-between", alignItems: "center", marginBottom: spacing.md }}>
            <ThemedText variant="h1">فروشگاه</ThemedText>
            <Link href="/cart" asChild>
              <Pressable style={{ padding: spacing.sm }}>
                <ShopIcon color={colors.accent} />
              </Pressable>
            </Link>
          </View>
        }
        ItemSeparatorComponent={() => <View style={{ height: spacing.md }} />}
        ListEmptyComponent={isLoading ? <ActivityIndicator color={colors.accent} /> : <ThemedText muted>محصولی موجود نیست</ThemedText>}
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
