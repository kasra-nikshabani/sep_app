import { useState } from "react";
import { View, Pressable } from "react-native";
import { Link } from "expo-router";
import { LinearGradient } from "expo-linear-gradient";
import { Screen, Card } from "@/components/Screen";
import { ThemedText } from "@/components/ThemedText";
import {
  NewsIcon,
  ShopIcon,
  SimCardIcon,
  BankIcon,
  BallIcon,
  FilmIcon,
  SuitcaseIcon,
  ShieldIcon,
  CarIcon,
  TvIcon,
  ProfileIcon,
  type IconComponent,
} from "@/components/Icon";
import { ServiceMenuSheet, type ServiceMenuOption } from "@/components/ServiceMenuSheet";
import { BrandMark } from "@/components/BrandMark";
import { useTheme } from "@/hooks/useTheme";
import { spacing, radius, palette } from "@/theme";
import { useMe } from "@/features/users/api";
import { useLoyaltyAccount } from "@/features/loyalty/api";
import { useAuth } from "@/lib/auth";
import { formatNumber, toPersianDigits } from "@/lib/format";

type ServiceItem =
  | { kind: "link"; label: string; Icon: IconComponent; href: string; soon?: boolean }
  | { kind: "menu"; label: string; Icon: IconComponent; options: ServiceMenuOption[] };

// خدماتی که باشگاه واقعاً ارائه می‌دهد -- شامل موارد آماده و موارد «به‌زودی صادقانه»
// (بیمه/سیمکارت/بانکی/گردشگری: طبق تصمیم صریح Phase 13/ADR-0015 و توافق‌های بعدی،
// بدون یک Provider واقعی، پیاده‌سازی حدسی انجام نشد -- فقط زیردسته‌بندی مشخص شد).
const SERVICES: ServiceItem[] = [
  { kind: "link", label: "اخبار", Icon: NewsIcon, href: "/news" },
  { kind: "link", label: "فروشگاه", Icon: ShopIcon, href: "/shop" },
  { kind: "link", label: "سپاهان TV", Icon: TvIcon, href: "/soon/sepahan-tv", soon: true },
  {
    kind: "menu",
    label: "سیمکارت",
    Icon: SimCardIcon,
    options: [
      { label: "خرید شارژ", href: "/soon/topup-credit", soon: true },
      { label: "خرید اینترنت", href: "/soon/topup-internet", soon: true },
    ],
  },
  {
    kind: "menu",
    label: "خدمات بانکی",
    Icon: BankIcon,
    options: [
      { label: "انتقال وجه", href: "/soon/banking-transfer", soon: true },
      { label: "پرداخت قبض", href: "/soon/banking-bill", soon: true },
      { label: "کارت به کارت", href: "/soon/banking-card", soon: true },
    ],
  },
  { kind: "link", label: "مسابقات فوتبال", Icon: BallIcon, href: "/soon/matches", soon: true },
  {
    kind: "menu",
    label: "سرگرمی",
    Icon: FilmIcon,
    options: [
      { label: "تئاتر", href: "/tickets" },
      { label: "سینما", href: "/soon/cinema", soon: true },
      { label: "کنسرت", href: "/soon/concert", soon: true },
    ],
  },
  {
    kind: "menu",
    label: "گردشگری",
    Icon: SuitcaseIcon,
    options: [
      { label: "تور", href: "/soon/tour", soon: true },
      { label: "هتل", href: "/soon/hotel", soon: true },
      { label: "بلیط هواپیما", href: "/soon/flight", soon: true },
    ],
  },
  {
    kind: "menu",
    label: "بیمه",
    Icon: ShieldIcon,
    options: [
      { label: "بیمه شخص ثالث", href: "/soon/insurance-third-party", soon: true },
      { label: "بیمه بدنه", href: "/soon/insurance-comprehensive", soon: true },
    ],
  },
  {
    kind: "menu",
    label: "خدمات خودرو",
    Icon: CarIcon,
    options: [
      { label: "خلافی", href: "/soon/vehicle-violations", soon: true },
      { label: "عوارض آزاد راه", href: "/soon/vehicle-toll", soon: true },
    ],
  },
];

function isFullySoon(item: ServiceItem): boolean {
  if (item.kind === "link") return !!item.soon;
  return item.options.every((option) => option.soon);
}

