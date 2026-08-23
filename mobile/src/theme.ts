// پالت و تایپوگرافی مستقیماً از docs/architecture/ui-ux/design-system.html (v0.6)
// -- همان مرجعی که Admin Panel (Phase 14) هم از آن استفاده کرد.

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

export type ThemeColors = {
  bg: string;
  surface: string;
  surfaceRaised: string;
  text: string;
  textMuted: string;
  border: string;
  accent: string;
  accentContrast: string;
  gold: string;
  goldText: string;
};

export const lightColors: ThemeColors = {
  bg: palette.n50,
  surface: palette.n0,
  surfaceRaised: palette.n0,
  text: palette.n900,
  textMuted: palette.n700,
  border: palette.n200,
  accent: palette.n900,
  accentContrast: palette.n0,
  gold: palette.gold500,
  goldText: palette.gold700,
};

export const darkColors: ThemeColors = {
  bg: palette.n900,
  surface: "#1C170D",
  surfaceRaised: "#241D10",
  text: "#F6EFDD",
  textMuted: "#CBBFA0",
  border: "#3A311D",
  accent: palette.gold500,
  accentContrast: palette.n900,
  gold: palette.gold500,
  goldText: palette.gold300,
};

export const spacing = { xs: 4, sm: 8, md: 12, lg: 16, xl: 24, xxl: 32, xxxl: 48, huge: 64 };

export const radius = { field: 9, button: 10, card: 14, pill: 999 };

export const typography = {
  display: { fontFamily: "Vazirmatn-Black", fontSize: 40 },
  h1: { fontFamily: "Vazirmatn-Black", fontSize: 28 },
  h2: { fontFamily: "Vazirmatn-Medium", fontSize: 21 },
  body: { fontFamily: "Vazirmatn-Regular", fontSize: 15.5 },
  caption: { fontFamily: "Vazirmatn-Medium", fontSize: 12.5 },
  numeric: { fontFamily: "Vazirmatn-Medium", fontSize: 19 },
};

export const statusMeta = {
  success: { color: palette.success, bg: palette.successBg },
  warning: { color: palette.warning, bg: palette.warningBg },
  danger: { color: palette.danger, bg: palette.dangerBg },
  info: { color: palette.info, bg: palette.infoBg },
  gold: { color: palette.gold700, bg: "#FBF1D9" },
};
