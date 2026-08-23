import { Result, Button } from "antd";
import Link from "next/link";

export default function UnauthorizedPage() {
  return (
    <div style={{ display: "flex", minHeight: "100vh", alignItems: "center", justifyContent: "center" }}>
      <Result
        status="403"
        title="۴۰۳"
        subTitle="دسترسی به پنل مدیریت فقط برای نقش admin مجاز است."
        extra={
          <Link href="/">
            <Button type="primary">بازگشت</Button>
          </Link>
        }
      />
    </div>
  );
}
