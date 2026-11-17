import { FlatList, View, ActivityIndicator } from "react-native";
import { Stack } from "expo-router";
import { Screen } from "@/components/Screen";
import { ThemedText } from "@/components/ThemedText";
import { ErrorState } from "@/components/ErrorState";
import { FeaturedNewsCard, NewsCard, NewsSectionTitle, newsHeaderOptions } from "@/components/NewsCard";
import { spacing } from "@/theme";
import { useTheme } from "@/hooks/useTheme";
import { useArticles } from "@/features/news/api";

export default function NewsScreen() {
  const { colors } = useTheme();
  const { data, isLoading, isError, refetch, fetchNextPage, hasNextPage, isFetchingNextPage } = useArticles();
  const articles = data?.pages.flat() ?? [];
  const featured = articles[0];
  const rest = articles.slice(1);

  return (
    <Screen scroll={false}>
      <Stack.Screen options={newsHeaderOptions("اخبار باشگاه")} />
      <FlatList
        numColumns={2}
        columnWrapperStyle={{ gap: spacing.sm }}
        contentContainerStyle={{ padding: spacing.lg, paddingBottom: spacing.xxl, gap: spacing.sm }}
        data={rest}
        keyExtractor={(a) => a.id}
        onEndReached={() => hasNextPage && !isFetchingNextPage && fetchNextPage()}
        onEndReachedThreshold={0.5}
        ListHeaderComponent={
          featured ? (
            <View style={{ gap: spacing.md, marginBottom: spacing.sm }}>
              <FeaturedNewsCard article={featured} />
              <NewsSectionTitle />
            </View>
          ) : null
        }
        ListFooterComponent={
          isFetchingNextPage ? <ActivityIndicator color={colors.accent} style={{ marginVertical: spacing.md }} /> : null
        }
        ListEmptyComponent={
          featured ? null : isLoading ? (
            <ActivityIndicator color={colors.accent} style={{ marginTop: spacing.xxl }} />
          ) : isError ? (
            <ErrorState onRetry={refetch} />
          ) : (
            <ThemedText muted style={{ textAlign: "center", marginTop: spacing.xxl }}>
              خبری موجود نیست
            </ThemedText>
          )
        }
        renderItem={({ item }) => <NewsCard article={item} />}
      />
    </Screen>
  );
}
