"use client";

import { useActionState } from "react";
import { Tabs, Form, Input, Button, Alert } from "antd";
import { sendSmsAction, sendEmailAction, sendPushAction, type SendState } from "./actions";

const initial: SendState = {};

function SmsForm() {
  const [state, formAction, pending] = useActionState(sendSmsAction, initial);
  return (
    <form action={formAction} key={state.success ? "sent" : "idle"}>
      {state.error && <Alert type="error" showIcon title={state.error} style={{ marginBottom: 16 }} />}
      {state.success && <Alert type="success" showIcon title="ثبت شد -- در دفترکل زیر ببینید" style={{ marginBottom: 16 }} />}
      <Form.Item label="موبایل" required>
        <Input name="mobile" dir="ltr" placeholder="09120000000" />
      </Form.Item>
      <Form.Item label="متن پیامک" required>
        <Input.TextArea name="message" rows={3} />
      </Form.Item>
      <Button type="primary" htmlType="submit" loading={pending}>
        ارسال پیامک
      </Button>
    </form>
  );
}

function EmailForm() {
  const [state, formAction, pending] = useActionState(sendEmailAction, initial);
  return (
    <form action={formAction} key={state.success ? "sent" : "idle"}>
      {state.error && <Alert type="error" showIcon title={state.error} style={{ marginBottom: 16 }} />}
      {state.success && <Alert type="success" showIcon title="ثبت شد -- در دفترکل زیر ببینید" style={{ marginBottom: 16 }} />}
      <Form.Item label="ایمیل مقصد" required>
        <Input name="to" dir="ltr" type="email" />
      </Form.Item>
      <Form.Item label="موضوع" required>
        <Input name="subject" />
      </Form.Item>
      <Form.Item label="متن" required>
        <Input.TextArea name="body" rows={4} />
      </Form.Item>
      <Button type="primary" htmlType="submit" loading={pending}>
        ارسال ایمیل
      </Button>
    </form>
  );
}

function PushForm() {
  const [state, formAction, pending] = useActionState(sendPushAction, initial);
  return (
    <form action={formAction} key={state.success ? "sent" : "idle"}>
      {state.error && <Alert type="error" showIcon title={state.error} style={{ marginBottom: 16 }} />}
      {state.success && <Alert type="success" showIcon title="ثبت شد -- در دفترکل زیر ببینید" style={{ marginBottom: 16 }} />}
      <Alert
        type="info"
        showIcon
        style={{ marginBottom: 16 }}
        title="فقط Fake Provider (بدون اپ موبایل واقعی -- ADR-0015)"
      />
      <Form.Item label="Device Token" required>
        <Input name="deviceToken" dir="ltr" />
      </Form.Item>
      <Form.Item label="عنوان" required>
        <Input name="title" />
      </Form.Item>
      <Form.Item label="متن" required>
        <Input.TextArea name="body" rows={3} />
      </Form.Item>
      <Button type="primary" htmlType="submit" loading={pending}>
        ارسال Push
      </Button>
    </form>
  );
}

export function SendForms() {
  return (
    <Tabs
      items={[
        { key: "sms", label: "پیامک", children: <SmsForm /> },
        { key: "email", label: "ایمیل", children: <EmailForm /> },
        { key: "push", label: "Push", children: <PushForm /> },
      ]}
    />
  );
}
