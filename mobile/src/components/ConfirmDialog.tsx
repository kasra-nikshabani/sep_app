import { Modal, Pressable, View } from "react-native";
import { useTheme } from "@/hooks/useTheme";
import { palette, radius, spacing } from "@/theme";
import { ThemedText } from "./ThemedText";
import { Button } from "./Button";

// Alert.alert با دکمه در react-native-web پیاده نشده (روی Web هیچ کاری نمی‌کند) -- پس برای
// کارهای برگشت‌ناپذیر (خرج امتیاز، رزرو صندلی) یک Modal دست‌ساز که روی هر سه پلتفرم یکسان است.
export function ConfirmDialog({
  visible,
  title,
  message,
  confirmTitle,
  loading = false,
  error,
  onConfirm,
  onCancel,
}: {
  visible: boolean;
  title: string;
  message: string;
  confirmTitle: string;
  loading?: boolean;
  error?: string;
  onConfirm: () => void;
  onCancel: () => void;
}) {
  const { colors } = useTheme();

  return (
    <Modal visible={visible} transparent animationType="fade" onRequestClose={loading ? undefined : onCancel}>
      <Pressable
        onPress={loading ? undefined : onCancel}
        style={{ flex: 1, backgroundColor: "rgba(20,17,10,0.55)", justifyContent: "center", padding: spacing.xl }}
      >
        <Pressable
          onPress={(e) => e.stopPropagation()}
          accessibilityRole="alert"
          style={{ backgroundColor: colors.surface, borderRadius: radius.card, padding: spacing.xl, gap: spacing.md }}
        >
          <ThemedText variant="h2" style={{ fontSize: 19 }}>
            {title}
          </ThemedText>
          <ThemedText muted>{message}</ThemedText>
          {error ? <ThemedText color={palette.danger}>{error}</ThemedText> : null}
          <View style={{ gap: spacing.sm, marginTop: spacing.sm }}>
            <Button title={confirmTitle} variant="gold" loading={loading} onPress={onConfirm} />
            <Button title="انصراف" variant="ghost" disabled={loading} onPress={onCancel} />
          </View>
        </Pressable>
      </Pressable>
    </Modal>
  );
}
