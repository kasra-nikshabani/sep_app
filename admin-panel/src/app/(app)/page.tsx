import { Card, Col, Row, Statistic, Alert } from "antd";
import { backendFetch } from "@/lib/backend";

async function countOrZero(path: string): Promise<number | null> {
  try {
    const list = await backendFetch<unknown[]>(path);
    return list.length;
  } catch {
    return null;
  }
}

export default async function DashboardPage() {
  const [users, articles, orders, redemptions, notifications, partners, payments] = await Promise.all([
    countOrZero("/api/v1/users/admin"),
    countOrZero("/api/v1/news/admin/articles"),
    countOrZero("/api/v1/shop/admin/orders"),
    countOrZero("/api/v1/loyalty/admin/redemptions"),
    countOrZero("/api/v1/notifications/admin/logs"),
    countOrZero("/api/v1/partners/admin"),
    countOrZero("/api/v1/payments/admin"),
  ]);

  const cards: { title: string; value: number | null }[] = [
    { title: "کاربران/هواداران", value: users },
    { title: "اخبار", value: articles },
    { title: "سفارش‌های فروشگاه", value: orders },
    { title: "درخواست جایزه‌ی امتیازی", value: redemptions },
    { title: "Notification ثبت‌شده", value: notifications },
    { title: "پارتنر", value: partners },
    { title: "پرداخت ثبت‌شده", value: payments },
  ];

  const anyFailed = cards.some((c) => c.value === null);

  return (
    <>
      <h2 style={{ marginBottom: 16 }}>داشبورد</h2>
      {anyFailed && (
        <Alert
          style={{ marginBottom: 16 }}
          type="warning"
          showIcon
          title="برخی آمارها در دسترس نبودند"
          description="Backend برای آن بخش پاسخ نداد -- سایر بخش‌های پنل مستقل کار می‌کنند."
        />
      )}
      <Row gutter={[16, 16]}>
        {cards.map((c) => (
          <Col xs={24} sm={12} md={8} lg={6} key={c.title}>
            <Card>
              <Statistic title={c.title} value={c.value ?? "—"} />
            </Card>
          </Col>
        ))}
      </Row>
    </>
  );
}
