import { Tabs } from "expo-router";
import { useTheme } from "@/hooks/useTheme";
import { HomeIcon, NewsIcon, ProfileIcon } from "@/components/Icon";

/**
 * از قبل شامل بلیط/فروشگاه/کیف‌پول هم بود -- طبق درخواست کارفرما به سه مقصد اصلی
 * (خانه/اخبار/حساب کاربری) محدود شد؛ خودِ صفحات بلیط و فروشگاه حذف نشدند، فقط دیگر
 * Tab جدا نیستند -- از گرید خدمات صفحه‌ی خانه در دسترسند (همان مسیر/URL قبلی).
 */
export default function TabsLayout() {
  const { colors } = useTheme();

  return (
    <Tabs
      screenOptions={{
        headerShown: false,
        tabBarActiveTintColor: colors.accent,
        tabBarInactiveTintColor: colors.textMuted,
        // بدون height/paddingBottom ثابت -- طبق خودِ کد Expo Router
        // (BottomTabBar.js: getTabBarHeight)، هر height دستی این فرمول را کامل
        // نادیده می‌گیرد: paddingBottom = spacing/2 + insets.bottom؛ روی یک گوشی
        // واقعی (Home Indicator/Gesture Bar) این باعث می‌شد نوار به لبه‌ی پایین
        // بچسبد و ناقص/بریده نمایش داده شود -- کشف واقعی همین بازخورد کارفرما.
        tabBarStyle: {
          backgroundColor: colors.surface,
          borderTopColor: colors.border,
        },
        tabBarLabelStyle: { fontFamily: "Vazirmatn-Medium", fontSize: 12.5, marginTop: 2 },
        tabBarItemStyle: { paddingVertical: 6 },
      }}
    >
      <Tabs.Screen name="index" options={{ title: "خانه", tabBarIcon: ({ color }) => <HomeIcon color={color as string} size={28} /> }} />
      <Tabs.Screen name="news" options={{ title: "اخبار", tabBarIcon: ({ color }) => <NewsIcon color={color as string} size={28} /> }} />
      <Tabs.Screen
        name="profile"
        options={{ title: "حساب کاربری", tabBarIcon: ({ color }) => <ProfileIcon color={color as string} size={28} /> }}
      />
      {/* شارگذاری‌شده (Loaded) ولی از نوار پایین پنهان -- طبق مستندات Expo Router، بدون
          <Tabs.Screen> صریح یک مسیر Tab نمی‌شود؛ برای وضوح این دو مسیر عمداً با
          href:null اعلام شده‌اند (به‌جای حذف کامل)، تا این استثنا آشکارا مستند بماند.
          کیف‌پول (Wallet) کامل حذف شد -- در فهرست جدید خدمات کارفرما نبود و هیچ‌جای
          دیگری هم به آن Link نمی‌داد. */}
      <Tabs.Screen name="shop" options={{ href: null }} />
      <Tabs.Screen name="tickets" options={{ href: null }} />
    </Tabs>
  );
}
