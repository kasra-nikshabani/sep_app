"use server";

import { signIn } from "@/auth";

export async function signInAction() {
  // بدون redirectTo صریح، مقصد پیش‌فرض همان صفحه‌ای است که signIn از آن صدا زده شده
  // (یعنی خودِ /signin) -- بعد از ورود موفق دوباره فرم ورود را نشان می‌دهد، نه Dashboard را.
  await signIn("keycloak", { redirectTo: "/" });
}
