import { useEffect, useRef, useState } from "react";
import { AccessibilityInfo, Animated, Easing, Image, Platform, Pressable, View } from "react-native";
import { Link, useIsFocused } from "expo-router";
import { LinearGradient } from "expo-linear-gradient";
import Svg, { Path } from "react-native-svg";
import { ThemedText } from "./ThemedText";
import { ArrowIcon } from "./Icon";
import { palette, radius, spacing } from "@/theme";
import { formatNumber } from "@/lib/format";
import type { Account } from "@/features/loyalty/api";

// روی وب ماژول Native انیمیشن وجود ندارد و useNativeDriver فقط هشدار می‌دهد (NativeAnimatedHelper).
const NATIVE_DRIVER = Platform.OS !== "web";

const PAD = 20;
const LOGO = 60;
const SUN = 440;
const SHINE = 110;
const RAY_COUNT = 18;
const RAY_STEP = 360 / RAY_COUNT;

// پرتوهای خورشیدِ لوگوی سپاهان: هر پرتو نیمِ یک گام است تا پرتو و فاصله یک‌درمیان باشند.
const SUNBURST_PATH = Array.from({ length: RAY_COUNT }, (_, i) => {
  const point = (deg: number) => {
    const rad = (deg * Math.PI) / 180;
    return `${(100 + 100 * Math.cos(rad)).toFixed(2)} ${(100 + 100 * Math.sin(rad)).toFixed(2)}`;
  };
  return `M100 100L${point(i * RAY_STEP)}L${point(i * RAY_STEP + RAY_STEP / 2)}Z`;
}).join("");

function useReducedMotion() {
  const [reduced, setReduced] = useState(false);
  useEffect(() => {
    AccessibilityInfo.isReduceMotionEnabled().then(setReduced);
    const subscription = AccessibilityInfo.addEventListener("reduceMotionChanged", setReduced);
    return () => subscription.remove();
  }, []);
  return reduced;
}

function useCountUp(target: number | undefined, animate: boolean) {
  const value = useRef(new Animated.Value(0)).current;
  const [shown, setShown] = useState<number>();

  useEffect(() => {
    if (target === undefined) return;
    if (!animate) {
      value.setValue(target);
      setShown(target);
      return;
    }
    const id = value.addListener(({ value: v }) => setShown(Math.round(v)));
    const animation = Animated.timing(value, {
      toValue: target,
      duration: 1400,
      easing: Easing.out(Easing.cubic),
      useNativeDriver: false,
    });
    animation.start(({ finished }) => finished && setShown(target));
    return () => {
      animation.stop();
      value.removeListener(id);
    };
  }, [target, animate, value]);

  return shown;
}

