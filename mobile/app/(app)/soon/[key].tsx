import { useLocalSearchParams, Stack } from "expo-router";
import { Screen } from "@/components/Screen";
import { ComingSoon } from "@/components/ComingSoon";
import { BallIcon, ShieldIcon, SuitcaseIcon, FilmIcon, BankIcon, SimCardIcon, CarIcon, TvIcon, type IconComponent } from "@/components/Icon";

const ADR0015_REASON =
  "این حوزه هنوز Provider واقعی تأییدشده‌ای ندارد -- طبق تصمیم صریح Phase 13 (ADR-0015)، بدون یک قرارداد/API واقعی، پیاده‌سازی حدسی انجام نشد.";

const NO_PROVIDER_YET_REASON =
  "این حوزه هنوز در این پروژه بررسی نشده و یک Provider واقعی برایش تأیید نشده -- مثل بقیه‌ی این فهرست، بدون یک قرارداد/API واقعی، پیاده‌سازی حدسی انجام نشد.";

const CONTENT: Record<string, { title: string; reason: string; Icon: IconComponent }> = {
  matches: {
    title: "مسابقات فوتبال",
    reason:
      "مرور/خرید بلیط فوتبال کاملاً داخل سامانه‌ی جدای Django است و آن سامانه هنوز JSON API واقعی برای این جریان ندارد (فقط صفحات HTML سرور-رندر‌شده) -- طبق تصمیم صریح کارفرما، این‌جا حدس زده نشد.",
    Icon: BallIcon,
  },
  "topup-credit": { title: "خرید شارژ", reason: NO_PROVIDER_YET_REASON, Icon: SimCardIcon },
  "topup-internet": { title: "خرید اینترنت", reason: NO_PROVIDER_YET_REASON, Icon: SimCardIcon },
  "banking-transfer": { title: "انتقال وجه", reason: ADR0015_REASON, Icon: BankIcon },
  "banking-bill": { title: "پرداخت قبض", reason: ADR0015_REASON, Icon: BankIcon },
  "banking-card": { title: "کارت به کارت", reason: ADR0015_REASON, Icon: BankIcon },
  cinema: { title: "سینما", reason: NO_PROVIDER_YET_REASON, Icon: FilmIcon },
  concert: { title: "کنسرت", reason: NO_PROVIDER_YET_REASON, Icon: FilmIcon },
  tour: { title: "تور", reason: ADR0015_REASON, Icon: SuitcaseIcon },
  hotel: { title: "هتل", reason: ADR0015_REASON, Icon: SuitcaseIcon },
  flight: { title: "بلیط هواپیما", reason: ADR0015_REASON, Icon: SuitcaseIcon },
  "insurance-third-party": { title: "بیمه شخص ثالث", reason: ADR0015_REASON, Icon: ShieldIcon },
  "insurance-comprehensive": { title: "بیمه بدنه", reason: ADR0015_REASON, Icon: ShieldIcon },
  "vehicle-violations": { title: "خلافی", reason: ADR0015_REASON, Icon: CarIcon },
  "vehicle-toll": { title: "عوارض آزاد راه", reason: ADR0015_REASON, Icon: CarIcon },
  "sepahan-tv": {
    title: "سپاهان TV",
    reason:
      "پخش زنده/آرشیو ویدیو نیازمند یک سرویس واقعی پخش (Streaming) است که هنوز در این پروژه راه‌اندازی نشده -- مثل بقیه‌ی این فهرست، بدون یک زیرساخت واقعی، پیاده‌سازی حدسی انجام نشد.",
    Icon: TvIcon,
  },
};

export default function SoonScreen() {
  const { key } = useLocalSearchParams<{ key: string }>();
  const content = CONTENT[key] ?? { title: key, reason: "این بخش هنوز آماده نیست.", Icon: undefined };

  return (
    <Screen scroll={false}>
      <Stack.Screen options={{ headerShown: true, title: content.title }} />
      <ComingSoon title={content.title} reason={content.reason} Icon={content.Icon} />
    </Screen>
  );
}
