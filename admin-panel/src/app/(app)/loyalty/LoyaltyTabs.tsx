"use client";

import { useActionState, useState, useTransition } from "react";
import { Tabs, Table, Form, Input, InputNumber, Select, Button, Alert, Tag, Space, Popconfirm, Card } from "antd";
import { formatDateTime } from "@/lib/format";
import {
  createLevelAction,
  createEarningRuleAction,
  createRewardAction,
  fulfillRedemptionAction,
  cancelRedemptionAction,
  type ActionState,
} from "./actions";

export type Level = { id: string; name: string; minPoints: number; benefits: string | null };
export type EarningRule = {
  id: string;
  sourceType: "shop_order" | "ticket_purchase" | "football_ticket_purchase";
  pointsPerAmount: string;
  active: boolean;
};
export type Reward = {
  id: string;
  name: string;
  description: string | null;
  pointsCost: number;
  stockQuantity: number | null;
  active: boolean;
};
export type Redemption = {
  id: string;
  rewardName: string;
  pointsSpent: number;
  redemptionCode: string;
  status: "requested" | "fulfilled" | "cancelled";
  createdAt: string;
  fulfilledAt: string | null;
};

const sourceTypeLabel: Record<EarningRule["sourceType"], string> = {
  shop_order: "سفارش فروشگاه",
  ticket_purchase: "بلیط تئاتر",
  football_ticket_purchase: "بلیط فوتبال",
};

const initial: ActionState = {};

function LevelsTab({ data }: { data: Level[] }) {
  const [state, formAction, pending] = useActionState(createLevelAction, initial);
  return (
    <Space orientation="vertical" style={{ width: "100%" }} size="large">
      <Table
        rowKey="id"
        dataSource={data}
        columns={[
          { title: "نام سطح", dataIndex: "name" },
          { title: "حداقل امتیاز", dataIndex: "minPoints" },
          { title: "مزایا", dataIndex: "benefits", render: (v) => v ?? "—" },
        ]}
        pagination={false}
      />
      <Card title="سطح جدید" size="small">
        <form
          action={formAction}
          key={state.success ? "1" : "0"}
          style={{ display: "flex", gap: 8, flexWrap: "wrap", alignItems: "flex-end" }}
        >
          {state.error && <Alert type="error" title={state.error} style={{ width: "100%", marginBottom: 12 }} />}
          <Form.Item>
            <Input name="name" placeholder="نام (مثلاً برنزی)" required />
          </Form.Item>
          <Form.Item>
            <InputNumber name="minPoints" placeholder="حداقل امتیاز" min={0} required />
          </Form.Item>
          <Form.Item>
            <Input name="benefits" placeholder="مزایا (اختیاری)" style={{ width: 240 }} />
          </Form.Item>
          <Button type="primary" htmlType="submit" loading={pending}>
            افزودن
          </Button>
        </form>
      </Card>
    </Space>
  );
}

function EarningRulesTab({ data }: { data: EarningRule[] }) {
  const [state, formAction, pending] = useActionState(createEarningRuleAction, initial);
  const [sourceType, setSourceType] = useState<string>();
  return (
    <Space orientation="vertical" style={{ width: "100%" }} size="large">
      <Table
        rowKey="id"
        dataSource={data}
        columns={[
          { title: "منبع", dataIndex: "sourceType", render: (v: EarningRule["sourceType"]) => sourceTypeLabel[v] },
          { title: "امتیاز به ازای هر واحد مبلغ", dataIndex: "pointsPerAmount" },
          {
            title: "وضعیت",
            dataIndex: "active",
            render: (v: boolean) => <Tag color={v ? "success" : "default"}>{v ? "فعال" : "غیرفعال"}</Tag>,
          },
        ]}
        pagination={false}
      />
      <Card title="قانون امتیازدهی جدید" size="small">
        <form
          action={formAction}
          key={state.success ? "1" : "0"}
          style={{ display: "flex", gap: 8, flexWrap: "wrap", alignItems: "flex-end" }}
        >
          {state.error && <Alert type="error" title={state.error} style={{ width: "100%", marginBottom: 12 }} />}
          <input type="hidden" name="sourceType" value={sourceType ?? ""} />
          <Form.Item>
            <Select
              placeholder="منبع"
              style={{ width: 200 }}
              value={sourceType}
              onChange={setSourceType}
              options={Object.entries(sourceTypeLabel).map(([value, label]) => ({ value, label }))}
            />
          </Form.Item>
          <Form.Item>
            <InputNumber name="pointsPerAmount" placeholder="امتیاز/واحد مبلغ" min={0} step={0.01} required />
          </Form.Item>
          <Button type="primary" htmlType="submit" loading={pending}>
            افزودن
          </Button>
        </form>
      </Card>
    </Space>
  );
}

