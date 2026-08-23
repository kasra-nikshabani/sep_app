# ADR-0016: Admin Panel (Next.js 16 + Auth.js + Ant Design)

- **وضعیت:** پذیرفته‌شده (Phase 14)
- **تصمیم‌گیرندگان:** Architect + کارفرما (سه فرک واقعی از طریق AskUserQuestion قبل از شروع پیاده‌سازی: محدوده‌ی فاز، کتابخانه‌ی UI، پیاده‌سازی Auth)

## Context

طبق بند ۱۸ بریف، Admin Panel باید ۱۸ بخش داشته باشد: Dashboard, Users, Fans, News, Matches, Tickets, Shop, Orders, Payments, Insurance, Travel, Vehicle, Entertainment, Loyalty, Notifications, Partners, Integrations, Audit Logs, Settings. بررسی سطح API فعلی (Phase 6 تا 13) نشان داد بخش زیادی از این‌ها Backend واقعی ندارند: Users/Fans فقط `/me` داشت، Payments فقط Callback عمومی داشت، Audit Log اصلاً وجود نداشت، Insurance/Travel/Vehicle/Entertainment همان چهار حوزه‌ای بودند که در Phase 13 (ADR-0015) به‌خاطر نبود Provider واقعی کنار گذاشته شدند، و فوتبال/Matches کاملاً داخل Django جداست (ADR-0004/0006).

## Decision 1: محدوده‌ی این فاز

با AskUserQuestion، گزینه‌ی «فقط بخش‌های آماده + دو Endpoint کوچک جدید» انتخاب شد:

- **UI کامل و متصل به Backend واقعی:** Dashboard (تجمیع سمت کلاینت)، News، Shop (کاتالوگ)، Orders، Loyalty، Notifications، Partners، Tickets (فقط تئاتر داخلی)، Settings.
- **دو Endpoint Read-only کوچک جدید** (دقیقاً هم‌الگوی Endpointهای موجود -- بدون منطق کسب‌وکار جدید، بدون Dependency جدید):
  - `GET /api/v1/users/admin` (`UserAdminController`) -- فهرست AppUser+FanProfile.
  - `GET /api/v1/payments/admin` (`PaymentAdminController`) -- فهرست Payment، فقط برای Reconciliation (بدون Refund -- زیبال API استرداد رسمی ندارد).
- **صفحه‌ی «به‌زودی» با دلیل صادقانه‌ی هرکدام** برای: Insurance/Travel/Vehicle/Entertainment (بدون Provider تأییدشده -- ADR-0015)، Integrations (بدون هیچ Adapter ساخته‌شده)، Matches فوتبال (کاملاً داخل Django -- ADR-0004/0006)، Audit Logs (بدون هیچ Backend، نه فقط بدون Provider).

### محدودیت‌های کشف‌شده حین پیاده‌سازی (بدون افزودن Endpoint جدید فراتر از دو مورد بالا)

چند Endpoint فهرست‌کننده که برای UI کامل لازم بود از قبل وجود نداشت؛ به‌جای حدس‌زدن یک API جدید، هرکدام با یک راه‌حل کاربردیِ محدود به API موجود حل شد:

| کمبود | راه‌حل در UI |
|---|---|
| بدون `GET` فهرست Coupon | فقط فرم ساخت؛ بدون جدول |
| بدون `GET` فهرست Venue (تئاتر) | شناسه‌ی سالن بعد از ساخت نمایش داده می‌شود تا برای مرحله‌ی بعد کپی شود |
| بدون `GET` فهرست همه‌ی Return Request | کارت «بررسی با شناسه‌ی دستی» -- ادمین returnId را (که هوادار/پشتیبانی اعلام کرده) وارد می‌کند |
| `GET /loyalty/rewards` فقط جوایز فعال برمی‌گرداند (Endpoint فان) | برچسب صریح در UI که فقط جوایز فعال دیده می‌شوند |
| `GET /shop/products` فقط محصولات فعال (Endpoint فان) | همین محدودیت، مستند شده |

این‌ها Bug نیستند -- تصمیم آگاهانه‌اند تا از افزودن Endpoint فراتر از دامنه‌ی تأییدشده پرهیز شود؛ اگر در آینده یک صف واقعی (مثلاً مدیریت مرجوعی) لازم شود، افزودن یک `GET` ساده به همان الگوی موجود، کار کوچکی خواهد بود.

## Decision 2: کتابخانه‌ی UI -- Ant Design

