import { Modal, Pressable, View } from "react-native";
import { Link } from "expo-router";
import { LinearGradient } from "expo-linear-gradient";
import { useTheme } from "@/hooks/useTheme";
import { palette, radius, spacing } from "@/theme";
import { ThemedText } from "./ThemedText";
import { ArrowIcon, type IconComponent } from "./Icon";

export type ServiceMenuOption = { label: string; href: string; soon?: boolean };

export function ServiceMenuSheet({
  visible,
  title,
  Icon,
  options,
  onClose,
}: {
  visible: boolean;
  title: string;
  Icon: IconComponent;
  options: ServiceMenuOption[];
  onClose: () => void;
}) {
  const { colors } = useTheme();

  return (
    <Modal visible={visible} transparent animationType="slide" onRequestClose={onClose}>
      <Pressable
        onPress={onClose}
        style={{ flex: 1, backgroundColor: "rgba(20,17,10,0.55)", justifyContent: "flex-end" }}
      >
        <Pressable
          onPress={(e) => e.stopPropagation()}
          style={{
            backgroundColor: colors.surface,
            borderTopLeftRadius: radius.card * 1.8,
            borderTopRightRadius: radius.card * 1.8,
            padding: spacing.lg,
            paddingTop: spacing.sm,
            gap: spacing.lg,
            shadowColor: "#000",
            shadowOffset: { width: 0, height: -4 },
            shadowOpacity: 0.15,
            shadowRadius: 16,
            elevation: 12,
          }}
        >
          <View
            style={{
              width: 40,
              height: 4,
              borderRadius: 2,
              backgroundColor: colors.border,
              alignSelf: "center",
            }}
          />

          <View style={{ flexDirection: "row-reverse", alignItems: "center", gap: spacing.md }}>
            <LinearGradient
              colors={[palette.gold300, palette.gold500, palette.gold700]}
              start={{ x: 0, y: 0 }}
              end={{ x: 1, y: 1 }}
              style={{
                width: 48,
                height: 48,
                borderRadius: radius.card,
                alignItems: "center",
                justifyContent: "center",
              }}
            >
              <Icon color={palette.n900} size={24} />
            </LinearGradient>
            <ThemedText variant="h2" style={{ fontSize: 19 }}>
              {title}
            </ThemedText>
          </View>

          <View style={{ gap: spacing.sm }}>
            {options.map((option) => (
              <Link key={option.href} href={option.href as never} asChild>
                <Pressable
                  onPress={onClose}
                  style={{
                    flexDirection: "row-reverse",
                    alignItems: "center",
                    justifyContent: "space-between",
                    paddingVertical: spacing.md,
                    paddingHorizontal: spacing.lg,
                    borderRadius: radius.card,
                    backgroundColor: colors.bg,
                    borderWidth: 1,
                    borderColor: option.soon ? colors.border : palette.gold500,
                  }}
                >
                  <ThemedText style={{ fontSize: 15.5, fontFamily: "Vazirmatn-Medium" }}>{option.label}</ThemedText>
                  {option.soon ? (
                    <View
                      style={{
                        borderWidth: 1,
                        borderColor: palette.n900,
                        borderRadius: radius.pill,
                        paddingVertical: 4,
                        paddingHorizontal: spacing.sm,
                      }}
                    >
                      <ThemedText variant="caption" style={{ color: palette.n900 }}>
                        به‌زودی
                      </ThemedText>
                    </View>
                  ) : (
                    <View
                      style={{
                        width: 28,
                        height: 28,
                        borderRadius: 14,
                        backgroundColor: palette.n900,
                        alignItems: "center",
                        justifyContent: "center",
                      }}
                    >
                      <ArrowIcon color={palette.n0} size={14} />
                    </View>
                  )}
                </Pressable>
              </Link>
            ))}
          </View>
        </Pressable>
      </Pressable>
    </Modal>
  );
}