function ServiceGrid({ items, onOpenMenu }: { items: ServiceItem[]; onOpenMenu: (item: ServiceItem & { kind: "menu" }) => void }) {
  return (
    <View style={{ flexDirection: "row", flexWrap: "wrap", gap: spacing.md, rowGap: spacing.lg }}>
      {items.map((item) => {
        const soon = isFullySoon(item);
        const tileContent = (
          <>
            <View
              style={{
                width: 60,
                height: 60,
                borderRadius: radius.card,
                alignItems: "center",
                justifyContent: "center",
                backgroundColor: palette.n900,
              }}
            >
              <item.Icon color={palette.n0} size={30} />
            </View>
            <ThemedText variant="caption" style={{ textAlign: "center", fontFamily: "Vazirmatn-Medium" }} muted={soon}>
              {item.label}
            </ThemedText>
            <View style={{ height: 12 }}>
              {soon ? (
                <ThemedText variant="caption" style={{ textAlign: "center", fontSize: 10, color: palette.n900 }}>
                  به‌زودی
                </ThemedText>
              ) : null}
            </View>
          </>
        );

        if (item.kind === "menu") {
          return (
            <Pressable
              key={item.label}
              onPress={() => onOpenMenu(item)}
              style={{ width: "22%", alignItems: "center", gap: spacing.xs }}
            >
              {tileContent}
            </Pressable>
          );
        }

        return (
          <Link key={item.href} href={item.href as never} asChild>
            <Pressable style={{ width: "22%", alignItems: "center", gap: spacing.xs }}>{tileContent}</Pressable>
          </Link>
        );
      })}
    </View>
  );
}

// نمایشی -- هنوز به یک منبع واقعی جدول لیگ وصل نیست (Django فقط صفحات HTML سرور-رندرشده
// عکس ثابت (Snapshot) واقعی -- نه یک اتصال زنده/API. از سایت ورزش۳ در تاریخ ۲ شهریور ۱۴۰۵
// دستی گرفته شد (هفته‌ی سوم لیگ برتر). یک اتصال زنده‌ی واقعی نیازمند Backend/Provider
// جداست (خارج از دامنه‌ی همین تغییر) -- تا وقتی آن ساخته نشود، این عدد‌ها به‌روز نمی‌مانند.
const LEAGUE_TABLE = [
  { team: "استقلال", played: 3, points: 9 },
  { team: "تراکتور", played: 3, points: 9 },
  { team: "گل‌گهر سیرجان", played: 3, points: 7 },
  { team: "آلومینیوم اراک", played: 3, points: 7 },
  { team: "پرسپولیس", played: 3, points: 6 },
  { team: "فولاد", played: 3, points: 5 },
  { team: "فجر سپاسی", played: 3, points: 5 },
  { team: "ملوان", played: 3, points: 4 },
  { team: "پیکان", played: 3, points: 4 },
  { team: "سپاهان", played: 3, points: 3 },
  { team: "خیبر خرم‌آباد", played: 3, points: 2 },
  { team: "ذوب‌آهن", played: 3, points: 2 },
  { team: "صنعت نفت آبادان", played: 3, points: 2 },
  { team: "مس شهر بابک", played: 3, points: 2 },
  { team: "نساجی مازندران", played: 3, points: 1 },
  { team: "شمس آذر قزوین", played: 3, points: 1 },
  { team: "استقلال خوزستان", played: 3, points: 1 },
  { team: "چادرملو اردکان", played: 3, points: 1 },
];

function LeagueTable() {
  const { colors } = useTheme();
  return (
    <View style={{ gap: spacing.md }}>
      <View style={{ flexDirection: "row", justifyContent: "space-between", alignItems: "center" }}>
        <ThemedText variant="h2">جدول لیگ برتر</ThemedText>
        <ThemedText variant="caption" muted>
          هفته ۳
        </ThemedText>
      </View>

      <Card style={{ padding: 0, gap: 0, overflow: "hidden" }}>
        <View
          style={{
            flexDirection: "row",
            paddingVertical: spacing.sm,
            paddingHorizontal: spacing.md,
            backgroundColor: colors.bg,
          }}
        >
          <ThemedText variant="caption" muted style={{ width: 24, textAlign: "center" }}>
            #
          </ThemedText>
          <ThemedText variant="caption" muted style={{ flex: 1, textAlign: "right" }}>
            تیم
          </ThemedText>
          <ThemedText variant="caption" muted style={{ width: 44, textAlign: "center" }}>
            بازی
          </ThemedText>
          <ThemedText variant="caption" muted style={{ width: 44, textAlign: "center" }}>
            امتیاز
          </ThemedText>
        </View>
        {LEAGUE_TABLE.map((row, index) => (
          <View
            key={row.team}
            style={{
              flexDirection: "row",
              alignItems: "center",
              paddingVertical: spacing.sm,
              paddingHorizontal: spacing.md,
              borderTopWidth: 1,
              borderTopColor: colors.border,
              backgroundColor: row.team === "سپاهان" ? colors.bg : "transparent",
            }}
          >
            <ThemedText variant="caption" muted style={{ width: 24, textAlign: "center" }}>
              {toPersianDigits(index + 1)}
            </ThemedText>
            <ThemedText
              style={{
                flex: 1,
                textAlign: "right",
                fontFamily: row.team === "سپاهان" ? "Vazirmatn-Black" : "Vazirmatn-Medium",
              }}
            >
              {row.team}
            </ThemedText>
            <ThemedText variant="caption" muted style={{ width: 44, textAlign: "center", fontVariant: ["tabular-nums"] }}>
              {toPersianDigits(row.played)}
            </ThemedText>
            <ThemedText style={{ width: 44, textAlign: "center", fontFamily: "Vazirmatn-Medium", fontVariant: ["tabular-nums"] }}>
              {toPersianDigits(row.points)}
            </ThemedText>
          </View>
        ))}
      </Card>

      <ThemedText variant="caption" muted style={{ textAlign: "center" }}>
        منبع: ورزش۳ · آخرین به‌روزرسانی: ۲ شهریور ۱۴۰۵
      </ThemedText>
    </View>
  );
}

