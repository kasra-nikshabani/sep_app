import { ScrollView, View, type ViewProps } from "react-native";
import { SafeAreaView } from "react-native-safe-area-context";
import { useTheme } from "@/hooks/useTheme";
import { spacing } from "@/theme";

export function Screen({ children, scroll = true }: { children: React.ReactNode; scroll?: boolean }) {
  const { colors } = useTheme();
  const Container = scroll ? ScrollView : View;
  return (
    <SafeAreaView style={{ flex: 1, backgroundColor: colors.bg }}>
      <Container
        style={{ flex: 1 }}
        contentContainerStyle={scroll ? { padding: spacing.lg, gap: spacing.lg } : undefined}
      >
        {children}
      </Container>
    </SafeAreaView>
  );
}

export function Card({ children, style }: ViewProps) {
  const { colors } = useTheme();
  return (
    <View
      style={[
        {
          backgroundColor: colors.surface,
          borderRadius: 14,
          padding: spacing.lg,
          borderWidth: 1,
          borderColor: colors.border,
          gap: spacing.sm,
        },
        style,
      ]}
    >
      {children}
    </View>
  );
}
