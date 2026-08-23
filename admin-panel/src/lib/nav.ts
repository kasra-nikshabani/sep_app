export type NavItem = {
  key: string;
  href: string;
  label: string;
  soon?: boolean;
};

export type NavGroup = {
  key: string;
  label: string;
  items: NavItem[];
};

// طبق بند ۱۸ بریف (فهرست کامل بخش‌های Admin Panel) و تصمیم محدوده‌ی Phase 14:
// فقط بخش‌های دارای Backend واقعی صفحه‌ی کامل دارند؛ بقیه صفحه‌ی «به‌زودی» می‌گیرند
// (طبق همان تصمیمی که در Phase 13/ADR-0015 چهار حوزه‌ی بدون Provider را کنار گذاشت).
export const NAV_GROUPS: NavGroup[] = [
  {
    key: "overview",
    label: "کلی",
    items: [{ key: "dashboard", href: "/", label: "داشبورد" }],
  },
  {
    key: "content",
    label: "محتوا",
    items: [
      { key: "news", href: "/news", label: "اخبار" },
      { key: "matches", href: "/soon/matches", label: "مسابقات فوتبال", soon: true },
    ],
  },
  {
    key: "commerce",
    label: "تجاری",
    items: [
      { key: "shop", href: "/shop", label: "فروشگاه" },
      { key: "orders", href: "/orders", label: "سفارش‌ها" },
      { key: "tickets", href: "/tickets", label: "بلیط (تئاتر)" },
      { key: "payments", href: "/payments", label: "پرداخت‌ها" },
    ],
  },
  {
    key: "engagement",
    label: "تعامل با هوادار",
    items: [
      { key: "loyalty", href: "/loyalty", label: "باشگاه امتیاز" },
      { key: "notifications", href: "/notifications", label: "اعلان‌ها" },
      { key: "users", href: "/users", label: "کاربران و هواداران" },
    ],
  },
  {
    key: "platform",
    label: "پلتفرم",
    items: [
      { key: "partners", href: "/partners", label: "پارتنرها" },
      { key: "integrations", href: "/soon/integrations", label: "یکپارچه‌سازی‌ها", soon: true },
      { key: "insurance", href: "/soon/insurance", label: "بیمه", soon: true },
      { key: "travel", href: "/soon/travel", label: "سفر", soon: true },
      { key: "vehicle", href: "/soon/vehicle", label: "خودرو", soon: true },
      { key: "entertainment", href: "/soon/entertainment", label: "سرگرمی", soon: true },
      { key: "audit-logs", href: "/soon/audit-logs", label: "گزارش ممیزی", soon: true },
    ],
  },
  {
    key: "system",
    label: "سیستم",
    items: [{ key: "settings", href: "/settings", label: "تنظیمات" }],
  },
];

export const SOON_LABELS: Record<string, string> = Object.fromEntries(
  NAV_GROUPS.flatMap((g) => g.items).map((i) => [i.href.replace("/soon/", ""), i.label]),
);
