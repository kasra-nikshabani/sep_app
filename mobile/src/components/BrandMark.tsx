import { View, Image } from "react-native";
import { ThemedText } from "./ThemedText";
import { spacing } from "@/theme";

export function BrandMark() {
  return (
    <View style={{ alignItems: "center", gap: spacing.xs }}>
      <Image source={require("../../assets/sepahan-logo.png")} style={{ width: 56, height: 56 }} />
      <ThemedText style={{ fontFamily: "Vazirmatn-Black", fontSize: 15, textAlign: "center" }}>
        باشگاه فولاد مبارکه سپاهان
      </ThemedText>
    </View>
  );
}
