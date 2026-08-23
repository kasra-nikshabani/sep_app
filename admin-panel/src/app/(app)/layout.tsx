import { redirect } from "next/navigation";
import { auth } from "@/auth";
import { AppShell } from "@/components/AppShell";

export default async function AppLayout({ children }: LayoutProps<"/">) {
  const session = await auth();

  // دفاع دوم طبق هشدار مستندات Next.js 16 -- proxy.ts تنها خط دفاعی نیست
  if (!session) {
    redirect("/api/auth/signin");
  }
  if (!session.roles?.includes("admin")) {
    redirect("/unauthorized");
  }

  const displayName = session.user?.name ?? session.user?.email ?? "کاربر ادمین";

  return <AppShell displayName={displayName}>{children}</AppShell>;
}
