import { Result } from "antd";
import { SOON_LABELS } from "@/lib/nav";

const REASONS: Record<string, string> = {
  matches:
    "مسابقات و بلیط فوتبال کاملاً داخل سامانه‌ی جدای Django مدیریت می‌شوند (بدون هیچ داده‌ای در این Backend) -- طبق ADR-0004/ADR-0006. یکپارچه‌سازی این‌جا موضوع تصمیم آینده است.",
  integrations:
    "هیچ ماژول integrations یا Adapter واقعی هنوز ساخته نشده -- منتظر تصمیم روی یک Provider واقعی برای هر حوزه.",
  "audit-logs":
    "هیچ ماژول Audit Log واقعی هنوز ساخته نشده (نه فقط بدون Provider -- اصلاً بدون Backend). موضوع یک فاز جدا خواهد بود.",
};

const DEFAULT_REASON =
  "این حوزه هنوز Provider واقعی/تأییدشده‌ای ندارد؛ طبق تصمیم صریح کارفرما (ADR-0015)، ساختن حتی یک رابط خالی بدون API واقعی، حدس‌زدن API است. وقتی یک Provider واقعی انتخاب شود، این بخش موضوع فاز خودش خواهد بود.";

export default async function ComingSoonPage({ params }: PageProps<"/soon/[key]">) {
  const { key } = await params;
  const label = SOON_LABELS[key] ?? key;

  return <Result status="info" title={`${label} -- به‌زودی`} subTitle={REASONS[key] ?? DEFAULT_REASON} />;
}
