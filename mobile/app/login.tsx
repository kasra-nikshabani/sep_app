import { View } from "react-native";
import { Redirect } from "expo-router";
import { useAuth } from "@/lib/auth";
import { useTheme } from "@/hooks/useTheme";
import { spacing } from "@/theme";
import { ThemedText } from "@/components/ThemedText";
import { Button } from "@/components/Button";

export default function LoginScreen() {
  const { session, isLoading, login } = useAuth();
  const { colors } = useTheme();

  if (isLoading) return null;
  if (session) return <Redirect href="/(app)/(tabs)" />;

  return (
    <View style={{ flex: 1, backgroundColor: colors.bg, alignItems: "center", justifyContent: "center", padding: spacing.xxl, gap: spacing.xl }}>
      <ThemedText variant="display" style={{ color: colors.gold }}>
        س
      </ThemedText>
      <ThemedText variant="h1" style={{ textAlign: "center" }}>
        باشگاه فولاد مبارکه سپاهان
      </ThemedText>
      <ThemedText muted style={{ textAlign: "center" }}>
        با یک حساب کاربری مرکزی (Fan ID) به بلیط، فروشگاه، وفاداری و اخبار باشگاه دسترسی داشته باشید.
      </ThemedText>
      <View style={{ width: "100%", marginTop: spacing.xl }}>
        <Button title="ورود با Fan ID" onPress={login} variant="gold" />
      </View>
    </View>
  );
}
