"use client";

import { useActionState } from "react";
import { Card, Row, Col, Form, Input, Button, Alert, Tag, Space } from "antd";
import { createCategoryAction, createTagAction, type ActionState } from "./actions";
import type { Category, NewsTag } from "./NewsAdmin";

const initial: ActionState = {};

export function TaxonomyCard({ categories, tags }: { categories: Category[]; tags: NewsTag[] }) {
  const [catState, catAction, catPending] = useActionState(createCategoryAction, initial);
  const [tagState, tagAction, tagPending] = useActionState(createTagAction, initial);

  return (
    <Card size="small" title="دسته‌بندی‌ها و برچسب‌ها" style={{ marginBottom: 16 }}>
      <Row gutter={24}>
        <Col span={12}>
          <Space wrap style={{ marginBottom: 12 }}>
            {categories.map((c) => (
              <Tag key={c.id}>{c.name}</Tag>
            ))}
          </Space>
          <form action={catAction} key={catState.success ? "1" : "0"} style={{ display: "flex", gap: 8, flexWrap: "wrap", alignItems: "flex-end" }}>
            {catState.error && <Alert type="error" title={catState.error} style={{ width: "100%", marginBottom: 8 }} />}
            <Form.Item>
              <Input name="name" placeholder="نام دسته" required />
            </Form.Item>
            <Form.Item>
              <Input name="slug" placeholder="slug" dir="ltr" required />
            </Form.Item>
            <Button htmlType="submit" loading={catPending}>
              افزودن دسته
            </Button>
          </form>
        </Col>
        <Col span={12}>
          <Space wrap style={{ marginBottom: 12 }}>
            {tags.map((t) => (
              <Tag key={t.id} color="gold">
                {t.name}
              </Tag>
            ))}
          </Space>
          <form action={tagAction} key={tagState.success ? "1" : "0"} style={{ display: "flex", gap: 8, flexWrap: "wrap", alignItems: "flex-end" }}>
            {tagState.error && <Alert type="error" title={tagState.error} style={{ width: "100%", marginBottom: 8 }} />}
            <Form.Item>
              <Input name="name" placeholder="نام برچسب" required />
            </Form.Item>
            <Form.Item>
              <Input name="slug" placeholder="slug" dir="ltr" required />
            </Form.Item>
            <Button htmlType="submit" loading={tagPending}>
              افزودن برچسب
            </Button>
          </form>
        </Col>
      </Row>
    </Card>
  );
}
