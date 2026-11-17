import { View, Pressable, Platform, Linking } from "react-native";
import { Link } from "expo-router";
import * as WebBrowser from "expo-web-browser";
import { ThemedText } from "@/components/ThemedText";
import { useTheme } from "@/hooks/useTheme";
import { spacing, radius, palette } from "@/theme";
import type { ServiceItem } from "@/features/services/catalog";

// روی گوشی مرورگر داخل اپ (Custom Tabs / SFSafariViewController)؛ روی وب یک تب تازه --
// openBrowserAsync در وب همیشه یک پنجره‌ی Popup کوچک باز می‌کند.
export function openWebService(url: string) {
  if (Platform.OS === "web") {
    Linking.openURL(url);
    return;
  }
  WebBrowser.openBrowserAsync(url, {
    toolbarColor: palette.n900,
    controlsColor: palette.gold500,
    dismissButtonStyle: "close",
    showTitle: true,
  });
}

// عرض ۲۵٪ بدون فاصله‌ی ستونی (نه ۲۲٪ + gap): داخل Card روی گوشی‌های ۳۶۰px، ۲۲٪ + gap
// از عرض بیشتر می‌شد و ردیف چهارتایی به سه ستون می‌شکست.
const TILE_STYLE = { width: "25%", alignItems: "center", gap: spacing.xs } as const;
const BOX = 60;

export function ServiceTile({ item }: { item: ServiceItem }) {
  const { colors } = useTheme();
  const soon = !!item.soon;

  const body = (
    <>
      <View
        style={{
          width: BOX,
          height: BOX,
          borderRadius: radius.card,
          alignItems: "center",
          justifyContent: "center",
          // فعال: مشکی پررنگ. «به‌زودی»: زمینه‌ی کم‌رنگ + حاشیه، تا از سرویس‌های واقعی جدا دیده شود.
          backgroundColor: soon ? palette.n100 : colors.accent,
          borderWidth: soon ? 1 : 0,
          borderColor: colors.border,
        }}
      >
        <item.Icon color={soon ? palette.n400 : colors.accentContrast} size={30} />
      </View>
      <ThemedText
        variant="caption"
        style={{ textAlign: "center", fontFamily: "Vazirmatn-Medium" }}
        muted={soon}
        numberOfLines={2}
      >
        {item.label}
      </ThemedText>
      {soon ? (
        <View
          style={{
            backgroundColor: palette.n100,
            borderRadius: radius.pill,
            paddingHorizontal: spacing.sm,
            paddingVertical: 1,
          }}
        >
          <ThemedText variant="caption" style={{ fontSize: 12, color: palette.n700 }}>
            به‌زودی
          </ThemedText>
        </View>
      ) : null}
    </>
  );

  const a11yLabel = soon ? `${item.label}، به‌زودی` : item.label;

  if (item.external) {
    return (
      <Pressable
        accessibilityRole="link"
        accessibilityLabel={a11yLabel}
        accessibilityHint="در مرورگر داخل برنامه باز می‌شود"
        onPress={() => openWebService(item.href)}
        style={TILE_STYLE}
      >
        {body}
      </Pressable>
    );
  }

  return (
    <Link href={item.href as never} asChild>
      <Pressable accessibilityRole="button" accessibilityLabel={a11yLabel} style={TILE_STYLE}>
        {body}
      </Pressable>
    </Link>
  );
}

/** میان‌بُر بزرگ بالای صفحه‌ی خانه برای پرکاربردترین خدمات. */
export function ShortcutButton({ item }: { item: ServiceItem }) {
  const { colors } = useTheme();
  const content = (
    <View
      style={{
        flex: 1,
        flexDirection: "row",
        alignItems: "center",
        justifyContent: "center",
        gap: spacing.sm,
        backgroundColor: colors.accent,
        borderRadius: radius.button,
        paddingVertical: spacing.md,
        minHeight: 48,
      }}
    >
      <item.Icon color={palette.gold500} size={22} />
      <ThemedText variant="caption" color={colors.accentContrast} style={{ fontFamily: "Vazirmatn-Black" }}>
        {item.label}
      </ThemedText>
    </View>
  );

  if (item.external) {
    return (
      <Pressable
        accessibilityRole="link"
        accessibilityLabel={item.label}
        onPress={() => openWebService(item.href)}
        style={{ flex: 1 }}
      >
        {content}
      </Pressable>
    );
  }
  return (
    <Link href={item.href as never} asChild>
      <Pressable accessibilityRole="button" accessibilityLabel={item.label} style={{ flex: 1 }}>
        {content}
      </Pressable>
    </Link>
  );
}
