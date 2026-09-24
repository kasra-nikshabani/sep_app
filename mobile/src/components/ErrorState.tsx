import { View } from "react-native";
import { spacing } from "@/theme";
import { ThemedText } from "./ThemedText";
import { Button } from "./Button";

// قبلاً هیچ صفحه‌ای isError را نمی‌خواند: با قطع Backend، لیست‌ها «موجود نیست» نشان می‌دادند
// (گمراه‌کننده) و صفحات جزئیات برای همیشه Spinner می‌ماندند. پیام خامِ ApiError (متن پاسخ
// سرور) عمداً نمایش داده نمی‌شود -- برای کاربر معنایی ندارد.
export function ErrorState({ onRetry }: { onRetry: () => void }) {
  return (
    <View style={{ alignItems: "center", gap: spacing.md, paddingVertical: spacing.xl }}>
      <ThemedText muted style={{ textAlign: "center" }}>
        دریافت اطلاعات ناموفق بود. اتصال اینترنت را بررسی کنید و دوباره تلاش کنید.
      </ThemedText>
      <Button title="تلاش مجدد" variant="secondary" onPress={() => onRetry()} />
    </View>
  );
}
