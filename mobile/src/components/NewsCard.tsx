import { useState } from "react";
import { Image, Pressable, View } from "react-native";
import { Link } from "expo-router";
import Svg, { Circle, Path, Rect } from "react-native-svg";
import { ThemedText } from "./ThemedText";
import { palette, spacing } from "@/theme";
import { formatJalaliNumeric } from "@/lib/format";
import type { Article } from "@/features/news/api";

// رنگ‌های صفحه‌ی اخبار سایت باشگاه (sepahansc.com): سرمه‌ای، زرد نشان، و خاکستری متادیتا.
const NAVY = "#000039";
const YELLOW = "#FFCD03";
const MUTED = "#54537E";
const CARD_RADIUS = 20;

function Cover({ uri, height }: { uri: string | null; height: number }) {
  const [failed, setFailed] = useState(false);
  if (!uri || failed) {
    return <View style={{ height, backgroundColor: "#EFEFEF" }} />;
  }
  return (
    <Image
      source={{ uri }}
      onError={() => setFailed(true)}
      style={{ width: "100%", height }}
      resizeMode="cover"
    />
  );
}

function YellowDot() {
  return <View style={{ width: 7, height: 7, borderRadius: 4, backgroundColor: YELLOW, marginTop: 7 }} />;
}

function CalendarIcon({ color }: { color: string }) {
  return (
    <Svg width={12} height={12} viewBox="0 0 24 24" fill="none" stroke={color} strokeWidth={2}>
      <Rect x={3} y={5} width={18} height={16} rx={2} />
      <Path d="M3 10h18M8 3v4M16 3v4" />
    </Svg>
  );
}

function MetaRow({ article, light, compact }: { article: Article; light?: boolean; compact?: boolean }) {
  const color = light ? "#D9D8EA" : MUTED;
  return (
    <View style={{ flexDirection: "row", alignItems: "center", justifyContent: "space-between", gap: spacing.sm }}>
      {compact ? (
        <View />
      ) : (
        <View style={{ flexDirection: "row", alignItems: "center", gap: 4, flexShrink: 1 }}>
          <View style={{ width: 5, height: 5, borderRadius: 3, backgroundColor: color }} />
          <ThemedText variant="caption" color={color} numberOfLines={1}>
            {article.categoryName}
          </ThemedText>
        </View>
      )}
      <View style={{ flexDirection: "row", alignItems: "center", gap: 4 }}>
        <CalendarIcon color={color} />
        <ThemedText variant="caption" color={color}>
          {formatJalaliNumeric(article.publishedAt)}
        </ThemedText>
      </View>
    </View>
  );
}

/** خبر اول: تصویر بزرگ و نوار سرمه‌ای، مثل کارت بالای صفحه‌ی اصلی سایت باشگاه. */
export function FeaturedNewsCard({ article }: { article: Article }) {
  return (
    <Link href={`/news-detail/${article.slug}` as never} asChild>
      <Pressable
        accessibilityRole="button"
        accessibilityLabel={`${article.title}، ${formatJalaliNumeric(article.publishedAt)}`}
        style={{ borderRadius: CARD_RADIUS, overflow: "hidden", backgroundColor: NAVY }}
      >
        <Cover uri={article.coverImageUrl} height={210} />
        <View style={{ paddingHorizontal: spacing.md, paddingVertical: spacing.md, gap: spacing.sm }}>
          <View style={{ flexDirection: "row", alignItems: "flex-start", gap: 6 }}>
            <YellowDot />
            <ThemedText
              style={{ flex: 1, fontFamily: "Vazirmatn-Black", fontSize: 15, lineHeight: 24, color: palette.n0 }}
              numberOfLines={3}
            >
              {article.title}
            </ThemedText>
          </View>
          <MetaRow article={article} light />
        </View>
      </Pressable>
    </Link>
  );
}

/** بقیه‌ی اخبار: کارت سفید با تصویر بالا و نقطه‌ی زرد کنار عنوان. */
export function NewsCard({ article }: { article: Article }) {
  return (
    <Link href={`/news-detail/${article.slug}` as never} asChild>
      <Pressable
        accessibilityRole="button"
        accessibilityLabel={`${article.title}، ${formatJalaliNumeric(article.publishedAt)}`}
        style={{
          flex: 1,
          backgroundColor: palette.n0,
          borderRadius: 16,
          overflow: "hidden",
          borderWidth: 1,
          borderColor: "#EFEFEF",
        }}
      >
        <Cover uri={article.coverImageUrl} height={108} />
        <View style={{ padding: spacing.sm, gap: spacing.sm, minHeight: 96, justifyContent: "space-between" }}>
          <View style={{ flexDirection: "row", alignItems: "flex-start", gap: 4 }}>
            <YellowDot />
            <ThemedText
              style={{ flex: 1, fontFamily: "Vazirmatn-Medium", fontSize: 12.5, lineHeight: 20, color: NAVY }}
              numberOfLines={3}
            >
              {article.title}
            </ThemedText>
          </View>
          <MetaRow article={article} compact />
        </View>
      </Pressable>
    </Link>
  );
}

/** نوار «اخبار سپاهان»: زمینه‌ی سرمه‌ای، آیکن زرد سمت راست، مثل بلوک اخبار سایت باشگاه. */
export function NewsSectionTitle() {
  return (
    <View
      style={{
        flexDirection: "row",
        alignItems: "center",
        gap: spacing.sm,
        marginTop: spacing.xs,
        backgroundColor: "#000025",
        borderRadius: 18,
        paddingVertical: 10,
        paddingHorizontal: spacing.md,
      }}
    >
      <View
        style={{
          width: 34,
          height: 34,
          borderRadius: 17,
          backgroundColor: YELLOW,
          alignItems: "center",
          justifyContent: "center",
        }}
      >
        <Svg width={16} height={16} viewBox="0 0 24 24" fill="none" stroke={NAVY} strokeWidth={2}>
          <Rect x={3} y={5} width={18} height={14} rx={2} />
          <Path d="M3 10h18" />
          <Circle cx={8} cy={14} r={1} fill={NAVY} />
        </Svg>
      </View>
      <ThemedText style={{ fontFamily: "Vazirmatn-Black", fontSize: 18, color: palette.n0 }}>اخبار سپاهان</ThemedText>
    </View>
  );
}

/** هدر صفحه‌های اخبار: نوار سرمه‌ای با خط زرد پایین، هم‌رنگ هدر سایت باشگاه. */
export function newsHeaderOptions(title: string) {
  return {
    headerShown: true,
    title,
    headerTintColor: palette.n0,
    headerTitleStyle: { fontFamily: "Vazirmatn-Black", fontSize: 17, color: palette.n0 },
    headerShadowVisible: false,
    headerBackground: () => (
      <View style={{ flex: 1, backgroundColor: NAVY, borderBottomWidth: 3, borderBottomColor: YELLOW }} />
    ),
  };
}