export default function HomeScreen() {
  const { colors } = useTheme();
  const { data: me } = useMe();
  const { data: account, isError: accountError, refetch: refetchAccount } = useLoyaltyAccount();
  const { session } = useAuth();
  const [activeMenu, setActiveMenu] = useState<(ServiceItem & { kind: "menu" }) | null>(null);
  const displayName = me?.displayName ?? session?.displayName ?? "—";

  return (
    <Screen>
      <BrandMark />

      <View style={{ flexDirection: "row", alignItems: "center", gap: spacing.md }}>
        <View
          style={{
            width: 60,
            height: 60,
            borderRadius: 30,
            backgroundColor: colors.surfaceRaised,
            borderWidth: 1,
            borderColor: colors.border,
            alignItems: "center",
            justifyContent: "center",
          }}
        >
          {/* بدون فیلد عکس پروفایل در Backend فعلاً -- همیشه آیکون پیش‌فرض تا وقتی این قابلیت واقعاً ساخته شود. */}
          <ProfileIcon color={palette.n900} size={30} />
        </View>
        <View style={{ flex: 1 }}>
          <ThemedText variant="caption" muted>
            خوش آمدی هوادار سپاهانی
          </ThemedText>
          <ThemedText variant="h1" style={{ fontSize: 22 }}>
            {displayName}
          </ThemedText>
        </View>
      </View>

      <LinearGradient
        colors={[palette.gold300, palette.gold500, palette.gold700]}
        // روشن از بالا-راست (شروع متن در RTL) به تیره در پایین-چپ -- قبلاً برعکس بود و «سطح»
        // درست روی تیره‌ترین گوشه (gold700) می‌افتاد: کنتراست ۳.۳:۱. alignItems: flex-start
        // (= راست در RTL) عدد امتیاز را هم -- که بدون حرف فارسی dir=ltr می‌گیرد -- سمت راست نگه می‌دارد.
        start={{ x: 1, y: 0 }}
        end={{ x: 0, y: 1 }}
        style={{ borderRadius: radius.card, padding: spacing.lg, gap: spacing.sm, alignItems: "flex-start" }}
      >
        <ThemedText variant="caption" style={{ color: palette.n900 }}>
          امتیاز باشگاه وفاداری
        </ThemedText>
        {accountError && !account ? (
          <View style={{ gap: spacing.sm, alignItems: "flex-start" }}>
            <ThemedText style={{ color: palette.n900 }}>امتیاز شما بارگذاری نشد.</ThemedText>
            <Pressable
              onPress={() => refetchAccount()}
              accessibilityRole="button"
              style={{ borderWidth: 1, borderColor: palette.n900, borderRadius: radius.pill, paddingVertical: 6, paddingHorizontal: spacing.lg }}
            >
              <ThemedText style={{ color: palette.n900, fontFamily: "Vazirmatn-Medium" }}>تلاش مجدد</ThemedText>
            </Pressable>
          </View>
        ) : (
          <>
            <ThemedText
              variant="display"
              style={{ color: palette.n900, fontSize: 44, letterSpacing: -0.5, fontVariant: ["tabular-nums"] }}
            >
              {formatNumber(account?.pointsBalance)}
            </ThemedText>
            <ThemedText style={{ color: palette.n900, fontFamily: "Vazirmatn-Medium", fontSize: 12.5 }}>
              سطح: {account?.levelName ?? "—"}
            </ThemedText>
          </>
        )}
      </LinearGradient>

      <View style={{ gap: spacing.md }}>
        <ThemedText variant="h2">خدمات</ThemedText>
        <ServiceGrid items={SERVICES} onOpenMenu={setActiveMenu} />
      </View>

      <LeagueTable />

      {activeMenu && (
        <ServiceMenuSheet
          visible={!!activeMenu}
          title={activeMenu.label}
          Icon={activeMenu.Icon}
          options={activeMenu.options}
          onClose={() => setActiveMenu(null)}
        />
      )}
    </Screen>
  );
}