function RewardsTab({ data }: { data: Reward[] }) {
  const [state, formAction, pending] = useActionState(createRewardAction, initial);
  return (
    <Space orientation="vertical" style={{ width: "100%" }} size="large">
      <Alert type="info" showIcon title="فقط جوایز فعال نمایش داده می‌شوند (از Endpoint عمومی فان)" />
      <Table
        rowKey="id"
        dataSource={data}
        columns={[
          { title: "نام", dataIndex: "name" },
          { title: "توضیح", dataIndex: "description", render: (v) => v ?? "—" },
          { title: "هزینه (امتیاز)", dataIndex: "pointsCost" },
          { title: "موجودی", dataIndex: "stockQuantity", render: (v) => v ?? "نامحدود" },
        ]}
        pagination={false}
      />
      <Card title="جایزه‌ی جدید" size="small">
        <form
          action={formAction}
          key={state.success ? "1" : "0"}
          style={{ display: "flex", gap: 8, flexWrap: "wrap", alignItems: "flex-end" }}
        >
          {state.error && <Alert type="error" title={state.error} style={{ width: "100%", marginBottom: 12 }} />}
          <Form.Item>
            <Input name="name" placeholder="نام جایزه" required />
          </Form.Item>
          <Form.Item>
            <Input name="description" placeholder="توضیح" style={{ width: 200 }} />
          </Form.Item>
          <Form.Item>
            <InputNumber name="pointsCost" placeholder="هزینه (امتیاز)" min={1} required />
          </Form.Item>
          <Form.Item>
            <InputNumber name="stockQuantity" placeholder="موجودی (خالی = نامحدود)" min={0} />
          </Form.Item>
          <Button type="primary" htmlType="submit" loading={pending}>
            افزودن
          </Button>
        </form>
      </Card>
    </Space>
  );
}

const redemptionStatusMeta: Record<Redemption["status"], { color: string; label: string }> = {
  requested: { color: "processing", label: "درخواست‌شده" },
  fulfilled: { color: "success", label: "انجام‌شده" },
  cancelled: { color: "default", label: "لغوشده" },
};

function RedemptionsTab({ data }: { data: Redemption[] }) {
  const [isPending, startTransition] = useTransition();

  return (
    <Table
      rowKey="id"
      dataSource={data}
      columns={[
        { title: "جایزه", dataIndex: "rewardName" },
        { title: "کد", dataIndex: "redemptionCode" },
        { title: "امتیاز مصرفی", dataIndex: "pointsSpent" },
        {
          title: "وضعیت",
          dataIndex: "status",
          render: (v: Redemption["status"]) => <Tag color={redemptionStatusMeta[v].color}>{redemptionStatusMeta[v].label}</Tag>,
        },
        { title: "تاریخ درخواست", dataIndex: "createdAt", render: (v) => formatDateTime(v) },
        {
          title: "عملیات",
          render: (_, record) =>
            record.status === "requested" ? (
              <Space>
                <Popconfirm title="تحویل این جایزه ثبت شود؟" onConfirm={() => startTransition(() => { fulfillRedemptionAction(record.id); })}>
                  <Button size="small" type="primary" loading={isPending}>
                    تحویل شد
                  </Button>
                </Popconfirm>
                <Popconfirm title="این درخواست لغو شود؟" onConfirm={() => startTransition(() => { cancelRedemptionAction(record.id); })}>
                  <Button size="small" danger loading={isPending}>
                    لغو
                  </Button>
                </Popconfirm>
              </Space>
            ) : (
              "—"
            ),
        },
      ]}
      pagination={{ pageSize: 20 }}
    />
  );
}

export function LoyaltyTabs({
  levels,
  rules,
  rewards,
  redemptions,
}: {
  levels: Level[];
  rules: EarningRule[];
  rewards: Reward[];
  redemptions: Redemption[];
}) {
  return (
    <Tabs
      items={[
        { key: "redemptions", label: "درخواست‌های جایزه", children: <RedemptionsTab data={redemptions} /> },
        { key: "levels", label: "سطوح", children: <LevelsTab data={levels} /> },
        { key: "rules", label: "قوانین امتیازدهی", children: <EarningRulesTab data={rules} /> },
        { key: "rewards", label: "جوایز", children: <RewardsTab data={rewards} /> },
      ]}
    />
  );
}
