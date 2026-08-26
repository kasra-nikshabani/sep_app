import type { NextConfig } from "next";

const nextConfig: NextConfig = {
  // فقط برای Docker Build واقعی معنی دارد (Phase 20، ADR-0022) -- یک server.js مستقل و
  // حداقلی می‌سازد که node_modules کامل لازم ندارد؛ npm run dev/build محلی بدون تغییر کار
  // می‌کند (طبق مستندات رسمی Next.js همین نسخه‌ی نصب‌شده -- node_modules/next/dist/docs).
  output: "standalone",
};

export default nextConfig;
