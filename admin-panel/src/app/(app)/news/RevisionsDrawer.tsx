"use client";

import { Drawer, List, Typography } from "antd";
import { formatDateTime } from "@/lib/format";

export type Revision = {
  id: string;
  title: string;
  summary: string | null;
  content: string;
  editedBy: string | null;
  editedAt: string;
};

export function RevisionsDrawer({
  open,
  onClose,
  revisions,
}: {
  open: boolean;
  onClose: () => void;
  revisions: Revision[];
}) {
  return (
    <Drawer title="تاریخچه‌ی ویرایش" open={open} onClose={onClose} size={480}>
      <List
        dataSource={revisions}
        locale={{ emptyText: "هنوز ویرایشی ثبت نشده" }}
        renderItem={(r) => (
          <List.Item>
            <List.Item.Meta
              title={r.title}
              description={
                <>
                  <Typography.Text type="secondary">{formatDateTime(r.editedAt)}</Typography.Text>
                  <br />
                  {r.summary}
                </>
              }
            />
          </List.Item>
        )}
      />
    </Drawer>
  );
}
