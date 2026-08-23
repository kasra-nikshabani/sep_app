"use client";

import { useActionState, useEffect, useState } from "react";
import { Modal, Form, Input, Select, Alert, Button } from "antd";
import { createArticleAction, updateArticleAction, type ActionState } from "./actions";
import { MediaUploadField } from "./MediaUploadField";
import type { ArticleDetail, Category, NewsTag } from "./NewsAdmin";

const initial: ActionState = {};

export function ArticleFormModal({
  open,
  onClose,
  article,
  categories,
  tags,
}: {
  open: boolean;
  onClose: () => void;
  article: ArticleDetail | null;
  categories: Category[];
  tags: NewsTag[];
}) {
  const action = article ? updateArticleAction.bind(null, article.id) : createArticleAction;
  const [state, formAction, pending] = useActionState(action, initial);

  useEffect(() => {
    if (state.success) onClose();
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [state.success]);

  const defaultCategoryId = article ? categories.find((c) => c.name === article.categoryName)?.id : undefined;
  const [categoryId, setCategoryId] = useState<string | undefined>(defaultCategoryId);
  const [tagIds, setTagIds] = useState<string[]>(article?.tags?.map((t) => t.id) ?? []);

  return (
    <Modal title={article ? "ویرایش خبر" : "خبر جدید"} open={open} onCancel={onClose} footer={null} width={720} destroyOnHidden>
      <form action={formAction}>
        {state.error && <Alert type="error" showIcon title={state.error} style={{ marginBottom: 16 }} />}
        <input type="hidden" name="categoryId" value={categoryId ?? ""} />
        {tagIds.map((id) => (
          <input key={id} type="hidden" name="tagIds" value={id} />
        ))}
        <Form.Item label="عنوان" required>
          <Input name="title" defaultValue={article?.title} />
        </Form.Item>
        {!article && (
          <Form.Item label="Slug" required>
            <Input name="slug" dir="ltr" />
          </Form.Item>
        )}
        <Form.Item label="دسته‌بندی" required>
          <Select value={categoryId} onChange={setCategoryId} options={categories.map((c) => ({ value: c.id, label: c.name }))} />
        </Form.Item>
        <Form.Item label="برچسب‌ها">
          <Select
            mode="multiple"
            value={tagIds}
            onChange={setTagIds}
            options={tags.map((t) => ({ value: t.id, label: t.name }))}
          />
        </Form.Item>
        <Form.Item label="خلاصه">
          <Input.TextArea name="summary" defaultValue={article?.summary ?? ""} rows={2} />
        </Form.Item>
        <Form.Item label="متن کامل" required>
          <Input.TextArea name="content" defaultValue={article?.content ?? ""} rows={8} />
        </Form.Item>
        <Form.Item label="تصویر کاور">
          <MediaUploadField name="coverImageUrl" defaultValue={article?.coverImageUrl} />
        </Form.Item>
        <Form.Item label="Meta Title">
          <Input name="metaTitle" defaultValue={article?.metaTitle ?? ""} />
        </Form.Item>
        <Form.Item label="Meta Description">
          <Input name="metaDescription" defaultValue={article?.metaDescription ?? ""} />
        </Form.Item>
        <Button type="primary" htmlType="submit" loading={pending} block>
          ذخیره
        </Button>
      </form>
    </Modal>
  );
}
