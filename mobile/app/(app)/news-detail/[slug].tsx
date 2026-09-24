import { View, Image, ActivityIndicator } from "react-native";
import { useLocalSearchParams, Stack } from "expo-router";
import { Screen } from "@/components/Screen";
import { ThemedText } from "@/components/ThemedText";
import { ErrorState } from "@/components/ErrorState";
import { spacing } from "@/theme";
import { useTheme } from "@/hooks/useTheme";
import { useArticle } from "@/features/news/api";
import { formatDateTime } from "@/lib/format";

export default function NewsDetailScreen() {
  const { slug } = useLocalSearchParams<{ slug: string }>();
  const { colors } = useTheme();
  const { data, isLoading, isError, refetch } = useArticle(slug);

  return (
    <Screen>
      <Stack.Screen options={{ headerShown: true, title: data?.title ?? "خبر" }} />
      {isError && !data ? (
        <ErrorState onRetry={refetch} />
      ) : isLoading || !data ? (
        <ActivityIndicator color={colors.accent} />
      ) : (
        <View style={{ gap: spacing.md }}>
          {data.coverImageUrl ? (
            <Image source={{ uri: data.coverImageUrl }} style={{ width: "100%", height: 200, borderRadius: 14 }} />
          ) : null}
          <ThemedText variant="caption" color={colors.goldText}>
            {data.categoryName} · {formatDateTime(data.publishedAt)}
          </ThemedText>
          <ThemedText variant="h1">{data.title}</ThemedText>
          <ThemedText variant="body">{data.content}</ThemedText>
        </View>
      )}
    </Screen>
  );
}
