# اپ موبایل — Expo + TypeScript

**وضعیت:** قابل‌اجرا (Phase 15، فقط نسخه‌ی Web تأیید شده — بدون Emulator/دستگاه واقعی در این محیط). Expo SDK 57 + Expo Router + expo-auth-session (Keycloak PKCE) — [ADR-0017](../docs/adr/0017-mobile-app.md).

## اجرا (محلی، در برابر Backend + Keycloak زنده)

```bash
# ۱. Stack زیرساخت + Backend باید بالا باشند (طبق ریشه‌ی پروژه)
cd ../infra && docker-compose --env-file .env up -d
cd ../backend && ./mvnw spring-boot:run &

# ۲. اجرای اپ موبایل (نسخه‌ی Web -- تنها راه تست در این محیط)
cd ../mobile
cp .env.example .env.local   # مقادیر واقعی را طبق راهنمای داخل فایل پر کنید
npm install
npm run web -- --port 8082
```

روی `http://localhost:8082` بالا می‌آید. ورود از طریق دکمه‌ی «ورود با کد ملی» (SSO Keycloak)؛ بعد از ورود موفق، هر Fan ID معتبر مجاز به استفاده است (برخلاف Admin Panel که فقط نقش `admin` را می‌پذیرد).

**نکته‌ی مهم درباره‌ی Redirect URI:** Client `mobile-app` در Keycloak باید هم `http://localhost:8082/*` (Web) و هم `sepahan://auth/callback` (Native، برای Buildهای آینده) را در `redirectUris` داشته باشد.

**اجرای Native واقعی (خارج از این محیط):** `npx expo start` و اسکن QR با Expo Go، یا `eas build` برای Build واقعی. بدون Emulator/دستگاه در این‌جا، این مسیرها فقط از طریق کد/مستندات طراحی شدند و تست زنده نشدند (رجوع به محدودیت‌های [ADR-0017](../docs/adr/0017-mobile-app.md)).

## مرجع تصمیم‌ها

- پایه‌ی بصری (رنگ/تایپوگرافی/آیکون مشترک با Admin Panel): [Design System](../docs/architecture/ui-ux/design-system.html) — مستقیماً در [`src/theme.ts`](src/theme.ts) و [`src/components/Icon.tsx`](src/components/Icon.tsx) پیاده شده؛ فونت Vazirmatn Self-host در [`assets/fonts/`](assets/fonts/)
- احراز هویت: Authorization Code Flow + PKCE با Keycloak، مستقیم در Client (بدون لایه‌ی سروری میانی، برخلاف Admin Panel) — [ADR-0003](../docs/adr/0003-keycloak-central-sso-fan-id.md) / [ADR-0017](../docs/adr/0017-mobile-app.md)
- معماری کلی این فاز (محدوده، دو باگ Redirect/Discovery در PKCE روی Web، افزودن CORS به Backend، رفع باگ Race در JIT Provisioning): [ADR-0017](../docs/adr/0017-mobile-app.md)
- بلیط فوتبال (Matches): جریان خرید در Django کاملاً HTML/Session-محور است، نه JSON API — بدون Endpoint جدید یا حدس API، فعلاً «به‌زودی» — رجوع به [ADR-0017](../docs/adr/0017-mobile-app.md)

## بخش‌های پیاده‌سازی‌شده (طبق بند ۱۹ بریف)

| بخش | وضعیت |
|---|---|
| SSO Login | ✅ (PKCE، Keycloak) |
| Home | ✅ (تجمیع سمت کلاینت از چند Endpoint موجود) |
| News | ✅ کامل (لیست + جزئیات) |
| Shop | ✅ کامل (کاتالوگ/سبد/Checkout/پرداخت/سفارش‌ها) |
| Tickets | ✅ فقط تئاتر داخلی (رویداد/صندلی/رزرو/بلیط‌های من) |
| Loyalty | ✅ کامل (حساب/سطح/تراکنش/جایزه) |
| Notifications | ✅ ثبت/لغو Device Token (بدون Provider واقعی Push، طبق ADR-0015) |
| Profile | ✅ حداقلی (اطلاعات حساب خودم) |
| Wallet, Services (بیمه/سفر/خودرو/سرگرمی) | 🕓 «به‌زودی» — بدون ماژول Backend (طبق ADR-0002/ADR-0015) |
| Matches (بلیط فوتبال) | 🕓 «به‌زودی» — Django بدون JSON API برای این جریان (کشف Phase 15) |

## ساختار

- `src/lib/auth.tsx` — `AuthProvider` (expo-auth-session PKCE)، شامل مسیر ویژه‌ی Redirect کامل روی Web و هماهنگی با `app/auth/callback.tsx`.
- `src/lib/storage.ts` — انتزاع Storage (`expo-secure-store` روی Native، `localStorage` روی Web).
- `src/lib/api.ts` — تنها راه فراخوانی Backend (تزریق خودکار Bearer Token + Refresh).
- `src/features/*/api.ts` — Hookهای React Query هر ماژول، تایپ‌شده دقیقاً روی DTOهای Backend.
- `app/` — مسیرهای Expo Router (فایل‌محور)؛ `app/(app)/(tabs)/` ناوبری پایین اصلی، `app/(app)/soon/[key]` صفحه‌ی مشترک «به‌زودی».
