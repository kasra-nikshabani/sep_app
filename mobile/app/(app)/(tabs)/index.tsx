import { View, FlatList, Pressable } from "react-native";
import { Link } from "expo-router";
import { Screen, Card } from "@/components/Screen";
import { ThemedText } from "@/components/ThemedText";
import { GiftIcon, BellIcon, ProfileIcon, ArrowIcon } from "@/components/Icon";
import { useTheme } from "@/hooks/useTheme";
import { spacing } from "@/theme";
import { useMe } from "@/features/users/api";
import { useLoyaltyAccount } from "@/features/loyalty/api";
import { useArticles } from "@/features/news/api";
import { formatDate } from "@/lib/format";

const QUICK_LINKS = [
  { href: "/loyalty", label: "باشگاه امتیاز", Icon: GiftIcon },
  { href: "/notifications", label: "اعلان‌ها", Icon: BellIcon },
  { href: "/profile", label: "پروفایل", Icon: ProfileIcon },
  { href: "/soon/matches", label: "مسابقات فوتبال", Icon: GiftIcon },
  { href: "/soon/services", label: "خدمات", Icon: GiftIcon },
] as const;

export default function HomeScreen() {
  const { colors } = useTheme();
  const { data: me } = useMe();
  const { data: account } = useLoyaltyAccount();
  const { data: articles } = useArticles();

  return (
    <Screen>
      <View>
        <ThemedText variant="caption" muted>
          خوش آمدید
        </ThemedText>
        <ThemedText variant="h1">{me?.displayName ?? "هوادار سپاهانی"}</ThemedText>
      </View>

      <Card style={{ backgroundColor: colors.accent, borderColor: colors.accent }}>
        <ThemedText variant="caption" color={colors.accentContrast}>
          امتیاز باشگاه وفاداری
        </ThemedText>
        <ThemedText variant="display" color={colors.gold}>
          {account?.pointsBalance ?? "—"}
        </ThemedText>
        <ThemedText variant="caption" color={colors.accentContrast}>
          سطح: {account?.levelName ?? "—"}
        </ThemedText>
      </Card>

      <View style={{ flexDirection: "row-reverse", flexWrap: "wrap", gap: spacing.md }}>
        {QUICK_LINKS.map(({ href, label, Icon }) => (
          <Link key={href} href={href as never} asChild>
            <Pressable
              style={{
                width: "31%",
                alignItems: "center",
                gap: spacing.xs,
                backgroundColor: colors.surface,
                borderRadius: 14,
                borderWidth: 1,
                borderColor: colors.border,
                paddingVertical: spacing.md,
              }}
            >
              <Icon color={colors.accent} size={22} />
              <ThemedText variant="caption" style={{ textAlign: "center" }}>
                {label}
              </ThemedText>
            </Pressable>
          </Link>
        ))}
      </View>

      <View style={{ flexDirection: "row-reverse", justifyContent: "space-between", alignItems: "center" }}>
        <ThemedText variant="h2">آخرین اخبار</ThemedText>
        <Link href="/news" asChild>
          <Pressable style={{ flexDirection: "row-reverse", alignItems: "center", gap: 4 }}>
            <ThemedText variant="caption" color={colors.goldText}>
              همه‌ی اخبار
            </ThemedText>
            <ArrowIcon color={colors.goldText} size={14} />
          </Pressable>
        </Link>
      </View>

      <FlatList
        data={articles?.slice(0, 5) ?? []}
        keyExtractor={(a) => a.id}
        scrollEnabled={false}
        ItemSeparatorComponent={() => <View style={{ height: spacing.sm }} />}
        renderItem={({ item }) => (
          <Link href={`/news-detail/${item.slug}`} asChild>
            <Pressable>
              <Card>
                <ThemedText variant="caption" color={colors.goldText}>
                  {item.categoryName}
                </ThemedText>
                <ThemedText variant="h2" style={{ fontSize: 16 }}>
                  {item.title}
                </ThemedText>
                <ThemedText variant="caption" muted>
                  {formatDate(item.publishedAt)}
                </ThemedText>
              </Card>
            </Pressable>
          </Link>
        )}
        ListEmptyComponent={
          <ThemedText muted>خبری موجود نیست</ThemedText>
        }
      />
    </Screen>
  );
}
