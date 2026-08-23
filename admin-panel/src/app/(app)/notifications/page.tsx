import { Card, Col, Row } from "antd";
import { backendFetch } from "@/lib/backend";
import { SendForms } from "./SendForms";
import { LogsTable, type NotificationLog } from "./LogsTable";

export default async function NotificationsPage() {
  const logs = await backendFetch<NotificationLog[]>("/api/v1/notifications/admin/logs");
  return (
    <>
      <h2 style={{ marginBottom: 16 }}>اعلان‌ها</h2>
      <Row gutter={16} style={{ marginBottom: 24 }}>
        <Col span={24}>
          <Card title="ارسال دستی/آزمایشی">
            <SendForms />
          </Card>
        </Col>
      </Row>
      <h3 style={{ marginBottom: 12 }}>دفترکل</h3>
      <LogsTable data={logs} />
    </>
  );
}
