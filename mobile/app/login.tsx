import { Image, View } from "react-native";
import { Redirect } from "expo-router";
import { LinearGradient } from "expo-linear-gradient";
import Svg, { Path } from "react-native-svg";
import { useAuth } from "@/lib/auth";
import { useTheme } from "@/hooks/useTheme";
import { palette, radius, spacing } from "@/theme";
import { ThemedText } from "@/components/ThemedText";
import { Button } from "@/components/Button";

const SUN = 380;
const RAY_COUNT = 18;
const RAY_STEP = 360 / RAY_COUNT;

const SUNBURST_PATH = Array.from({ length: RAY_COUNT }, (_, i) => {
  const point = (deg: number) => {
    const rad = (deg * Math.PI) / 180;
    return `${(100 + 100 * Math.cos(rad)).toFixed(2)} ${(100 + 100 * Math.sin(rad)).toFixed(2)}`;
  };
  return `M100 100L${point(i * RAY_STEP)}L${point(i * RAY_STEP + RAY_STEP / 2)}Z`;
}).join("");

export default function LoginScreen() {
  const { session, isLoading, login } = useAuth();
  const { colors } = useTheme();

  if (isLoading) return null;
  if (session) return <Redirect href="/(app)/(tabs)" />;

  return (
    <View style={{ flex: 1, backgroundColor: colors.bg, alignItems: "center", justifyContent: "center", padding: spacing.xl }}>
      <View
        style={{
          width: "100%",
          maxWidth: 420,
          borderRadius: 24,
          overflow: "hidden",
          backgroundColor: colors.surface,
          boxShadow: "0px 16px 40px rgba(156, 114, 9, 0.28)",
        }}
      >
        <LinearGradient
          colors={[palette.gold300, palette.gold500, palette.gold700]}
          start={{ x: 1, y: 0 }}
          end={{ x: 0, y: 1 }}
          style={{ minHeight: 248, alignItems: "center", justifyContent: "center", paddingHorizontal: spacing.xl, paddingVertical: spacing.xxl, gap: spacing.sm }}
        >
          <View
            pointerEvents="none"
            style={{ position: "absolute", width: SUN, height: SUN, left: "50%", top: "46%", marginLeft: -SUN / 2, marginTop: -SUN / 2 }}
          >
            <Svg width={SUN} height={SUN} viewBox="0 0 200 200">
              <Path d={SUNBURST_PATH} fill={palette.n0} fillOpacity={0.22} />
            </Svg>
          </View>
          <Image
            source={require("../assets/sepahan-logo.png")}
            style={{ width: 92, height: 92 }}
            accessibilityLabel="نشان باشگاه فولاد مبارکه سپاهان"
          />
          <ThemedText variant="h1" style={{ textAlign: "center", color: palette.n900, fontSize: 22 }}>
            باشگاه فولاد مبارکه سپاهان
          </ThemedText>
          <ThemedText style={{ textAlign: "center", color: palette.n800, fontFamily: "Vazirmatn-Medium" }}>
            سوپر اپ هواداری
          </ThemedText>
        </LinearGradient>

        <View style={{ padding: spacing.xl, gap: spacing.lg }}>
          <ThemedText style={{ textAlign: "center", lineHeight: 26 }}>
            با Fan ID به بلیط، فروشگاه، امتیاز هواداری و اخبار باشگاه دسترسی دارید.
          </ThemedText>
          <Button title="ورود با Fan ID" onPress={login} variant="gold" />
          <View
            style={{
              borderRadius: radius.card,
              backgroundColor: palette.n100,
              paddingVertical: spacing.md,
              paddingHorizontal: spacing.lg,
            }}
          >
            <ThemedText variant="caption" style={{ textAlign: "center", color: palette.n700, lineHeight: 22 }}>
              حساب ندارید؟ در صفحه بعد با کد ملی ثبت‌نام کنید.
            </ThemedText>
          </View>
        </View>
      </View>
    </View>
  );
}
