import { NextRequest, NextResponse } from "next/server";
import { auth } from "@/auth";

/**
 * پل BFF برای Client Componentهای تعاملی (جدول با Pagination/Sort/فیلتر) که
 * نمی‌توانند مستقیم Server Action صدا بزنند. طبق docs/backend-for-frontend
 * (Next.js 16): فقط Header لازم را عبور می‌دهیم، نه کل Request را کورکورانه.
 * هر درخواست جدا Session را چک می‌کند (طبق هشدار Next.js 16: Proxy تنها خط
 * دفاعی نیست) -- دفاع واقعی همیشه Spring Boot (JWT امضاشده) است.
 */
async function proxy(req: NextRequest, path: string[]): Promise<NextResponse> {
  const session = await auth();
  if (!session?.accessToken) {
    return NextResponse.json({ message: "احراز هویت لازم است" }, { status: 401 });
  }
  if (!session.roles?.includes("admin")) {
    return NextResponse.json({ message: "دسترسی غیرمجاز" }, { status: 403 });
  }

  const targetUrl = `${process.env.BACKEND_API_BASE_URL}/api/v1/${path.join("/")}${req.nextUrl.search}`;
  const hasBody = !["GET", "HEAD"].includes(req.method);

  const backendResponse = await fetch(targetUrl, {
    method: req.method,
    headers: {
      "Content-Type": req.headers.get("content-type") ?? "application/json",
      Authorization: `Bearer ${session.accessToken}`,
    },
    body: hasBody ? await req.text() : undefined,
    cache: "no-store",
  });

  const responseBody = await backendResponse.text();
  return new NextResponse(responseBody.length > 0 ? responseBody : null, {
    status: backendResponse.status,
    headers: { "Content-Type": backendResponse.headers.get("content-type") ?? "application/json" },
  });
}

type RouteContext = { params: Promise<{ path: string[] }> };

export async function GET(req: NextRequest, ctx: RouteContext) {
  return proxy(req, (await ctx.params).path);
}
export async function POST(req: NextRequest, ctx: RouteContext) {
  return proxy(req, (await ctx.params).path);
}
export async function PUT(req: NextRequest, ctx: RouteContext) {
  return proxy(req, (await ctx.params).path);
}
export async function DELETE(req: NextRequest, ctx: RouteContext) {
  return proxy(req, (await ctx.params).path);
}
