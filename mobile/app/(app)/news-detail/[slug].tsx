import { useState } from "react";
import { View, Image, ActivityIndicator } from "react-native";
import { useLocalSearchParams, Stack } from "expo-router";
import { Screen } from "@/components/Screen";
import { ThemedText } from "@/components/ThemedText";
import { ErrorState } from "@/components/ErrorState";
import { palette, spacing } from "@/theme";
import { useTheme } from "@/hooks/useTheme";
import { newsHeaderOptions } from "@/components/NewsCard";
import { useArticle } from "@/features/news/api";
import { formatJalaliNumeric, toPersianDigits } from "@/lib/format";

const NAVY = "#000039";
const LEAD = "#54537E";

function Cover({ uri }: { uri: string | null }) {
  const [failed, setFailed] = useState(false);
  if (!uri || failed) return null;
  return (
    <Image
      source={{ uri }}
      onError={() => setFailed(true)}
      style={{ width: "100%", height: 230, borderRadius: 16 }}
      resizeMode="cover"
    />
  );
}

export default function NewsDetailScreen() {
  const { slug } = useLocalSearchParams<{ slug: string }>();
  const { colors } = useTheme();
  const { data, isLoading, isError, refetch } = useArticle(slug);
  const paragraphs = (data?.content ?? "")
    .split(/\n{2,}/)
    .map((p) => p.trim())
    .filter(Boolean);
  const sourceId = data?.slug.startsWith("sepahansc-") ? data.slug.slice("sepahansc-".length) : null;

  return (
    <Screen>
      <Stack.Screen options={newsHeaderOptions("خبر")} />
      {isError && !data ? (
        <ErrorState onRetry={refetch} />
      ) : isLoading || !data ? (
        <ActivityIndicator color={colors.accent} />
      ) : (
        <View style={{ gap: spacing.md }}>
          <ThemedText style={{ fontFamily: "Vazirmatn-Black", fontSize: 20, lineHeight: 32, color: NAVY, textAlign: "center" }}>
            {data.title}
          </ThemedText>

          <Cover uri={data.coverImageUrl} />

          {data.summary ? (
            <View
              style={{
                backgroundColor: LEAD,
                borderRadius: 16,
                paddingHorizontal: spacing.lg,
                paddingVertical: spacing.md,
                marginTop: data.coverImageUrl ? -36 : 0,
              }}
            >
              <ThemedText style={{ color: palette.n0, textAlign: "center", lineHeight: 26 }}>{data.summary}</ThemedText>
            </View>
          ) : null}

          <View style={{ flexDirection: "row", alignItems: "center", justifyContent: "space-between" }}>
            {sourceId ? (
              <ThemedText variant="caption" color={LEAD}>
                کد خبر: {toPersianDigits(sourceId)}
              </ThemedText>
            ) : (
              <View />
            )}
            <ThemedText variant="caption" color={LEAD}>
              {formatJalaliNumeric(data.publishedAt)}
            </ThemedText>
          </View>

          <View style={{ gap: spacing.md }}>
            {paragraphs.map((p, i) => (
              <ThemedText key={i} variant="body" style={{ lineHeight: 30, textAlign: "justify" }}>
                {p}
              </ThemedText>
            ))}
          </View>

          {data.tags.length > 0 ? (
            <View style={{ gap: spacing.sm, paddingTop: spacing.sm, borderTopWidth: 1, borderTopColor: colors.border }}>
              <ThemedText variant="caption" color={LEAD}>
                برچسب‌ها
              </ThemedText>
              <View style={{ flexDirection: "row", flexWrap: "wrap", gap: spacing.sm }}>
                {data.tags.map((tag) => (
                  <View
                    key={tag.id}
                    style={{
                      borderWidth: 1,
                      borderColor: LEAD,
                      borderRadius: 999,
                      paddingVertical: 4,
                      paddingHorizontal: spacing.md,
                    }}
                  >
                    <ThemedText variant="caption" color={LEAD}>
                      {tag.name}
                    </ThemedText>
                  </View>
                ))}
              </View>
            </View>
          ) : null}
        </View>
      )}
    </Screen>
  );
}
