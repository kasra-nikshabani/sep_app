import { View, Image } from "react-native";
import { ThemedText } from "./ThemedText";
import { spacing } from "@/theme";

export function BrandMark({ large = false }: { large?: boolean }) {
  const logoSize = large ? 96 : 56;
  return (
    <View style={{ alignItems: "center", gap: large ? spacing.sm : spacing.xs }}>
      <Image source={require("../../assets/sepahan-logo.png")} style={{ width: logoSize, height: logoSize }} />
      <ThemedText style={{ fontFamily: "Vazirmatn-Black", fontSize: large ? 22 : 15, textAlign: "center" }}>
        باشگاه فولاد مبارکه سپاهان
      </ThemedText>
    </View>
  );
}
