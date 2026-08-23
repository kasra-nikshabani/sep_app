import { Text, type TextProps } from "react-native";
import { useTheme } from "@/hooks/useTheme";
import { typography } from "@/theme";

type Variant = keyof typeof typography;

export function ThemedText({
  variant = "body",
  muted = false,
  color,
  style,
  ...rest
}: TextProps & { variant?: Variant; muted?: boolean; color?: string }) {
  const { colors } = useTheme();
  return (
    <Text
      style={[typography[variant], { color: color ?? (muted ? colors.textMuted : colors.text) }, style]}
      {...rest}
    />
  );
}
