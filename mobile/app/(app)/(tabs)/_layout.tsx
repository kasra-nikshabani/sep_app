import { Tabs } from "expo-router";
import { useSafeAreaInsets } from "react-native-safe-area-context";
import { useTheme } from "@/hooks/useTheme";
import { HomeIcon, NewsIcon, ProfileIcon } from "@/components/Icon";

// ارتفاع پیش‌فرض Expo Router (۴۹) برای آیکن ۲۸ + برچسب Vazirmatn کافی نبود -- برچسب‌ها
// روی گوشی به ارتفاع صفر فشرده و کاملاً نامرئی می‌شدند (فقط روی تبلت که برچسب کنار آیکن
// است دیده می‌شدند).
const TAB_BAR_CONTENT_HEIGHT = 64;

/**
 * از قبل شامل بلیط/فروشگاه/کیف‌پول هم بود -- طبق درخواست کارفرما به سه مقصد اصلی
 * (خانه/اخبار/حساب کاربری) محدود شد؛ خودِ صفحات بلیط و فروشگاه حذف نشدند، فقط دیگر
 * Tab جدا نیستند -- از گرید خدمات صفحه‌ی خانه در دسترسند (همان مسیر/URL قبلی).
 *
 * این دو صفحه قبلاً به‌صورت Tab پنهان (href:null) همین‌جا مانده بودند؛ نتیجه: ورود از
 * خانه نه دکمه‌ی بازگشت داشت نه Tab فعال (کاربر گیر می‌افتاد). حالا مثل سبد خرید/سفارش‌ها
 * در Stack والد (app/(app)/) هستند -- با Header و بازگشت واقعی.
 *
 * کیف‌پول (Wallet) کامل حذف شد -- در فهرست جدید خدمات کارفرما نبود و هیچ‌جای دیگری هم
 * به آن Link نمی‌داد.
 */
export default function TabsLayout() {
  const { colors } = useTheme();
  const insets = useSafeAreaInsets();

  return (
    <Tabs
      screenOptions={{
        headerShown: false,
        tabBarActiveTintColor: colors.accent,
        tabBarInactiveTintColor: colors.textMuted,
        // طبق خودِ کد Expo Router (BottomTabBar.js: getTabBarHeight)، height دستی
        // بدون افزودن insets.bottom برگردانده می‌شود؛ یک height ثابت (بدون inset) روی
        // گوشی واقعی (Home Indicator/Gesture Bar) باعث می‌شد نوار به لبه‌ی پایین بچسبد
        // و بریده شود -- کشف واقعی بازخورد کارفرما. پس inset همین‌جا صریحاً اضافه
        // می‌شود؛ paddingBottom = insets.bottom را خودِ BottomTabBar حفظ می‌کند.
        tabBarStyle: {
          backgroundColor: colors.surface,
          borderTopColor: colors.border,
          height: TAB_BAR_CONTENT_HEIGHT + insets.bottom,
        },
        tabBarLabelStyle: { fontFamily: "Vazirmatn-Medium", fontSize: 12.5, marginTop: 2 },
      }}
    >
      <Tabs.Screen name="index" options={{ title: "خانه", tabBarIcon: ({ color }) => <HomeIcon color={color as string} size={28} /> }} />
      <Tabs.Screen name="news" options={{ title: "اخبار", tabBarIcon: ({ color }) => <NewsIcon color={color as string} size={28} /> }} />
      <Tabs.Screen
        name="profile"
        options={{ title: "حساب کاربری", tabBarIcon: ({ color }) => <ProfileIcon color={color as string} size={28} /> }}
      />
    </Tabs>
  );
}
