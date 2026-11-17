import { View } from "react-native";
import { Screen, Card } from "@/components/Screen";
import { ThemedText } from "@/components/ThemedText";
import { BrandMark } from "@/components/BrandMark";
import { LoyaltyCard } from "@/components/LoyaltyCard";
import { ServiceTile, ShortcutButton } from "@/components/ServiceTile";
import { spacing } from "@/theme";
import { useLoyaltyAccount } from "@/features/loyalty/api";
import { SERVICE_SECTIONS, type ServiceItem } from "@/features/services/catalog";

const ALL_ITEMS = SERVICE_SECTIONS.flatMap((s) => s.items);
// پرکاربردترین مسیرها: بلیط، اخبار، فروشگاه (از فهرست خدمات؛ تکرار منبع نیست).
const SHORTCUT_LABELS = ["بلیط مسابقات", "اخبار باشگاه", "فروشگاه"];
const SHORTCUTS = SHORTCUT_LABELS.map((l) => ALL_ITEMS.find((i) => i.label === l)).filter(
  (i): i is ServiceItem => !!i,
);

function ServiceSections() {
  return (
    <View style={{ gap: spacing.md }} accessibilityRole="list">
      <ThemedText variant="h2" accessibilityRole="header">
        خدمات
      </ThemedText>
      {SERVICE_SECTIONS.map((section) => {
        // سرویس‌های فعال اول، «به‌زودی» آخر (ترتیب نسبی حفظ می‌شود).
        const items = [...section.items].sort((a, b) => Number(!!a.soon) - Number(!!b.soon));
        return (
          <Card key={section.title} style={{ gap: spacing.md }}>
            <ThemedText accessibilityRole="header" style={{ fontFamily: "Vazirmatn-Black", fontSize: 15.5 }}>
              {section.title}
            </ThemedText>
            <View style={{ flexDirection: "row", flexWrap: "wrap", rowGap: spacing.lg }}>
              {items.map((item) => (
                <ServiceTile key={item.href} item={item} />
              ))}
            </View>
          </Card>
        );
      })}
    </View>
  );
}

export default function HomeScreen() {
  const { data: account, isError: accountError, refetch: refetchAccount } = useLoyaltyAccount();

  return (
    <Screen>
      <BrandMark large />

      <LoyaltyCard account={account} isError={accountError} onRetry={() => refetchAccount()} />

      <View style={{ flexDirection: "row", gap: spacing.sm }}>
        {SHORTCUTS.map((item) => (
          <ShortcutButton key={item.href} item={item} />
        ))}
      </View>

      <ServiceSections />
    </Screen>
  );
}
