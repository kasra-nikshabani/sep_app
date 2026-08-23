"use client";

import { useActionState, useEffect, useState } from "react";
import { Table, Tag, Button, Modal, Form, Input, Alert, Typography } from "antd";
import { PlusOutlined } from "@ant-design/icons";
import { createPartnerAction, type CreatePartnerState } from "./actions";

export type Partner = {
  id: string;
  name: string;
  slug: string;
  contactEmail: string | null;
  contactPhone: string | null;
  active: boolean;
};

const initialState: CreatePartnerState = {};

export function PartnersTable({ data }: { data: Partner[] }) {
  const [open, setOpen] = useState(false);
  const [state, formAction, isPending] = useActionState(createPartnerAction, initialState);

  useEffect(() => {
    if (state.created) {
      // فرم مخفی می‌شود، اما Modal باز می‌ماند تا کلید یک‌بارمصرف نمایش داده شود
    }
  }, [state.created]);

  return (
    <>
      <Button type="primary" icon={<PlusOutlined />} onClick={() => setOpen(true)} style={{ marginBottom: 16 }}>
        Partner جدید
      </Button>

      <Table
        rowKey="id"
        dataSource={data}
        columns={[
          { title: "نام", dataIndex: "name" },
          { title: "Slug", dataIndex: "slug" },
          { title: "ایمیل", dataIndex: "contactEmail", render: (v) => v ?? "—" },
          { title: "موبایل", dataIndex: "contactPhone", render: (v) => v ?? "—" },
          {
            title: "وضعیت",
            dataIndex: "active",
            render: (v: boolean) => <Tag color={v ? "success" : "default"}>{v ? "فعال" : "غیرفعال"}</Tag>,
          },
        ]}
        pagination={{ pageSize: 20 }}
      />

      <Modal
        title="ساخت Partner جدید"
        open={open}
        onCancel={() => {
          setOpen(false);
        }}
        footer={null}
        destroyOnHidden
      >
        {state.created ? (
          <>
            <Alert
              type="warning"
              showIcon
              title="این کلید فقط همین یک‌بار نمایش داده می‌شود"
              description="آن را همین حالا در جایی امن ذخیره کنید -- بعد از بستن این پنجره دیگر قابل بازیابی نیست."
              style={{ marginBottom: 16 }}
            />
            <Typography.Paragraph copyable={{ text: state.created.apiKey }} code style={{ wordBreak: "break-all" }}>
              {state.created.apiKey}
            </Typography.Paragraph>
            <Button type="primary" block onClick={() => setOpen(false)}>
              بستن
            </Button>
          </>
        ) : (
          <form action={formAction}>
            {state.error && <Alert type="error" showIcon title={state.error} style={{ marginBottom: 16 }} />}
            <Form.Item label="نام" required>
              <Input name="name" placeholder="مثلاً فروشگاه همکار الف" />
            </Form.Item>
            <Form.Item label="Slug" required>
              <Input name="slug" placeholder="partner-a" dir="ltr" />
            </Form.Item>
            <Form.Item label="ایمیل تماس">
              <Input name="contactEmail" dir="ltr" />
            </Form.Item>
            <Form.Item label="موبایل تماس">
              <Input name="contactPhone" dir="ltr" />
            </Form.Item>
            <Button type="primary" htmlType="submit" block loading={isPending}>
              ساخت
            </Button>
          </form>
        )}
      </Modal>
    </>
  );
}
