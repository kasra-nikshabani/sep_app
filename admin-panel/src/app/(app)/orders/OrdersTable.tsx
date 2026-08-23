"use client";

import { useTransition } from "react";
import { Table, Tag, Button, Space, Popconfirm } from "antd";
import { formatDateTime, formatRial } from "@/lib/format";
import { shipOrderAction, deliverOrderAction } from "./actions";

export type OrderItem = { productName: string; sku: string; unitPrice: string; quantity: number; subtotal: string };
type OrderStatusType =
  | "pending_payment"
  | "paid"
  | "shipped"
  | "delivered"
  | "cancelled"
  | "expired"
  | "return_requested"
  | "returned";

export type Order = {
  id: string;
  status: OrderStatusType;
  totalAmount: string;
  shippingMethodName: string;
  shippingRecipientName: string;
  shippingCity: string;
  requiresManualReview: boolean;
  createdAt: string;
  items: OrderItem[];
};

const statusMeta: Record<OrderStatusType, { color: string; label: string }> = {
  pending_payment: { color: "default", label: "در انتظار پرداخت" },
  paid: { color: "processing", label: "پرداخت‌شده" },
  shipped: { color: "cyan", label: "ارسال‌شده" },
  delivered: { color: "success", label: "تحویل‌شده" },
  cancelled: { color: "default", label: "لغوشده" },
  expired: { color: "default", label: "منقضی" },
  return_requested: { color: "warning", label: "درخواست مرجوعی" },
  returned: { color: "default", label: "مرجوع‌شده" },
};

export function OrdersTable({ data }: { data: Order[] }) {
  const [isPending, startTransition] = useTransition();

  return (
    <Table
      rowKey="id"
      dataSource={data}
      expandable={{
        expandedRowRender: (order) => (
          <Table
            size="small"
            rowKey="sku"
            dataSource={order.items}
            pagination={false}
            columns={[
              { title: "محصول", dataIndex: "productName" },
              { title: "SKU", dataIndex: "sku" },
              { title: "تعداد", dataIndex: "quantity" },
              { title: "جمع", dataIndex: "subtotal", render: (v) => formatRial(v) },
            ]}
          />
        ),
      }}
      columns={[
        { title: "گیرنده", dataIndex: "shippingRecipientName" },
        { title: "شهر", dataIndex: "shippingCity" },
        { title: "روش ارسال", dataIndex: "shippingMethodName" },
        { title: "مبلغ کل", dataIndex: "totalAmount", render: (v) => formatRial(v) },
        {
          title: "وضعیت",
          dataIndex: "status",
          render: (v: OrderStatusType, record: Order) => (
            <Space orientation="vertical" size={2}>
              <Tag color={statusMeta[v].color}>{statusMeta[v].label}</Tag>
              {record.requiresManualReview && <Tag color="red">نیازمند بررسی دستی</Tag>}
            </Space>
          ),
          filters: Object.entries(statusMeta).map(([value, m]) => ({ text: m.label, value })),
          onFilter: (value, record) => record.status === value,
        },
        { title: "تاریخ", dataIndex: "createdAt", render: (v) => formatDateTime(v) },
        {
          title: "عملیات",
          render: (_, record: Order) => (
            <Space>
              {record.status === "paid" && (
                <Popconfirm title="این سفارش ارسال شود؟" onConfirm={() => startTransition(() => { shipOrderAction(record.id); })}>
                  <Button size="small" loading={isPending}>
                    ارسال
                  </Button>
                </Popconfirm>
              )}
              {record.status === "shipped" && (
                <Popconfirm title="تحویل این سفارش ثبت شود؟" onConfirm={() => startTransition(() => { deliverOrderAction(record.id); })}>
                  <Button size="small" type="primary" loading={isPending}>
                    تحویل
                  </Button>
                </Popconfirm>
              )}
            </Space>
          ),
        },
      ]}
      pagination={{ pageSize: 15 }}
    />
  );
}
