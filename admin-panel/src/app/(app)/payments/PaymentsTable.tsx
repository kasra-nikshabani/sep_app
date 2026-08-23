"use client";

import { Table, Tag, Alert } from "antd";
import { formatDateTime, formatRial } from "@/lib/format";

export type AdminPayment = {
  id: string;
  purpose: "ticket_purchase" | "wallet_topup" | "shop_order";
  referenceId: string;
  userId: string;
  provider: string;
  trackId: string | null;
  amount: string;
  status: "pending" | "paid" | "failed";
  createdAt: string;
  updatedAt: string;
};

const purposeLabel: Record<AdminPayment["purpose"], string> = {
  ticket_purchase: "بلیط تئاتر",
  wallet_topup: "شارژ کیف‌پول",
  shop_order: "سفارش فروشگاه",
};

const statusMeta: Record<AdminPayment["status"], { color: string; label: string }> = {
  pending: { color: "warning", label: "در انتظار" },
  paid: { color: "success", label: "پرداخت‌شده" },
  failed: { color: "error", label: "ناموفق" },
};

export function PaymentsTable({ data }: { data: AdminPayment[] }) {
  return (
    <>
      <Alert
        style={{ marginBottom: 16 }}
        type="info"
        showIcon
        title="فقط مشاهده و تطبیق (Reconciliation)"
        description="زیبال API استرداد رسمی ندارد -- هیچ عملیات مالی از این صفحه انجام نمی‌شود."
      />
      <Table
        rowKey="id"
        dataSource={data}
        columns={[
          { title: "هدف", dataIndex: "purpose", render: (v: AdminPayment["purpose"]) => purposeLabel[v] },
          { title: "Provider", dataIndex: "provider" },
          { title: "Track ID", dataIndex: "trackId", render: (v) => v ?? "—" },
          { title: "مبلغ", dataIndex: "amount", render: (v) => formatRial(v) },
          {
            title: "وضعیت",
            dataIndex: "status",
            render: (v: AdminPayment["status"]) => <Tag color={statusMeta[v].color}>{statusMeta[v].label}</Tag>,
            filters: Object.entries(statusMeta).map(([value, m]) => ({ text: m.label, value })),
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
        pagination={{ pageSize: 20, showSizeChanger: true }}
      />
    </>
  );
}
