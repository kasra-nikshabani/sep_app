import { useColorScheme } from "react-native";
import { darkColors, lightColors, type ThemeColors } from "@/theme";

export function useTheme(): { colors: ThemeColors; dark: boolean } {
  const scheme = useColorScheme();
  const dark = scheme === "dark";
  return { colors: dark ? darkColors : lightColors, dark };
}
