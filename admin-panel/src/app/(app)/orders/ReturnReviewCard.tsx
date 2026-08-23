"use client";

import { useActionState, useState } from "react";
import { Card, Alert, Input, Form, Button, Space } from "antd";
import { reviewReturnAction, type ActionState } from "./actions";

const initial: ActionState = {};

export function ReturnReviewCard() {
  const [returnId, setReturnId] = useState("");
  const approve = useActionState(reviewReturnAction.bind(null, returnId, "approve"), initial);
  const reject = useActionState(reviewReturnAction.bind(null, returnId, "reject"), initial);
  const complete = useActionState(reviewReturnAction.bind(null, returnId, "complete"), initial);

  const [approveState, approveAction, approvePending] = approve;
  const [rejectState, rejectAction, rejectPending] = reject;
  const [completeState, completeAction, completePending] = complete;

  const anyError = approveState.error ?? rejectState.error ?? completeState.error;
  const anySuccess = approveState.success ?? rejectState.success ?? completeState.success;

  return (
    <Card size="small">
      <Alert
        type="info"
        showIcon
        style={{ marginBottom: 16 }}
        title="بدون Endpoint فهرست درخواست‌های مرجوعی"
        description="شناسه‌ی درخواست مرجوعی (returnId) را که هوادار/پشتیبانی اعلام کرده وارد کنید."
      />
      {anyError && <Alert type="error" showIcon title={anyError} style={{ marginBottom: 16 }} />}
      {anySuccess && <Alert type="success" showIcon title="ثبت شد" style={{ marginBottom: 16 }} />}
      <Form layout="inline" style={{ marginBottom: 12 }}>
        <Form.Item label="شناسه‌ی مرجوعی (returnId)">
          <Input value={returnId} onChange={(e) => setReturnId(e.target.value)} dir="ltr" style={{ width: 320 }} />
        </Form.Item>
      </Form>
      <Space wrap align="start">
        <form action={approveAction}>
          <Input.TextArea name="adminNote" placeholder="یادداشت (اختیاری)" style={{ width: 220, marginBottom: 8 }} />
          <Button htmlType="submit" type="primary" block disabled={!returnId} loading={approvePending}>
            تأیید
          </Button>
        </form>
        <form action={rejectAction}>
          <Input.TextArea name="adminNote" placeholder="دلیل رد" style={{ width: 220, marginBottom: 8 }} />
          <Button htmlType="submit" danger block disabled={!returnId} loading={rejectPending}>
            رد
          </Button>
        </form>
        <form action={completeAction}>
          <Input.TextArea name="adminNote" placeholder="یادداشت تکمیل" style={{ width: 220, marginBottom: 8 }} />
          <Button htmlType="submit" block disabled={!returnId} loading={completePending}>
            تکمیل بازپرداخت کالا
          </Button>
        </form>
      </Space>
    </Card>
  );
}
