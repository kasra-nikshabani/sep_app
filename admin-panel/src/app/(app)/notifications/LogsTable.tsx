"use client";

import { Table, Tag } from "antd";
import { formatDateTime } from "@/lib/format";

export type NotificationLog = {
  id: string;
  channel: "sms" | "email" | "push";
  recipient: string;
  subject: string | null;
  status: "sent" | "failed";
  providerName: string;
  errorMessage: string | null;
  createdAt: string;
};

const channelLabel: Record<NotificationLog["channel"], string> = {
  sms: "پیامک",
  email: "ایمیل",
  push: "Push",
};

export function LogsTable({ data }: { data: NotificationLog[] }) {
  return (
    <Table
      rowKey="id"
      dataSource={data}
      columns={[
        { title: "کانال", dataIndex: "channel", render: (v: NotificationLog["channel"]) => channelLabel[v] },
        { title: "گیرنده", dataIndex: "recipient" },
        { title: "موضوع", dataIndex: "subject", render: (v) => v ?? "—" },
        { title: "Provider", dataIndex: "providerName" },
        {
          title: "وضعیت",
          dataIndex: "status",
          render: (v: NotificationLog["status"], record) =>
            v === "sent" ? (
              <Tag color="success">موفق</Tag>
            ) : (
              <Tag color="error" title={record.errorMessage ?? undefined}>
                ناموفق
              </Tag>
            ),
          filters: [
            { text: "موفق", value: "sent" },
            { text: "ناموفق", value: "failed" },
          ],
          onFilter: (value, record) => record.status === value,
        },
        {
          title: "تاریخ",
          dataIndex: "createdAt",
          render: (v) => formatDateTime(v),
          sorter: (a, b) => new Date(a.createdAt).getTime() - new Date(b.createdAt).getTime(),
          defaultSortOrder: "descend",
        },
      ]}
      pagination={{ pageSize: 20 }}
    />
  );
}
