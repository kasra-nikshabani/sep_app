"use client";

import { useRef, useState, useTransition } from "react";
import { Input, Button, Space, Alert, Image } from "antd";
import { UploadOutlined } from "@ant-design/icons";
import { uploadMediaAction } from "./actions";

export function MediaUploadField({ name, defaultValue }: { name: string; defaultValue?: string | null }) {
  const [url, setUrl] = useState(defaultValue ?? "");
  const [error, setError] = useState<string>();
  const [isPending, startTransition] = useTransition();
  const fileInputRef = useRef<HTMLInputElement>(null);

  // یک <form> واقعی این‌جا نیست عمداً -- چون این کامپوننت داخل فرم اصلی خبر
  // Nest می‌شود و HTML اجازه‌ی <form> تودرتو نمی‌دهد؛ به‌جایش Server Action
  // مستقیماً (بدون Form Action) با FormData دستی صدا زده می‌شود.
  function handleUpload() {
    const file = fileInputRef.current?.files?.[0];
    if (!file) {
      setError("فایلی انتخاب نشده است");
      return;
    }
    const formData = new FormData();
    formData.set("file", file);
    startTransition(async () => {
      const result = await uploadMediaAction({}, formData);
      if (result.error) {
        setError(result.error);
      } else if (result.url) {
        setError(undefined);
        setUrl(result.url);
      }
    });
  }

  return (
    <Space orientation="vertical" style={{ width: "100%" }}>
      {error && <Alert type="error" title={error} />}
      <Input name={name} value={url} onChange={(e) => setUrl(e.target.value)} dir="ltr" placeholder="/media/..." />
      <div style={{ display: "flex", gap: 8, alignItems: "center" }}>
        <input ref={fileInputRef} type="file" accept="image/*" />
        <Button size="small" icon={<UploadOutlined />} loading={isPending} onClick={handleUpload}>
          آپلود
        </Button>
      </div>
      {url && <Image src={url} alt="پیش‌نمایش" width={120} />}
    </Space>
  );
}
