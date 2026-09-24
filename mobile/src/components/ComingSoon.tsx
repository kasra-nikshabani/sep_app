import { View } from "react-native";
import { spacing, palette } from "@/theme";
import { ThemedText } from "./ThemedText";
import { GiftIcon, type IconComponent } from "./Icon";

export function ComingSoon({ title, reason, Icon = GiftIcon }: { title: string; reason: string; Icon?: IconComponent }) {
  return (
    <View style={{ flex: 1, alignItems: "center", justifyContent: "center", padding: spacing.xxl, gap: spacing.md }}>
      <Icon color={palette.n900} size={40} />
      <ThemedText variant="h2">{title} -- به‌زودی</ThemedText>
      <ThemedText muted style={{ textAlign: "center" }}>
        {reason}
      </ThemedText>
    </View>
  );
}
