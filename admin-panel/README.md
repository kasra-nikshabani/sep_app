# Admin Panel — Next.js + TypeScript

**وضعیت:** قابل‌اجرا (Phase 19). Next.js 16 (App Router، Turbopack) + TypeScript + Auth.js v5 (Keycloak) + Ant Design — [ADR-0016](../docs/adr/0016-admin-panel.md).

**نکته‌ی Phase 19 (ممیزی امنیتی):** `backendFetch`/`backendUpload` (`src/lib/backend.ts`) حالا یک بررسی نقش `admin` مستقل هم دارند -- علاوه بر بررسی موجود `proxy.ts`/`layout.tsx`. دلیل: یک Server Action مسیر فراخوانی جداگانه‌ی خودش دارد که می‌تواند ساختاری آن بررسی سطح صفحه را دور بزند؛ این خط دومین و مستقل‌ترین لایه‌ی دفاع است. تأیید زنده در مرورگر (ورود واقعی + Dashboard/`/users`) انجام شد. جزئیات کامل در [ADR-0021](../docs/adr/0021-security-audit.md).

## اجرا (محلی، در برابر Backend + Keycloak زنده)

```bash
# ۱. Stack زیرساخت + Backend باید بالا باشند (طبق ریشه‌ی پروژه)
cd ../infra && docker-compose --env-file .env up -d
cd ../backend && ./mvnw spring-boot:run &

# ۲. اجرای Admin Panel
cd ../admin-panel
cp .env.example .env.local   # مقادیر واقعی را طبق راهنمای داخل فایل پر کنید
npm install
npm run dev -- -p 3001       # اگر پورت ۳۰۰۰ آزاد است، -p 3001 لازم نیست
```

سرور روی `http://localhost:3001` (یا ۳۰۰۰) بالا می‌آید. ورود از طریق دکمه‌ی «Sign in with Keycloak»؛ فقط نقش `admin` اجازه‌ی ورود دارد (بقیه به `/unauthorized` هدایت می‌شوند — `src/proxy.ts`).

**نکته‌ی مهم درباره‌ی پورت:** Realm واقعی Keycloak (`keycloak-realm.md`، Phase 4) Client `admin-panel` را برای Redirect URI پورت ۳۰۰۰ تنظیم کرده بود. اگر پورت دیگری استفاده می‌کنید (مثلاً چون ۳۰۰۰ توسط برنامه‌ی دیگری اشغال است)، باید همان پورت را هم به `redirectUris`/`webOrigins` همان Client در Keycloak Admin Console اضافه کنید (طبق همان الگو، هر دو پورت هم‌زمان می‌توانند ثبت باشند).

## مرجع تصمیم‌ها

- `sepahan-shop` (پروژه‌ی موجود روی Desktop) **فقط مرجع UI/UX** است؛ کد/Prisma/SQLite آن import نشد — [ADR-0002](../docs/adr/0002-modular-monolith-and-module-boundaries.md)
- پایه‌ی بصری (رنگ/تایپوگرافی/کامپوننت مشترک با Mobile): [Design System](../docs/architecture/ui-ux/design-system.html) — پالت و فونت مستقیماً در [`src/theme.ts`](src/theme.ts) و [`src/app/fonts/`](src/app/fonts/) پیاده شده
- احراز هویت: Authorization Code Flow با Keycloak از طریق Auth.js v5 (BFF، بدون افشای Token در مرورگر) — [ADR-0003](../docs/adr/0003-keycloak-central-sso-fan-id.md) / [ADR-0016](../docs/adr/0016-admin-panel.md)
- معماری کلی این فاز (محدوده، کتابخانه‌ی UI، الگوی BFF، باگ Refresh Token Rotation): [ADR-0016](../docs/adr/0016-admin-panel.md)

## بخش‌های پیاده‌سازی‌شده (طبق بند ۱۸ بریف)

| بخش | وضعیت |
|---|---|
| Dashboard | ✅ (تجمیع سمت کلاینت از چند Endpoint موجود) |
| News | ✅ کامل (دسته/برچسب/رسانه/Revision/زمان‌بندی) |
| Shop (کاتالوگ) | ✅ کامل (دسته/محصول/Variant/موجودی/ارسال) |
| Orders | ✅ (ارسال/تحویل + بررسی مرجوعی با شناسه‌ی دستی) |
| Tickets | ✅ فقط تئاتر داخلی (سالن/صندلی/رویداد) |
| Payments | ✅ فقط مشاهده/تطبیق (بدون Refund) |
| Loyalty | ✅ کامل (سطح/قانون امتیاز/جایزه/درخواست) |
| Notifications | ✅ کامل (ارسال دستی + دفترکل) |
| Partners | ✅ کامل (ساخت + نمایش یک‌بار کلید API) |
| Users / Fans | ✅ یک بخش ترکیبی (فقط مشاهده) |
| Settings | ✅ حداقلی (اطلاعات حساب خودم) |
| Matches (فوتبال), Insurance, Travel, Vehicle, Entertainment, Integrations, Audit Logs | 🕓 صفحه‌ی «به‌زودی» با دلیل مستند — طبق ADR-0016 |

## ساختار

- `src/auth.ts` — پیکربندی Auth.js (Keycloak Provider، Refresh Token Rotation).
- `src/proxy.ts` — جایگزین Next.js 16 برای `middleware.ts` (محافظت مسیرها + نقش admin).
- `src/lib/backend.ts` — تنها راه فراخوانی مستقیم Backend از Server Component/Action.
- `src/app/(app)/*` — هر بخش یک زیرپوشه (Server Component برای خواندن + Server Action برای نوشتن).
- `src/app/soon/[key]` — صفحه‌ی مشترک «به‌زودی» برای بخش‌های بدون Backend.
