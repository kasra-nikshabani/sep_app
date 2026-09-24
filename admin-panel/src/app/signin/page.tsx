"use client";

import Image from "next/image";
import { Typography } from "antd";
import { signInAction } from "@/app/actions/sign-in";

const { Title, Text } = Typography;

/**
 * صفحه‌ی پیش‌فرض Sign-in خودِ Auth.js برای این پیکربندی (فقط یک OAuth Provider، بدون فرم
 * Credentials) اصلاً theme.logo را رندر نمی‌کند -- تأیید شده با بازرسی مستقیم HTML واقعی
 * (بدون هیچ <img> از theme، فقط دکمه‌ی عمومی Provider). این صفحه‌ی سفارشی جایگزین آن است.
 */
export default function SignInPage() {
  return (
    <div
      style={{
        display: "flex",
        minHeight: "100vh",
        alignItems: "center",
        justifyContent: "center",
        background: "var(--background)",
      }}
    >
      <div style={{ display: "flex", flexDirection: "column", alignItems: "center", gap: 24, padding: 32 }}>
        <Image src="/sepahan-logo.png" alt="نشان باشگاه فولاد مبارکه سپاهان" width={96} height={96} priority />
        <div style={{ textAlign: "center" }}>
          <Title level={3} style={{ margin: 0 }}>
            پنل مدیریت سپاهان
          </Title>
          <Text type="secondary">سامانه مدیریت — باشگاه فولاد مبارکه سپاهان</Text>
        </div>
        <form action={signInAction} style={{ width: 280 }}>
          <button
            type="submit"
            style={{
              width: "100%",
              padding: "12px 20px",
              borderRadius: 10,
              border: "none",
              background: "#14110A",
              color: "#E8A80E",
              fontFamily: "var(--font-vazirmatn), Tahoma, Arial, sans-serif",
              fontWeight: 500,
              fontSize: 15,
              cursor: "pointer",
            }}
          >
            ورود با Fan ID
          </button>
        </form>
      </div>
    </div>
  );
}
