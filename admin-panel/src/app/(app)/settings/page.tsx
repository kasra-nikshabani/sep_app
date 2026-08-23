import { Card, Descriptions, Tag, Space } from "antd";
import { auth } from "@/auth";

export default async function SettingsPage() {
  const session = await auth();

  return (
    <>
      <h2 style={{ marginBottom: 16 }}>تنظیمات</h2>
      <Card title="حساب من" style={{ marginBottom: 16 }}>
        <Descriptions column={1} bordered size="small">
          <Descriptions.Item label="نام">{session?.user?.name ?? "—"}</Descriptions.Item>
          <Descriptions.Item label="ایمیل">{session?.user?.email ?? "—"}</Descriptions.Item>
          <Descriptions.Item label="نقش‌ها">
            <Space wrap>
              {(session?.roles ?? []).map((r) => (
                <Tag key={r} color={r === "admin" ? "gold" : "default"}>
                  {r}
                </Tag>
              ))}
            </Space>
          </Descriptions.Item>
        </Descriptions>
      </Card>
      <Card title="درباره">
        <p>پنل مدیریت — باشگاه فولاد مبارکه سپاهان (Phase 14)</p>
        <p>احراز هویت: Keycloak (Authorization Code Flow، Confidential Client -- ADR-0003)</p>
      </Card>
    </>
  );
}
