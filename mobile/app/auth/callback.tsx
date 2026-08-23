import { useEffect, useState } from "react";
import { View, ActivityIndicator } from "react-native";
import { useLocalSearchParams, router } from "expo-router";
import { useAuth } from "@/lib/auth";
import { useTheme } from "@/hooks/useTheme";
import { ThemedText } from "@/components/ThemedText";
import { palette, spacing } from "@/theme";

/**
 * فقط مسیر Web واقعاً به این صفحه می‌رسد -- طبق کشف Phase 15: روی Web،
 * Keycloak کل تب را Redirect می‌کند (نه Popup+postMessage)، پس نمونه‌ی قبلی
 * AuthProvider از بین رفته و promptAsync() هرگز Resolve نمی‌شود. این صفحه
 * دوباره Mount می‌شود و با Code Verifier ذخیره‌شده (نه یک نمونه‌ی تازه)
 * Exchange را کامل می‌کند.
 */
export default function AuthCallbackScreen() {
  const { code, error: oauthError } = useLocalSearchParams<{ code?: string; error?: string }>();
  const { completeExchange, isDiscoveryReady } = useAuth();
  const { colors } = useTheme();
  const [error, setError] = useState<string | undefined>(oauthError);

  useEffect(() => {
    // این نمونه‌ی AuthProvider تازه Mount شده (بعد از Redirect کامل صفحه در
    // Web) -- Discovery Document خودش دوباره باید Fetch شود؛ اگر زودتر از
    // آماده‌شدنش Exchange را صدا بزنیم، همیشه شکست می‌خورد (کشف در Phase 15).
    if (!code || !isDiscoveryReady) return;
    completeExchange(code).then((ok) => {
      router.replace(ok ? "/" : "/login");
      if (!ok) setError("ورود ناموفق بود -- دوباره تلاش کنید");
    });
  }, [code, completeExchange, isDiscoveryReady]);

  return (
    <View style={{ flex: 1, alignItems: "center", justifyContent: "center", backgroundColor: colors.bg, gap: spacing.md }}>
      <ActivityIndicator color={colors.accent} />
      {error ? <ThemedText color={palette.danger}>{error}</ThemedText> : <ThemedText muted>در حال ورود...</ThemedText>}
    </View>
  );
}
