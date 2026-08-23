import { View } from "react-native";
import { useTheme } from "@/hooks/useTheme";
import { spacing } from "@/theme";
import { ThemedText } from "./ThemedText";
import { GiftIcon } from "./Icon";

export function ComingSoon({ title, reason }: { title: string; reason: string }) {
  const { colors } = useTheme();
  return (
    <View style={{ flex: 1, alignItems: "center", justifyContent: "center", padding: spacing.xxl, gap: spacing.md }}>
      <GiftIcon color={colors.textMuted} size={40} />
      <ThemedText variant="h2">{title} -- به‌زودی</ThemedText>
      <ThemedText muted style={{ textAlign: "center" }}>
        {reason}
      </ThemedText>
    </View>
  );
}