سه گزینه مطرح شد: Ant Design، shadcn/ui+Tailwind، دست‌ساز روی کلاس‌های CSS موجود در Design System. **Ant Design انتخاب شد** (توصیه‌شده و تأییدشده): پشتیبانی RTL کامل و آماده (`ConfigProvider direction="rtl"` + `locale={faIR}`)، غنی‌ترین مجموعه کامپوننت آماده برای پنل ادمین (Table با Sort/Filter/Pagination، Form، Modal، DatePicker). پالت رسمی هویتی (مشکی/طلایی) و تایپوگرافی Vazirmatn از `docs/architecture/ui-ux/design-system.html` (v0.6) مستقیماً به `theme.token`/`theme.components` در [`src/theme.ts`](../../admin-panel/src/theme.ts) منتقل شد (هم برای حالت روشن هم تیره) -- بدون بازنویسی دستی کامپوننت‌ها.

فونت Vazirmatn (سه وزن 400/500/900) از حالت Base64 داخل همان فایل استخراج و به‌صورت فایل واقعی `.woff2` زیر `src/app/fonts/` قرار گرفت و با `next/font/local` Self-host شد -- بدون وابستگی به CDN خارجی.

## Decision 3: پیاده‌سازی Auth -- Auth.js v5 (Beta) با Keycloak Provider

طبق ADR-0003، Admin Panel باید Authorization Code Flow (Confidential Client، بدون PKCE چون Server-side است) با Token Exchange سمت سرور Next.js داشته باشد -- نه مرورگر. با AskUserQuestion، **Auth.js با Keycloak Provider** به‌جای پیاده‌سازی دستی OIDC انتخاب شد؛ دلیل: OAuth یکی از پرخطرترین حوزه‌ها برای پیاده‌سازی دستی صحیح است (State/Nonce، ذخیره‌ی امن توکن، Refresh Rotation) و طبق اولویت اول پروژه (Security)، استفاده از یک کتابخانه‌ی بلوغ‌یافته بر بازنویسی دستی ترجیح دارد. تنظیمات Realm از Phase 4 (`docs/authentication/keycloak-realm.md`) از قبل مسیر Callback را به سبک Auth.js (`/api/auth/callback/*`) فرض کرده بود -- تأییدی بر تصمیم.

**نکته‌ی مهم کشف‌شده:** next-auth هنوز نسخه‌ی پایدار v5 ندارد (`5.0.0-beta.32` در زمان این فاز) -- ریسک شناخته‌شده و پذیرفته‌شده، مستند در Consequences.

**نکته‌ی دوم:** Auth.js به‌صورت پیش‌فرض برای Provider های OIDC از PKCE استفاده می‌کند، حتی برای Client محرمانه -- این با متن ADR-0003 («بدون PKCE») مغایر به‌نظر می‌رسد اما در عمل یک لایه‌ی امنیتی اضافه (نه جایگزین) است؛ Keycloak آن را چون اختیاری پذیرفته، بدون نیاز به تغییر تنظیمات Client.

### الگوی BFF واقعی

- Session به‌صورت JWT رمزنگاری‌شده در Cookie با HttpOnly (پیش‌فرض Auth.js) -- شامل Access/Refresh Token و نقش‌های Realm (`realm_access.roles`، بدون تأیید امضا -- فقط برای راحتی UI/Redirect، نه تصمیم امنیتی واقعی).
- `src/lib/backend.ts` → `backendFetch()`: تنها نقطه‌ی فراخوانی مستقیم Spring Boot از Server Component/Server Action، با Bearer Token از Session.
- `src/app/api/backend/[...path]/route.ts`: پل BFF یدکی برای Client Componentهای آینده که به تعامل زنده (Polling/فیلتر پیچیده) نیاز داشته باشند -- در این فاز عملاً استفاده نشد چون همه‌ی صفحات با الگوی Server Component + Server Action ساخته شدند (سازگارتر با راهنمای رسمی Next.js 16 برای BFF: `node_modules/next/dist/docs/01-app/02-guides/backend-for-frontend.md`).
- **بدون CORS روی Spring Boot** (تعمدی، از قبل) -- تأییدی که مرورگر هرگز مستقیم به Backend وصل نمی‌شود.

### باگ واقعی کشف‌شده: Race در Refresh Token Rotation

Keycloak با `revokeRefreshToken: true` هر Refresh Token را فقط یک‌بار می‌پذیرد. Next.js 16 در یک بارگذاری صفحه، `auth()` را هم از `proxy.ts` (جایگزین middleware.ts -- رجوع به بخش بعد) هم از رندر RSC (Layout + Page) جداگانه صدا می‌زند -- این‌ها **دو فاز کاملاً مجزای Next.js هستند، نه یک Render واحد**، پس `React.cache()` (که فقط داخل یک Render Dedupe می‌کند) کافی نبود. بدون رفع، دومین صدازدن هم‌زمان با یک Refresh Token از قبل مصرف‌شده رد می‌شد (HTTP 400 از Keycloak) و کل صفحه با خطای احراز هویت ۵۰۰ می‌شد.

