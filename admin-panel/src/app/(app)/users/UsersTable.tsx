"use client";

import { Table, Tag, Input } from "antd";
import { useMemo, useState } from "react";
import { formatDateTime } from "@/lib/format";

export type AdminUser = {
  id: string;
  nationalCode: string;
  phoneNumber: string;
  displayName: string | null;
  status: "active" | "suspended";
  source: string;
  membershipNumber: string | null;
  city: string | null;
  joinedAt: string | null;
  createdAt: string;
};

const statusColor: Record<AdminUser["status"], string> = {
  active: "success",
  suspended: "error",
};

export function UsersTable({ data }: { data: AdminUser[] }) {
  const [search, setSearch] = useState("");

  const filtered = useMemo(() => {
    const q = search.trim();
    if (!q) return data;
    return data.filter(
      (u) =>
        u.phoneNumber.includes(q) ||
        u.nationalCode.includes(q) ||
        (u.displayName ?? "").includes(q) ||
        (u.membershipNumber ?? "").includes(q),
    );
  }, [data, search]);

  return (
    <>
      <Input.Search
        placeholder="جستجو بر اساس نام، موبایل، کد ملی یا شماره عضویت"
        allowClear
        style={{ maxWidth: 420, marginBottom: 16 }}
        onChange={(e) => setSearch(e.target.value)}
      />
      <Table
        rowKey="id"
        dataSource={filtered}
        columns={[
          { title: "نام", dataIndex: "displayName", render: (v) => v ?? "—" },
          { title: "موبایل", dataIndex: "phoneNumber" },
          { title: "کد ملی", dataIndex: "nationalCode" },
          { title: "شماره عضویت", dataIndex: "membershipNumber", render: (v) => v ?? "—" },
          { title: "شهر", dataIndex: "city", render: (v) => v ?? "—" },
          {
            title: "وضعیت",
            dataIndex: "status",
            render: (v: AdminUser["status"]) => (
              <Tag color={statusColor[v]}>{v === "active" ? "فعال" : "معلق"}</Tag>
            ),
            filters: [
              { text: "فعال", value: "active" },
              { text: "معلق", value: "suspended" },
            ],
            onFilter: (value, record) => record.status === value,
          },
          { title: "منبع", dataIndex: "source" },
          {
            title: "تاریخ عضویت",
            dataIndex: "joinedAt",
            render: (v) => formatDateTime(v),
            sorter: (a, b) => new Date(a.joinedAt ?? 0).getTime() - new Date(b.joinedAt ?? 0).getTime(),
          },
        ]}
        pagination={{ pageSize: 20, showSizeChanger: true }}
      />
    </>
  );
}
