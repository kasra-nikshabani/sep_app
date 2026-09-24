import { lightColors, type ThemeColors } from "@/theme";

// طبق درخواست صریح کارفرما، حالت تیره کامل غیرفعال شد -- همیشه پس‌زمینه‌ی سفید/روشن
// با آیکون‌های مشکی، مستقل از تنظیمات تیره‌ی سیستم‌عامل کاربر.
export function useTheme(): { colors: ThemeColors; dark: boolean } {
  return { colors: lightColors, dark: false };
}
