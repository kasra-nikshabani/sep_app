import { useLocalSearchParams, Stack } from "expo-router";
import { Screen } from "@/components/Screen";
import { ComingSoon } from "@/components/ComingSoon";
import { SERVICE_SECTIONS } from "@/features/services/catalog";

// دلیل فنی «به‌زودی» بودن هر مورد (برای توسعه‌دهنده، نه کاربر -- قبلاً همین متن‌ها با
// اشاره به Django/ADR/کارفرما مستقیم به هوادار نمایش داده می‌شد):
// - بانکی/خودرو/بیمه/گردشگری: بدون Provider واقعی تأییدشده، پیاده‌سازی حدسی انجام نشد (Phase 13، ADR-0015).
// - شارژ/اینترنت/قبوض: هنوز بررسی نشده و Provider واقعی ندارد.
const DEFAULT_MESSAGE = "این سرویس در حال آماده‌سازی است و به‌زودی در اپ سپاهان فعال می‌شود.";

const MESSAGES: Record<string, string> = {};

export default function SoonScreen() {
  const { key } = useLocalSearchParams<{ key: string }>();
  const item = SERVICE_SECTIONS.flatMap((section) => section.items).find((i) => i.href === `/soon/${key}`);
  const title = item?.label ?? "به‌زودی";

  return (
    <Screen scroll={false}>
      <Stack.Screen options={{ headerShown: true, title }} />
      <ComingSoon title={title} message={MESSAGES[key] ?? DEFAULT_MESSAGE} Icon={item?.Icon} />
    </Screen>
  );
}