**راه‌حل:** یک `Map` سطح‌ماژول (`inFlightRefreshes` در `src/auth.ts`) که در کل Node.js Process مشترک است -- هر Refresh Token واقعی فقط یک‌بار به Keycloak فرستاده می‌شود؛ صداهای هم‌زمان دیگر همان Promise را می‌گیرند. نکته‌ی جانبی: `promise.finally(cb)` یک Promise جدید و مستقل برمی‌گرداند که اگر اصلی Reject شود آن هم Reject می‌شود -- بدون `.catch()` جداگانه روی همان زنجیره، Node.js آن را `unhandledRejection` گزارش می‌داد حتی وقتی خودِ Promise اصلی را فراخوان‌ها به‌درستی گرفته بودند.

## Decision 4: نسخه‌ی Next.js -- 16 (جدیدترین Stable)، نه Pin شده به نسخه‌ی خاص

هم‌راستا با تصمیم مشابه در ADR-0009 (Spring Boot Baseline) -- استفاده از جدیدترین نسخه‌ی رسمی. Next.js 16 چند تغییر Breaking نسبت به نسخه‌های 13-15 دارد که مستقیماً روی این فاز اثر گذاشت:

- **`middleware.ts` منسوخ شده، جایگزین: `proxy.ts`** با Export پیش‌فرض به‌نام `proxy` (نه `middleware`). فایل [`src/proxy.ts`](../../admin-panel/src/proxy.ts) پیاده‌سازی شده. مستندات خود Next.js صراحتاً هشدار می‌دهد Server Action مسیر Proxy را دور می‌زند -- پس هر Server Action/Route Handler حساس (`backendFetch`، `/api/backend/**`) هم جداگانه Session را چک می‌کند (دفاع لایه‌دوم).
- `cookies()`/`headers()`/`params`/`searchParams` کاملاً Async شده‌اند (بدون حالت Sync قدیمی).
- `fetch()` پیش‌فرض بدون Cache است (خوب برای این فاز -- داده‌های Admin همیشه Live لازم دارند).

## Alternatives Considered

| گزینه | چرا رد شد |
|---|---|
| shadcn/ui + Tailwind برای UI | کنترل کامل بر Design ولی نیازمند ساخت دستی Table/DatePicker/Modal از صفر -- کار بیشتر برای پنل با بار زیاد جدول/فرم |
| دست‌ساز فقط با کلاس‌های Design System فعلی | صادق‌ترین گزینه با «بدون Dependency جدید»، اما هزینه‌ی ساخت/نگه‌داری Table/Pagination/Modal از صفر توجیه نداشت |
| پیاده‌سازی دستی OIDC (بدون Auth.js) | ریسک امنیتی پیاده‌سازی نادرست State/PKCE/Refresh Rotation -- در تضاد با اولویت اول پروژه (Security) |
| افزودن Endpoint فهرست برای هر کمبود کشف‌شده (Coupon/Venue/Return) | فراتر از دامنه‌ی صریحاً تأییدشده‌ی این فاز (فقط ۲ Endpoint)؛ راه‌حل‌های محدود در UI جایگزین شد |

## Consequences

**مثبت:** هیچ ماژول Backend موجودی (جز دو Endpoint Read-only کوچک) تغییر نکرد. UI کاملاً RTL/فارسی از پایه، هم‌راستا با Design System. الگوی BFF امنیتی (بدون افشای Token در مرورگر) کامل پیاده شد.

**ریسک/نیازمند توجه:**
- next-auth هنوز Beta است (`5.0.0-beta.32`) -- باید هنگام ارتقا به نسخه‌ی پایدار v5، رفتار Refresh/Session دوباره تست شود.
- Push Notification در Admin Panel اصلاً استفاده نشده (طبق ADR-0015، بدون Provider واقعی).
- محدودیت‌های فهرست (Coupon/Venue/Return/Reward غیرفعال/محصول غیرفعال) در بخش Decision 1 مستند شده‌اند -- در آینده با افزودن یک `GET` ساده به هر ماژول رفع می‌شوند.
- چهار حوزه‌ی Insurance/Travel/Vehicle/Entertainment + Integrations + Audit Logs + Matches فوتبال هنوز بدون UI واقعی‌اند -- منتظر تصمیم/فاز آینده.
