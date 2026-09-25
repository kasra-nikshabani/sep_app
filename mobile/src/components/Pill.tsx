import { View } from "react-native";
import { radius, spacing, statusMeta } from "@/theme";
import { ThemedText } from "./ThemedText";

export function Pill({ label, tone = "info" }: { label: string; tone?: keyof typeof statusMeta }) {
  const meta = statusMeta[tone];
  return (
    <View
      style={{
        flexDirection: "row",
        alignItems: "center",
        gap: 6,
        alignSelf: "flex-start",
        backgroundColor: meta.bg,
        borderRadius: radius.pill,
        paddingVertical: 5,
        paddingHorizontal: spacing.md,
      }}
    >
      <View style={{ width: 6, height: 6, borderRadius: 3, backgroundColor: meta.color }} />
      <ThemedText variant="caption" color={meta.color}>
        {label}
      </ThemedText>
    </View>
  );
}
