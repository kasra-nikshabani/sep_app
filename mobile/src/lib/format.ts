const dateFormatter = new Intl.DateTimeFormat("fa-IR-u-ca-persian", {
  dateStyle: "medium",
  timeZone: "Asia/Tehran",
});

const dateTimeFormatter = new Intl.DateTimeFormat("fa-IR-u-ca-persian", {
  dateStyle: "medium",
  timeStyle: "short",
  timeZone: "Asia/Tehran",
});

export function formatDate(value: string | null | undefined): string {
  if (!value) return "—";
  return dateFormatter.format(new Date(value));
}

export function formatDateTime(value: string | null | undefined): string {
  if (!value) return "—";
  return dateTimeFormatter.format(new Date(value));
}

export function formatRial(value: string | number | null | undefined): string {
  if (value === null || value === undefined) return "—";
  return `${Number(value).toLocaleString("fa-IR")} ریال`;
}

const PERSIAN_DIGITS = "۰۱۲۳۴۵۶۷۸۹";

// متن‌های اپ فارسی‌اند -- تاریخ/مبلغ از قبل با fa-IR ارقام فارسی داشتند، ولی امتیاز، تعداد،
// موبایل، کد ملی و شماره‌ی صندلی با رقم لاتین نمایش داده می‌شدند (ناهمگون در یک صفحه).
export function toPersianDigits(value: string | number | null | undefined): string {
  if (value === null || value === undefined) return "—";
  return String(value).replace(/[0-9]/g, (d) => PERSIAN_DIGITS[Number(d)]);
}

// برعکسِ toPersianDigits -- ورودی کاربر با کیبورد فارسی (مثلاً موبایل در Checkout) پیش از
// ارسال به Backend به رقم لاتین برگردانده می‌شود.
export function toLatinDigits(value: string): string {
  return value.replace(/[۰-۹]/g, (d) => String(PERSIAN_DIGITS.indexOf(d))).replace(/[٠-٩]/g, (d) => String(d.charCodeAt(0) - 0x0660));
}

export function formatNumber(value: number | null | undefined): string {
  if (value === null || value === undefined) return "—";
  return value.toLocaleString("fa-IR");
}
