import { Pressable, ActivityIndicator, type GestureResponderEvent } from "react-native";
import { useTheme } from "@/hooks/useTheme";
import { palette, radius, spacing } from "@/theme";
import { ThemedText } from "./ThemedText";

type Variant = "primary" | "gold" | "secondary" | "ghost" | "danger";

export function Button({
  title,
  onPress,
  variant = "primary",
  disabled = false,
  loading = false,
}: {
  title: string;
  onPress?: (e: GestureResponderEvent) => void;
  variant?: Variant;
  disabled?: boolean;
  loading?: boolean;
}) {
  const { colors } = useTheme();

  const styles: Record<Variant, { bg: string; text: string; border?: string }> = {
    primary: { bg: colors.accent, text: colors.accentContrast },
    gold: { bg: palette.gold500, text: palette.n900 },
    secondary: { bg: "transparent", text: colors.text, border: colors.border },
    ghost: { bg: "transparent", text: colors.textMuted },
    danger: { bg: palette.danger, text: "#fff" },
  };
  const s = styles[variant];
  const isDisabled = disabled || loading;

  return (
    <Pressable
      onPress={onPress}
      disabled={isDisabled}
      accessibilityRole="button"
      accessibilityLabel={title}
      style={{
        backgroundColor: s.bg,
        borderRadius: radius.button,
        paddingVertical: spacing.md,
        paddingHorizontal: spacing.lg,
        borderWidth: s.border ? 1 : 0,
        borderColor: s.border,
        alignItems: "center",
        opacity: isDisabled ? 0.5 : 1,
      }}
    >
      {loading ? <ActivityIndicator color={s.text} /> : <ThemedText style={{ color: s.text, fontFamily: "Vazirmatn-Medium" }}>{title}</ThemedText>}
    </Pressable>
  );
}
