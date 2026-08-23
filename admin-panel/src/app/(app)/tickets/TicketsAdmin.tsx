"use client";

import { useActionState, useState } from "react";
import { Tabs, Table, Tag, Card, Form, Input, InputNumber, DatePicker, Button, Alert, Typography, Space } from "antd";
import type { Dayjs } from "dayjs";
import { formatDateTime, formatRial } from "@/lib/format";
import { createVenueAction, addSeatsAction, createEventAction, type CreateVenueState, type ActionState } from "./actions";

export type Event = {
  id: string;
  title: string;
  description: string | null;
  venueName: string;
  startsAt: string;
  durationMinutes: number | null;
  status: "draft" | "published" | "cancelled" | "completed";
  basePrice: string;
};

const statusMeta: Record<Event["status"], { color: string; label: string }> = {
  draft: { color: "default", label: "پیش‌نویس" },
  published: { color: "success", label: "منتشرشده" },
  cancelled: { color: "error", label: "لغوشده" },
  completed: { color: "default", label: "برگزارشده" },
};

function EventsTab({ events }: { events: Event[] }) {
  return (
    <Table
      rowKey="id"
      dataSource={events}
      columns={[
        { title: "عنوان", dataIndex: "title" },
        { title: "سالن", dataIndex: "venueName" },
        { title: "زمان شروع", dataIndex: "startsAt", render: (v) => formatDateTime(v) },
        { title: "قیمت پایه", dataIndex: "basePrice", render: (v) => formatRial(v) },
        {
          title: "وضعیت",
          dataIndex: "status",
          render: (v: Event["status"]) => <Tag color={statusMeta[v].color}>{statusMeta[v].label}</Tag>,
        },
      ]}
      pagination={{ pageSize: 15 }}
    />
  );
}

const venueInitial: CreateVenueState = {};
const initial: ActionState = {};

function SetupTab() {
  const [venueState, venueAction, venuePending] = useActionState(createVenueAction, venueInitial);
  const [seatsState, seatsAction, seatsPending] = useActionState(addSeatsAction, initial);
  const [eventState, eventAction, eventPending] = useActionState(createEventAction, initial);
  const [startsAt, setStartsAt] = useState<Dayjs | null>(null);

  return (
    <Space orientation="vertical" size="large" style={{ width: "100%" }}>
      <Alert
        type="info"
        showIcon
        title="بدون Endpoint فهرست سالن‌ها -- شناسه‌ی سالن بعد از ساخت این‌جا نمایش داده می‌شود؛ آن را برای مرحله‌ی بعد کپی کنید"
      />

      <Card size="small" title="۱) ساخت سالن">
        <form action={venueAction} key={venueState.venueId ?? "0"} style={{ display: "flex", gap: 8, flexWrap: "wrap", alignItems: "flex-end" }}>
          {venueState.error && <Alert type="error" title={venueState.error} style={{ width: "100%", marginBottom: 12 }} />}
          <Form.Item>
            <Input name="name" placeholder="نام سالن" required />
          </Form.Item>
          <Form.Item>
            <Input name="city" placeholder="شهر" />
          </Form.Item>
          <Form.Item>
            <Input name="address" placeholder="آدرس" style={{ width: 260 }} />
          </Form.Item>
          <Button type="primary" htmlType="submit" loading={venuePending}>
            ساخت سالن
          </Button>
        </form>
        {venueState.venueId && (
          <Typography.Paragraph copyable={{ text: venueState.venueId }} code style={{ marginTop: 12 }}>
            {venueState.venueId}
          </Typography.Paragraph>
        )}
      </Card>

      <Card size="small" title="۲) افزودن صندلی به سالن (شبکه‌ی ردیف × صندلی)">
        <form action={seatsAction} key={seatsState.success ? "1" : "0"} style={{ display: "flex", gap: 8, flexWrap: "wrap", alignItems: "flex-end" }}>
          {seatsState.error && <Alert type="error" title={seatsState.error} style={{ width: "100%", marginBottom: 12 }} />}
          {seatsState.success && <Alert type="success" title="صندلی‌ها افزوده شدند" style={{ width: "100%", marginBottom: 12 }} />}
          <Form.Item>
            <Input name="venueId" placeholder="شناسه‌ی سالن" dir="ltr" style={{ width: 280 }} required />
          </Form.Item>
          <Form.Item>
            <Input name="section" placeholder="نام سکو (مثلاً A)" required />
          </Form.Item>
          <Form.Item>
            <InputNumber name="rows" placeholder="تعداد ردیف" min={1} required />
          </Form.Item>
          <Form.Item>
            <InputNumber name="seatsPerRow" placeholder="صندلی در هر ردیف" min={1} required />
          </Form.Item>
          <Button htmlType="submit" loading={seatsPending}>
            افزودن
          </Button>
        </form>
      </Card>

      <Card size="small" title="۳) ساخت رویداد (EventSeat برای همه‌ی صندلی‌های سالن خودکار ساخته می‌شود)">
        <form action={eventAction} key={eventState.success ? "1" : "0"} style={{ display: "flex", gap: 8, flexWrap: "wrap", alignItems: "flex-end" }}>
          {eventState.error && <Alert type="error" title={eventState.error} style={{ width: "100%", marginBottom: 12 }} />}
          {eventState.success && <Alert type="success" title="رویداد ساخته شد" style={{ width: "100%", marginBottom: 12 }} />}
          <input type="hidden" name="startsAt" value={startsAt?.toISOString() ?? ""} />
          <Form.Item>
            <Input name="venueId" placeholder="شناسه‌ی سالن" dir="ltr" style={{ width: 280 }} required />
          </Form.Item>
          <Form.Item>
            <Input name="title" placeholder="عنوان رویداد" required />
          </Form.Item>
          <Form.Item>
            <Input name="description" placeholder="توضیح" style={{ width: 200 }} />
          </Form.Item>
          <Form.Item>
            <DatePicker showTime placeholder="زمان شروع" value={startsAt} onChange={setStartsAt} />
          </Form.Item>
          <Form.Item>
            <InputNumber name="durationMinutes" placeholder="مدت (دقیقه)" min={1} />
          </Form.Item>
          <Form.Item>
            <InputNumber name="basePrice" placeholder="قیمت پایه (ریال)" min={0} required />
          </Form.Item>
          <Button type="primary" htmlType="submit" loading={eventPending}>
            ساخت رویداد
          </Button>
        </form>
      </Card>
    </Space>
  );
}

export function TicketsAdmin({ events }: { events: Event[] }) {
  return (
    <Tabs
      items={[
        { key: "events", label: "رویدادها", children: <EventsTab events={events} /> },
        { key: "setup", label: "ساخت سالن/رویداد", children: <SetupTab /> },
      ]}
    />
  );
}
