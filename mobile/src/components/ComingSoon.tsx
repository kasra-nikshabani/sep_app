import { View } from "react-native";
import { spacing, palette } from "@/theme";
import { ThemedText } from "./ThemedText";
import { Pill } from "./Pill";
import { GiftIcon, type IconComponent } from "./Icon";

export function ComingSoon({ title, message, Icon = GiftIcon }: { title: string; message: string; Icon?: IconComponent }) {
  return (
    <View style={{ flex: 1, alignItems: "center", justifyContent: "center", padding: spacing.xxl, gap: spacing.md }}>
      <Icon color={palette.n900} size={40} />
      <ThemedText variant="h2">{title}</ThemedText>
      <View style={{ alignSelf: "center" }}>
        <Pill label="به‌زودی" tone="gold" />
      </View>
      <ThemedText muted style={{ textAlign: "center" }}>
        {message}
      </ThemedText>
    </View>
  );
}
