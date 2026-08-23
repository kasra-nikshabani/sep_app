import { useLocalSearchParams, Stack } from "expo-router";
import { Screen } from "@/components/Screen";
import { ComingSoon } from "@/components/ComingSoon";

const CONTENT: Record<string, { title: string; reason: string }> = {
  matches: {
    title: "مسابقات فوتبال",
    reason:
      "مرور/خرید بلیط فوتبال کاملاً داخل سامانه‌ی جدای Django است و آن سامانه هنوز JSON API واقعی برای این جریان ندارد (فقط صفحات HTML سرور-رندر‌شده) -- طبق تصمیم صریح کارفرما، این‌جا حدس زده نشد.",
  },
  services: {
    title: "خدمات",
    reason: "بیمه/سفر/خودرو/سرگرمی هنوز Provider واقعی تأییدشده‌ای ندارند -- طبق همان تصمیم Phase 13 (ADR-0015).",
  },
};

export default function SoonScreen() {
  const { key } = useLocalSearchParams<{ key: string }>();
  const content = CONTENT[key] ?? { title: key, reason: "این بخش هنوز آماده نیست." };

  return (
    <Screen scroll={false}>
      <Stack.Screen options={{ headerShown: true, title: content.title }} />
      <ComingSoon title={content.title} reason={content.reason} />
    </Screen>
  );
}
