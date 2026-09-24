import { useLocalSearchParams, Stack } from "expo-router";
import { Screen } from "@/components/Screen";
import { ComingSoon } from "@/components/ComingSoon";
import { BallIcon, ShieldIcon, SuitcaseIcon, FilmIcon, BankIcon, SimCardIcon, CarIcon, TvIcon, type IconComponent } from "@/components/Icon";

// دلیل فنی «به‌زودی» بودن هر مورد (برای توسعه‌دهنده، نه کاربر -- قبلاً همین متن‌ها با
// اشاره به Django/ADR/کارفرما مستقیم به هوادار نمایش داده می‌شد):
// - matches: خرید بلیط فوتبال کاملاً داخل سامانه‌ی جدای Django است و آن سامانه هنوز JSON API
//   برای این جریان ندارد (فقط صفحات HTML سرور-رندرشده) -- طبق تصمیم صریح کارفرما حدس زده نشد (ADR-0017).
// - بانکی/گردشگری/بیمه/خودرو: بدون Provider واقعی تأییدشده، پیاده‌سازی حدسی انجام نشد (Phase 13، ADR-0015).
// - شارژ/اینترنت/سینما/کنسرت: هنوز بررسی نشده و Provider واقعی ندارد.
// - sepahan-tv: نیازمند یک سرویس واقعی پخش (Streaming) که هنوز راه‌اندازی نشده.
const DEFAULT_MESSAGE = "این سرویس در حال آماده‌سازی است و به‌زودی در اپ سپاهان فعال می‌شود.";

const CONTENT: Record<string, { title: string; message: string; Icon: IconComponent }> = {
  matches: {
    title: "مسابقات فوتبال",
    message: "خرید بلیط مسابقات فوتبال به‌زودی از داخل همین اپ امکان‌پذیر می‌شود.",
    Icon: BallIcon,
  },
  "topup-credit": { title: "خرید شارژ", message: DEFAULT_MESSAGE, Icon: SimCardIcon },
  "topup-internet": { title: "خرید اینترنت", message: DEFAULT_MESSAGE, Icon: SimCardIcon },
  "banking-transfer": { title: "انتقال وجه", message: DEFAULT_MESSAGE, Icon: BankIcon },
  "banking-bill": { title: "پرداخت قبض", message: DEFAULT_MESSAGE, Icon: BankIcon },
  "banking-card": { title: "کارت به کارت", message: DEFAULT_MESSAGE, Icon: BankIcon },
  cinema: { title: "سینما", message: DEFAULT_MESSAGE, Icon: FilmIcon },
  concert: { title: "کنسرت", message: DEFAULT_MESSAGE, Icon: FilmIcon },
  tour: { title: "تور", message: DEFAULT_MESSAGE, Icon: SuitcaseIcon },
  hotel: { title: "هتل", message: DEFAULT_MESSAGE, Icon: SuitcaseIcon },
  flight: { title: "بلیط هواپیما", message: DEFAULT_MESSAGE, Icon: SuitcaseIcon },
  "insurance-third-party": { title: "بیمه شخص ثالث", message: DEFAULT_MESSAGE, Icon: ShieldIcon },
  "insurance-comprehensive": { title: "بیمه بدنه", message: DEFAULT_MESSAGE, Icon: ShieldIcon },
  "vehicle-violations": { title: "خلافی", message: DEFAULT_MESSAGE, Icon: CarIcon },
  "vehicle-toll": { title: "عوارض آزاد راه", message: DEFAULT_MESSAGE, Icon: CarIcon },
  "sepahan-tv": {
    title: "سپاهان TV",
    message: "پخش زنده و آرشیو ویدیوهای باشگاه به‌زودی در همین بخش در دسترس خواهد بود.",
    Icon: TvIcon,
  },
};

export default function SoonScreen() {
  const { key } = useLocalSearchParams<{ key: string }>();
  const content = CONTENT[key] ?? { title: "به‌زودی", message: DEFAULT_MESSAGE, Icon: undefined };

  return (
    <Screen scroll={false}>
      <Stack.Screen options={{ headerShown: true, title: content.title }} />
      <ComingSoon title={content.title} message={content.message} Icon={content.Icon} />
    </Screen>
  );
}
