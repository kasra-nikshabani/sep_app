import { NextResponse } from "next/server";
import { auth } from "@/auth";

/**
 * جایگزین Next.js 16 برای middleware.ts (فایل و نام Export هر دو تغییر کرده‌اند).
 * این فقط یک Redirect برای تجربه‌ی کاربری است -- اجرای واقعی RBAC همیشه در
 * Spring Boot (JWT امضاشده + @PreAuthorize) انجام می‌شود؛ طبق هشدار مستندات
 * Next.js 16، Server Action/Route Handler هرم Proxy را دور می‌زنند، پس هر
 * Route Handler هم جداگانه Session را چک می‌کند (backendFetch/api/backend).
 */
export default auth((req) => {
  const { pathname } = req.nextUrl;
  const session = req.auth;

  if (!session) {
    // بدون این استثنا، چون /signin هم بدون Session است، خودش را دوباره Redirect می‌کند
    // (حلقه‌ی بی‌نهایت) -- pages.signIn در auth.ts همین مسیر را به‌عنوان صفحه‌ی ورود معرفی می‌کند.
    if (pathname === "/signin") {
      return NextResponse.next();
    }
    const signInUrl = new URL("/signin", req.nextUrl.origin);
    signInUrl.searchParams.set("callbackUrl", req.nextUrl.href);
    return NextResponse.redirect(signInUrl);
  }

  if (!session.roles?.includes("admin") && pathname !== "/unauthorized") {
    return NextResponse.redirect(new URL("/unauthorized", req.nextUrl.origin));
  }

  return NextResponse.next();
});

export const config = {
  // فایل‌های استاتیک عمومی (مثل لوگو) هم عمداً کنار گذاشته شدند -- بدون این، وقتی
  // next/image این فایل‌ها را داخلی Fetch می‌کند، خودش به این Middleware می‌خورد و به‌جای
  // تصویر واقعی، HTML صفحه‌ی ورود برمی‌گردد (کشف واقعی همین فاز).
  matcher: ["/((?!_next/static|_next/image|favicon.ico|api/auth|.*\\.(?:png|jpg|jpeg|gif|svg|webp|ico)$).*)"],
};
