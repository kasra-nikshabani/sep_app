import localFont from "next/font/local";

// همان فونت Vazirmatn استفاده‌شده در docs/architecture/ui-ux/design-system.html (v0.6)،
// از حالت Base64 داخل آن فایل استخراج و این‌جا Self-host شده -- بدون وابستگی به CDN خارجی.
export const vazirmatn = localFont({
  src: [
    { path: "./Vazirmatn-Regular.woff2", weight: "400", style: "normal" },
    { path: "./Vazirmatn-Medium.woff2", weight: "500", style: "normal" },
    { path: "./Vazirmatn-Black.woff2", weight: "900", style: "normal" },
  ],
  variable: "--font-vazirmatn",
  display: "swap",
});