export function LoyaltyCard({ account, isError, onRetry }: { account?: Account; isError: boolean; onRetry: () => void }) {
  const reduceMotion = useReducedMotion();
  const focused = useIsFocused();
  const points = useCountUp(account?.pointsBalance, !reduceMotion);
  const [size, setSize] = useState({ width: 0, height: 0 });

  const enter = useRef(new Animated.Value(0)).current;
  const press = useRef(new Animated.Value(1)).current;
  const spin = useRef(new Animated.Value(0)).current;
  const shine = useRef(new Animated.Value(0)).current;

  useEffect(() => {
    Animated.timing(enter, { toValue: 1, duration: 650, easing: Easing.out(Easing.cubic), useNativeDriver: NATIVE_DRIVER }).start();
  }, [enter]);

  // حلقه‌ها فقط وقتی تب خانه دیده می‌شود -- Tabها Mount می‌مانند و انیمیشن پنهان فقط باتری مصرف می‌کند.
  useEffect(() => {
    if (!focused || reduceMotion) return;
    // هر دور دقیقاً یک گام پرتو می‌چرخد تا بازگشت به صفر در شروع دور بعد دیده نشود.
    const spinLoop = Animated.loop(
      Animated.timing(spin, { toValue: 1, duration: 4000, easing: Easing.linear, useNativeDriver: NATIVE_DRIVER }),
    );
    spinLoop.start();

    // برق عمداً با loop(sequence) ساخته نشده: sequence پس از پایان، current را پیش از reset صفر
    // می‌کند و فقط فرزند اول (delay) Reset می‌شود -- برق فقط یک بار رد می‌شد. delay یک timing
    // هم در حلقه‌ی Native فقط یک بار اعمال می‌شود.
    let timer: ReturnType<typeof setTimeout>;
    const sweep = () => {
      shine.setValue(0);
      Animated.timing(shine, { toValue: 1, duration: 1300, easing: Easing.inOut(Easing.quad), useNativeDriver: NATIVE_DRIVER }).start(
        ({ finished }) => {
          if (finished) timer = setTimeout(sweep, 2400);
        },
      );
    };
    timer = setTimeout(sweep, 1200);

    return () => {
      spinLoop.stop();
      clearTimeout(timer);
      shine.stopAnimation();
      shine.setValue(0);
    };
  }, [focused, reduceMotion, spin, shine]);

  const pressTo = (toValue: number) =>
    Animated.spring(press, { toValue, speed: 40, bounciness: 6, useNativeDriver: NATIVE_DRIVER }).start();

  const failed = isError && !account;

  // فقط Transform (نه left/right) برای جای‌گذاری تزئینات: روی Native با forceRTL، left/right
  // جابه‌جا می‌شوند ولی روی وب نه؛ Transform روی هر دو فیزیکی است و لوگو در RTL روی هر دو سمت چپ است.
  const card = (
    <Animated.View
      style={{
        borderRadius: 20,
        boxShadow: "0px 10px 24px rgba(156, 114, 9, 0.35)",
        opacity: enter,
        transform: [{ translateY: enter.interpolate({ inputRange: [0, 1], outputRange: [16, 0] }) }, { scale: press }],
      }}
    >
      <LinearGradient
        colors={[palette.gold300, palette.gold500, palette.gold700]}
        start={{ x: 1, y: 0 }}
        end={{ x: 0, y: 1 }}
        onLayout={(e) => setSize({ width: e.nativeEvent.layout.width, height: e.nativeEvent.layout.height })}
        style={{ borderRadius: 20, overflow: "hidden", minHeight: 200, padding: PAD, justifyContent: "space-between" }}
      >
        {size.width > 0 ? (
          <>
            <Animated.View
              style={{
                position: "absolute",
                width: SUN,
                height: SUN,
                left: "50%",
                top: "50%",
                marginLeft: -SUN / 2,
                marginTop: -SUN / 2,
                pointerEvents: "none",
                transform: [
                  { translateX: PAD + LOGO / 2 - size.width / 2 },
                  { translateY: PAD + LOGO / 2 - size.height / 2 },
                  { rotate: spin.interpolate({ inputRange: [0, 1], outputRange: ["0deg", `${RAY_STEP}deg`] }) },
                ],
              }}
            >
              <Svg width={SUN} height={SUN} viewBox="0 0 200 200">
                <Path d={SUNBURST_PATH} fill={palette.n0} fillOpacity={0.2} />
              </Svg>
            </Animated.View>
            <Animated.View
              style={{
                position: "absolute",
                top: -40,
                bottom: -40,
                width: SHINE,
                left: "50%",
                marginLeft: -SHINE / 2,
                pointerEvents: "none",
                transform: [
                  {
                    translateX: shine.interpolate({
                      inputRange: [0, 1],
                      outputRange: [size.width / 2 + SHINE, -(size.width / 2 + SHINE)],
                    }),
                  },
                  { rotate: "20deg" },
                ],
              }}
            >
              <LinearGradient
                colors={["rgba(255,255,255,0)", "rgba(255,255,255,0.55)", "rgba(255,255,255,0)"]}
                start={{ x: 0, y: 0.5 }}
                end={{ x: 1, y: 0.5 }}
                style={{ flex: 1 }}
              />
            </Animated.View>
          </>
        ) : null}

        <View style={{ flexDirection: "row", alignItems: "center", justifyContent: "space-between" }}>
          <View style={{ gap: 2 }}>
            <ThemedText style={{ fontFamily: "Vazirmatn-Black", fontSize: 16, color: palette.n900 }}>کارت هواداری سپاهان</ThemedText>
            <ThemedText variant="caption" style={{ color: palette.n800 }}>
              باشگاه امتیاز هواداران
            </ThemedText>
          </View>
          <Image source={require("../../assets/sepahan-logo.png")} style={{ width: LOGO, height: LOGO }} />
        </View>

        {failed ? (
          <View style={{ gap: spacing.sm, alignItems: "flex-start", marginTop: spacing.md }}>
            <ThemedText style={{ color: palette.n900 }}>امتیاز شما بارگذاری نشد.</ThemedText>
            <Pressable
              onPress={onRetry}
              accessibilityRole="button"
              style={{ borderWidth: 1, borderColor: palette.n900, borderRadius: radius.pill, paddingVertical: 6, paddingHorizontal: spacing.lg }}
            >
              <ThemedText style={{ color: palette.n900, fontFamily: "Vazirmatn-Medium" }}>تلاش مجدد</ThemedText>
            </Pressable>
          </View>
        ) : (
          <>
            {/* alignItems: flex-start (= راست در RTL): عددِ بدون حرف فارسی روی وب dir=ltr می‌گیرد. */}
            <View style={{ alignItems: "flex-start", marginTop: spacing.md }}>
              <ThemedText variant="caption" style={{ color: palette.n800 }}>
                امتیاز قابل‌خرج
              </ThemedText>
              <View style={{ flexDirection: "row", alignItems: "baseline", gap: 6 }}>
                <ThemedText variant="display" style={{ color: palette.n900, fontSize: 44, fontVariant: ["tabular-nums"] }}>
                  {points === undefined ? "—" : formatNumber(points)}
                </ThemedText>
                <ThemedText style={{ color: palette.n900, fontFamily: "Vazirmatn-Medium" }}>امتیاز</ThemedText>
              </View>
            </View>

            <View style={{ flexDirection: "row", alignItems: "center", justifyContent: "space-between", marginTop: spacing.sm }}>
              <View style={{ flexDirection: "row", alignItems: "center", gap: spacing.sm }}>
                {account?.levelName ? (
                  <View style={{ backgroundColor: "rgba(20,17,10,0.85)", borderRadius: radius.pill, paddingVertical: 3, paddingHorizontal: spacing.sm }}>
                    <ThemedText variant="caption" style={{ color: palette.gold300 }}>
                      سطح {account.levelName}
                    </ThemedText>
                  </View>
                ) : null}
                <ThemedText variant="caption" style={{ color: palette.n800 }}>
                  مجموع کسب‌شده: {formatNumber(account?.lifetimePoints)}
                </ThemedText>
              </View>
              <View
                style={{
                  flexDirection: "row",
                  alignItems: "center",
                  gap: 2,
                  backgroundColor: "rgba(20,17,10,0.85)",
                  borderRadius: radius.pill,
                  paddingVertical: 3,
                  paddingHorizontal: spacing.sm,
                }}
              >
                <ThemedText variant="caption" style={{ color: palette.gold300 }}>
                  جزئیات
                </ThemedText>
                <ArrowIcon color={palette.gold300} size={14} />
              </View>
            </View>
          </>
        )}
      </LinearGradient>
    </Animated.View>
  );

  if (failed) return card;

  return (
    <Link href="/loyalty" asChild>
      <Pressable
        onPressIn={() => pressTo(0.97)}
        onPressOut={() => pressTo(1)}
        accessibilityRole="link"
        accessibilityLabel={`کارت هواداری سپاهان، ${formatNumber(account?.pointsBalance)} امتیاز قابل‌خرج. مشاهده‌ی باشگاه امتیاز`}
      >
        {card}
      </Pressable>
    </Link>
  );
}
