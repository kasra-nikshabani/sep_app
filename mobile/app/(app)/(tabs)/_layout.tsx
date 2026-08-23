import { Tabs } from "expo-router";
import { useTheme } from "@/hooks/useTheme";
import { HomeIcon, NewsIcon, ShopIcon, TicketIcon, WalletIcon } from "@/components/Icon";

export default function TabsLayout() {
  const { colors } = useTheme();

  return (
    <Tabs
      screenOptions={{
        headerShown: false,
        tabBarActiveTintColor: colors.accent,
        tabBarInactiveTintColor: colors.textMuted,
        tabBarStyle: { backgroundColor: colors.surface, borderTopColor: colors.border },
        tabBarLabelStyle: { fontFamily: "Vazirmatn-Medium", fontSize: 10 },
      }}
    >
      <Tabs.Screen name="index" options={{ title: "خانه", tabBarIcon: ({ color }) => <HomeIcon color={color as string} /> }} />
      <Tabs.Screen name="news" options={{ title: "اخبار", tabBarIcon: ({ color }) => <NewsIcon color={color as string} /> }} />
      <Tabs.Screen name="tickets" options={{ title: "بلیط", tabBarIcon: ({ color }) => <TicketIcon color={color as string} /> }} />
      <Tabs.Screen name="shop" options={{ title: "فروشگاه", tabBarIcon: ({ color }) => <ShopIcon color={color as string} /> }} />
      <Tabs.Screen name="wallet" options={{ title: "کیف‌پول", tabBarIcon: ({ color }) => <WalletIcon color={color as string} /> }} />
    </Tabs>
  );
}
