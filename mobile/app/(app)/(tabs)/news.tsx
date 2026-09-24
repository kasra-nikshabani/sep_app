import { FlatList, View, Pressable, ActivityIndicator, Image } from "react-native";
import { Link } from "expo-router";
import { Screen, Card } from "@/components/Screen";
import { ThemedText } from "@/components/ThemedText";
import { BrandMark } from "@/components/BrandMark";
import { spacing } from "@/theme";
import { useTheme } from "@/hooks/useTheme";
import { useArticles } from "@/features/news/api";
import { formatDate } from "@/lib/format";

export default function NewsScreen() {
  const { colors } = useTheme();
  const { data, isLoading } = useArticles();

  return (
    <Screen scroll={false}>
      <FlatList
        contentContainerStyle={{ padding: spacing.lg, gap: spacing.md }}
        data={data ?? []}
        keyExtractor={(a) => a.id}
        ListHeaderComponent={
          <View style={{ gap: spacing.md }}>
            <BrandMark />
            <ThemedText variant="h1">اخبار</ThemedText>
          </View>
        }
        ItemSeparatorComponent={() => <View style={{ height: spacing.md }} />}
        ListEmptyComponent={
          isLoading ? <ActivityIndicator color={colors.accent} /> : <ThemedText muted>خبری موجود نیست</ThemedText>
        }
        renderItem={({ item }) => (
          <Link href={`/news-detail/${item.slug}`} asChild>
            <Pressable>
              <Card style={{ flexDirection: "row-reverse", alignItems: "center" }}>
                {item.coverImageUrl ? (
                  <Image source={{ uri: item.coverImageUrl }} style={{ width: 64, height: 64, borderRadius: 10 }} />
                ) : null}
                <View style={{ flex: 1, gap: 4 }}>
                  <ThemedText variant="caption" color={colors.goldText}>
                    {item.categoryName}
                  </ThemedText>
                  <ThemedText style={{ fontFamily: "Vazirmatn-Medium" }}>{item.title}</ThemedText>
                  <ThemedText variant="caption" muted>
                    {formatDate(item.publishedAt)}
                  </ThemedText>
                </View>
              </Card>
            </Pressable>
          </Link>
        )}
      />
    </Screen>
  );
}
