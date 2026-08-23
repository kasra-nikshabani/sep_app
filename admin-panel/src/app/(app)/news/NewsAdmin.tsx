"use client";

import { useState, useTransition } from "react";
import { Table, Tag, Button, Space, Popconfirm, DatePicker, message } from "antd";
import { PlusOutlined } from "@ant-design/icons";
import dayjs from "dayjs";
import { formatDateTime } from "@/lib/format";
import { TaxonomyCard } from "./TaxonomyCard";
import { ArticleFormModal } from "./ArticleFormModal";
import { RevisionsDrawer, type Revision } from "./RevisionsDrawer";
import {
  publishArticleAction,
  archiveArticleAction,
  deleteArticleAction,
  scheduleArticleAction,
  getArticleDetailAction,
  getArticleRevisionsAction,
} from "./actions";

export type Category = { id: string; name: string; slug: string };
export type NewsTag = { id: string; name: string; slug: string };

type ArticleStatusType = "draft" | "scheduled" | "published" | "archived";

export type Article = {
  id: string;
  title: string;
  slug: string;
  summary: string | null;
  coverImageUrl: string | null;
  categoryName: string;
  status: ArticleStatusType;
  publishedAt: string | null;
};

export type ArticleDetail = Article & {
  content: string;
  metaTitle: string | null;
  metaDescription: string | null;
  authorDisplayName: string | null;
  tags: NewsTag[];
  scheduledAt: string | null;
  createdAt: string;
  updatedAt: string;
};

const statusMeta: Record<ArticleStatusType, { color: string; label: string }> = {
  draft: { color: "default", label: "پیش‌نویس" },
  scheduled: { color: "processing", label: "زمان‌بندی‌شده" },
  published: { color: "success", label: "منتشرشده" },
  archived: { color: "default", label: "بایگانی‌شده" },
};

export function NewsAdmin({
  articles,
  categories,
  tags,
}: {
  articles: Article[];
  categories: Category[];
  tags: NewsTag[];
}) {
  const [isPending, startTransition] = useTransition();
  const [formOpen, setFormOpen] = useState(false);
  const [editing, setEditing] = useState<ArticleDetail | null>(null);
  const [revisionsOpen, setRevisionsOpen] = useState(false);
  const [revisions, setRevisions] = useState<Revision[]>([]);

  async function openEdit(article: Article) {
    const detail = (await getArticleDetailAction(article.id)) as ArticleDetail;
    setEditing(detail);
    setFormOpen(true);
  }

  async function openRevisions(article: Article) {
    const list = (await getArticleRevisionsAction(article.id)) as Revision[];
    setRevisions(list);
    setRevisionsOpen(true);
  }

  return (
    <>
      <TaxonomyCard categories={categories} tags={tags} />

      <Button
        type="primary"
        icon={<PlusOutlined />}
        onClick={() => {
          setEditing(null);
          setFormOpen(true);
        }}
        style={{ marginBottom: 16 }}
      >
        خبر جدید
      </Button>

      <Table
        rowKey="id"
        dataSource={articles}
        columns={[
          { title: "عنوان", dataIndex: "title" },
          { title: "دسته‌بندی", dataIndex: "categoryName" },
          {
            title: "وضعیت",
            dataIndex: "status",
            render: (v: ArticleStatusType) => <Tag color={statusMeta[v].color}>{statusMeta[v].label}</Tag>,
            filters: Object.entries(statusMeta).map(([value, m]) => ({ text: m.label, value })),
            onFilter: (value, record) => record.status === value,
          },
          { title: "تاریخ انتشار", dataIndex: "publishedAt", render: (v) => formatDateTime(v) },
          {
            title: "عملیات",
            render: (_, record: Article) => (
              <Space wrap>
                <Button size="small" onClick={() => openEdit(record)}>
                  ویرایش
                </Button>
                <Button size="small" onClick={() => openRevisions(record)}>
                  تاریخچه
                </Button>
                {record.status !== "published" && (
                  <Popconfirm title="این خبر همین حالا منتشر شود؟" onConfirm={() => startTransition(() => { publishArticleAction(record.id); })}>
                    <Button size="small" type="primary" loading={isPending}>
                      انتشار
                    </Button>
                  </Popconfirm>
                )}
                {record.status !== "published" && (
                  <DatePicker
                    showTime
                    placeholder="زمان‌بندی"
                    onChange={(d) => {
                      if (!d) return;
                      startTransition(() => {
                        scheduleArticleAction(record.id, dayjs(d).toISOString()).then(() => {
                          message.success("زمان‌بندی شد");
                        });
                      });
                    }}
                  />
                )}
                {record.status !== "archived" && (
                  <Popconfirm title="این خبر بایگانی شود؟" onConfirm={() => startTransition(() => { archiveArticleAction(record.id); })}>
                    <Button size="small" loading={isPending}>
                      بایگانی
                    </Button>
                  </Popconfirm>
                )}
                <Popconfirm title="این خبر حذف شود؟ (Soft Delete)" onConfirm={() => startTransition(() => { deleteArticleAction(record.id); })}>
                  <Button size="small" danger loading={isPending}>
                    حذف
                  </Button>
                </Popconfirm>
              </Space>
            ),
          },
        ]}
        pagination={{ pageSize: 15 }}
      />

      <ArticleFormModal
        open={formOpen}
        onClose={() => setFormOpen(false)}
        article={editing}
        categories={categories}
        tags={tags}
      />
      <RevisionsDrawer open={revisionsOpen} onClose={() => setRevisionsOpen(false)} revisions={revisions} />
    </>
  );
}
