import { theme, type ThemeConfig } from "antd";

// برگرفته از پالت رسمی docs/architecture/ui-ux/design-system.html (v0.6) --
// مشکی/طلایی هویت باشگاه. رنگ‌های Status عمداً از پالت هویتی جدا نگه داشته شده‌اند.
export const palette = {
  gold700: "#9C7209",
  gold500: "#E8A80E",
  gold300: "#F3C765",
  n0: "#FFFFFF",
  n50: "#FAF8F2",
  n100: "#F1ECDD",
  n200: "#E3DAC0",
  n300: "#CFC29B",
  n400: "#AB9968",
  n600: "#6E6142",
  n700: "#4D4530",
  n800: "#2B2515",
  n900: "#14110A",
  success: "#2E7D4F",
  successBg: "#E4F3EA",
  warning: "#A8641D",
  warningBg: "#FBEEDC",
  danger: "#B23328",
  dangerBg: "#FBE7E4",
  info: "#33608F",
  infoBg: "#E4EDF5",
} as const;

const fontFamily = "var(--font-vazirmatn), Tahoma, Arial, sans-serif";

export const lightTheme: ThemeConfig = {
  token: {
    fontFamily,
    colorPrimary: palette.n900,
    colorLink: palette.gold700,
    colorSuccess: palette.success,
    colorWarning: palette.warning,
    colorError: palette.danger,
    colorInfo: palette.info,
    colorBgLayout: palette.n50,
    colorBgContainer: palette.n0,
    colorBgElevated: palette.n0,
    colorText: palette.n900,
    colorTextSecondary: palette.n700,
    colorBorder: palette.n200,
    borderRadius: 10,
  },
  components: {
    Layout: { siderBg: palette.n900, headerBg: palette.n0 },
    Menu: { darkItemBg: palette.n900, darkItemSelectedBg: palette.gold500 },
    Card: { borderRadiusLG: 14 },
  },
};

export const darkTheme: ThemeConfig = {
  algorithm: theme.darkAlgorithm,
  token: {
    fontFamily,
    colorPrimary: palette.gold500,
    colorLink: palette.gold300,
    colorSuccess: palette.success,
    colorWarning: palette.warning,
    colorError: palette.danger,
    colorInfo: palette.info,
    colorBgLayout: palette.n900,
    colorBgContainer: "#1C170D",
    colorBgElevated: "#241D10",
    colorText: "#F6EFDD",
    colorTextSecondary: "#CBBFA0",
    colorBorder: "#3A311D",
    borderRadius: 10,
  },
  components: {
    Layout: { siderBg: "#1C170D", headerBg: "#1C170D" },
    Card: { borderRadiusLG: 14 },
  },
};
