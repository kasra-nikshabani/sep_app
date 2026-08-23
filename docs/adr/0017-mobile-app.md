# ADR-0017: اپ موبایل (Expo + Auth.js-style PKCE با Keycloak)

- **وضعیت:** پذیرفته‌شده (Phase 15)
- **تصمیم‌گیرندگان:** Architect + کارفرما (دو فرک واقعی از طریق AskUserQuestion قبل و حین پیاده‌سازی)

## Context

طبق بند ۱۹ بریف، اپ موبایل (React Native + TypeScript) باید ۱۱ بخش داشته باشد: SSO Login, Home, News, Matches, Tickets, Shop, Services, Wallet, Loyalty, Profile, Notifications. برخلاف Phase 14 (Admin Panel، که در آن Next.js با الگوی BFF مستقر روی سرور کار می‌کند)، اپ موبایل خودش یک Client عمومی/Native OAuth است -- توکن را خودش نگه می‌دارد، نه یک لایه‌ی سروری میانی.

**محدودیت واقعی محیط:** این محیط هیچ Emulator/Simulator موبایل یا دستگاه واقعی ندارد (نه Android SDK، نه Xcode). بدون یک راه جایگزین، امکان تست زنده‌ی تأییدشده‌ی پروژه (طبق روال ثابت‌شده در فازهای قبل) وجود نداشت.

## Decision 1: Expo (نه React Native CLI خالی)

با AskUserQuestion، **Expo** انتخاب شد (توصیه‌شده): `expo start --web` روی react-native-web اپ را در یک مرورگر واقعی کامپایل می‌کند -- دقیقاً همان روشی که برای Admin Panel در Phase 14 استفاده شد، و تنها راهی بود که بدون Android/iOS واقعی می‌شد صفحات را واقعاً کلیک/تست کرد. برای Build واقعی Native در آینده، EAS Build بدون نیاز به Xcode/Android Studio محلی موجود است.

**محدودیت پذیرفته‌شده:** قابلیت‌های کاملاً Native (Push واقعی، دوربین، RTL با `forceRTL`+Reload که طبق مستندات فقط در Dev Client کار می‌کند نه Expo Go) در این فاز تأیید نشدند -- فقط از طریق کد و مستندات رسمی Expo SDK 57 (که فعال Fetch شد، نه از حافظه‌ی آموزشی، چون این نسخه به‌اندازه‌ی کافی جدید بود که رفتارش تفاوت داشته باشد) طراحی شدند.

## Decision 2: محدوده -- واقعی + به‌زودی صادقانه

با AskUserQuestion دوم (بعد از کشف جدید حین پیاده‌سازی، رجوع به بخش بعد)، محدوده نهایی:

- **کامل و متصل به Backend واقعی:** SSO Login، Home، News، Shop (کاتالوگ/سبد/Checkout/پرداخت/سفارش)، Tickets (فقط تئاتر داخلی: رویداد/صندلی/رزرو/پرداخت)، Loyalty (حساب/تراکنش/جایزه/درخواست)، Notifications (فقط ثبت/لغو Device Token -- بدون Provider واقعی Push، طبق ADR-0015)، Profile.
- **«به‌زودی» با دلیل صادقانه:**
  - **Wallet** -- هیچ ماژول Wallet در Backend وجود ندارد (فقط یک مقدار Enum Placeholder، طبق ADR-0002/ADR-0012).
  - **Services** -- همان بیمه/سفر/خودرو/سرگرمی که در Phase 13 بدون Provider واقعی کنار گذاشته شد (ADR-0015).
  - **Matches (بلیط فوتبال)** -- کشف جدید حین همین فاز (رجوع به بخش زیر).

### کشف مهم حین پیاده‌سازی: Django API واقعی برای فوتبال وجود ندارد

فرض اولیه (طبق ADR-0004) این بود که Middleware احراز هویت Bearer در Django (`KeycloakBearerAuthenticationMiddleware`) کافی است تا اپ موبایل مستقیماً با همان Access Token به بلیط فوتبال دسترسی پیدا کند. بررسی واقعی کد Django (`matches/views.py`, `tickets/views.py`, `tickets/urls.py`) نشان داد **کل جریان خرید بلیط فوتبال (مرور مسابقه، انتخاب طبقه/بلوک/ردیف/صندلی، رزرو، پرداخت) کاملاً Viewهای HTML سرور-رندرشده و متکی بر Django Session هستند** -- نه JSON API. تنها Endpoint JSON واقعی (`get_blocks_for_match`) خیلی محدود و بدون Auth است. یعنی میدلوور Bearer فقط مشکل احراز هویت را حل می‌کند؛ خودِ قرارداد API برای این جریان اصلاً وجود ندارد.

ساختن صفحات Native برای این جریان بدون این قرارداد، خودش نوعی حدس‌زدن API بود (ممنوع طبق بند ۸ بریف). با AskUserQuestion، گزینه‌ی «فعلاً به‌زودی در اپ» انتخاب شد -- امن‌ترین گزینه، بدون هیچ چیز حدس‌زده‌شده. راه‌حل واقعی (WebView به همان صفحات موجود، یا ساخت API جدید حداقلی روی Django) موضوع فاز/تصمیم آینده است.

## Decision 3: Auth -- expo-auth-session + Keycloak (Public Client، PKCE)

طبق ADR-0003، Client `mobile-app` از قبل Public + PKCE (S256) + بدون Secret تنظیم شده بود؛ فقط `redirectUris` (Placeholder) نیاز به آپدیت داشت. با همان استدلال Phase 14 (بازنویسی دستی OAuth/PKCE ریسک امنیتی بالایی دارد)، **expo-auth-session** (کتابخانه‌ی رسمی Expo) انتخاب شد.

### باگ واقعی کشف‌شده: Redirect کامل صفحه روی Web، نه Popup+Message

طبق مستندات فعلی Expo SDK 57 (`useProxy` کاملاً حذف شده)، تلاش اول با `promptAsync()` معمولی روی Web با خطای مرورگر `ERR_WEB_BROWSER_BLOCKED` («Popup window was blocked... invoked too long after a user input») رد شد -- چون بین کلیک کاربر و باز شدن Popup حتی یک Microtask (یک `await` ساده برای ذخیره‌ی Code Verifier) هم فاصله می‌انداخت. راه‌حل قابل‌اعتمادتر: **Redirect کامل تمام صفحه** (`window.location.assign`) به‌جای Popup روی Web؛ Native همان `promptAsync()` را حفظ می‌کند (آن‌جا این مشکل مرورگری اصلاً وجود ندارد).

Redirect کامل یعنی نمونه‌ی فعلی `AuthProvider` (و Code Verifier در حافظه‌اش) از بین می‌رود. راه‌حل: Code Verifier قبل از Redirect در Storage ذخیره می‌شود؛ یک مسیر جدید `app/auth/callback.tsx` بعد از بازگشت، دوباره Mount می‌شود و با همان Code Verifier ذخیره‌شده (نه یک نمونه‌ی تازه) Exchange را کامل می‌کند. نکته‌ی دوم: چون `AuthProvider` تازه Mount شده، Discovery Document خودش باید دوباره Fetch شود -- اگر Exchange زودتر از آماده‌شدن آن صدا زده شود همیشه شکست می‌خورد؛ یک پرچم `isDiscoveryReady` این را حل کرد.

### باگ واقعی کشف‌شده: بدون CORS، اپ موبایل (نسخه‌ی Web) اصلاً نمی‌توانست Backend را صدا بزند

برخلاف Admin Panel (الگوی BFF -- مرورگر هرگز مستقیم API را صدا نمی‌زند)، اپ موبایل معماری متفاوتی دارد: خودش مستقیم و با Bearer Token به API وصل می‌شود (الگوی درست و استاندارد برای یک Client موبایل/عمومی). روی Native این هیچ مشکلی ندارد (CORS فقط مفهوم مرورگر است)، اما نسخه‌ی Web (تنها راه تست در این محیط) با خطای CORS کامل مسدود شد چون Backend اصلاً پیکربندی CORS نداشت (عمداً، چون تا این فاز فقط Admin Panel با BFF وجود داشت). یک `CorsConfigurationSource` جدید و محدود (فقط `/api/v1/**`، فقط Originهای صراحتاً مجاز -- نه Wildcard، هرچند برای یک API فقط-Bearer حتی Wildcard هم بی‌خطر بود) به `SecurityConfig.java` اضافه شد.

### باگ واقعی کشف‌شده: Race در JIT Provisioning (اولین‌بار به‌طور واقعی رخ داد)

`UserProvisioningService`/`LoyaltyAccountService` از الگوی «Insert در REQUIRES_NEW، گرفتن DataIntegrityViolationException در صورت باخت مسابقه» استفاده می‌کنند (طبق ADR-0008، از Phase 5). اپ موبایل با React Query چند درخواست را **هم‌زمان واقعی** در بار اول (`/users/me`, `/loyalty/account`, `/news/articles`) می‌فرستد -- دقیقاً همان بار مسابقه‌ای که این الگو برایش طراحی شده بود، اما تا امروز به‌طور واقعی رخ نداده بود. بررسی نشان داد **`save()` ساده Insert را تا لحظه‌ی Commit تراکنش به تعویق می‌اندازد** -- یعنی `DataIntegrityViolationException` بیرون از همان try/catach (بعد از خروج از متد) پرتاب می‌شد و اصلاً گرفته نمی‌شد؛ کاربر یک 500 واقعی می‌دید. رفع با `saveAndFlush` به‌جای `save` در هر دو سرویس (دقیقاً هم‌خانواده‌ی درسی که در Phase 12/13 درباره‌ی `@Modifying`/Flush آموخته شد).

## Decision 4: طراحی بصری -- بدون کتابخانه‌ی UI جدید

برخلاف Admin Panel (Ant Design، چون Table/Form پیچیده لازم بود)، صفحات موبایل عمدتاً لیست/کارت/فرم ساده‌اند و React Native خودش FlatList مجازی‌شده رایگان می‌دهد. مجموعه‌ای کوچک از کامپوننت‌های Themed دست‌ساز (`Button`, `Card`, `Pill`, `ThemedText`, `Screen`) مستقیماً از توکن‌های `docs/architecture/ui-ux/design-system.html` (v0.6) ساخته شد -- بدون Dependency جدید UI. آیکون‌های ناوبری پایین دقیقاً همان مسیرهای SVG موجود در Design System‌اند (کپی‌شده به `react-native-svg`، یک Dependency تقریباً اجباری برای هر آیکون سفارشی در RN، نه یک کتابخانه‌ی کامپوننت). فونت Vazirmatn (سه وزن) از پکیج رسمی npm `vazirmatn` (همان پروژه‌ی متن‌باز rastikerdar که Design System از آن گرفته شده) استخراج و به‌صورت TTF محلی Self-host شد.

RTL: `I18nManager.allowRTL/forceRTL` + `document.documentElement.dir` مستقیم در `index.ts` قبل از Mount شدن هر چیزی تنظیم می‌شود (برای Web این کافی است -- برخلاف Native، Web نیازی به Reload کامل اپ ندارد چون State جاوااسکریپت بین Renderها از بین نمی‌رود).

## Decision 5: مدیریت Data Fetching -- TanStack Query

برخلاف Admin Panel (Server Component + Server Action، چون Next.js لایه‌ی سرور دارد)، اپ موبایل هیچ لایه‌ی سروری ندارد -- همه‌چیز واقعاً سمت Client است. TanStack Query برای Cache/Loading/Error/Invalidation استاندارد صنعت برای دقیقاً این حالت است؛ جایگزین معقول دیگری (SWR) تفاوت معناداری نداشت.

## Alternatives Considered

| گزینه | چرا رد شد |
|---|---|
| React Native CLI خالی | بدون هیچ راه تستِ زنده در این محیط (نه Emulator، نه Simulator) |
| WebView به صفحات فوتبال Django (برای حل فوری Matches) | همان تصمیم صریح کارفرما آن را رد کرد -- «فعلاً به‌زودی» امن‌تر از قفل‌شدن روی یک راه‌حل نیمه‌کاره بود |
| کتابخانه‌ی UI موبایل (React Native Paper/Tamagui) | هویت بصری کاملاً سفارشی (مشکی/طلایی باشگاه) با کتابخانه‌های عمومی (اغلب Material-محور) هم‌خوانی نداشت؛ لیست‌ها/فرم‌های این فاز به‌اندازه‌ی Admin Panel پیچیده نبودند |

## Consequences

**مثبت:** هیچ Endpoint Backend جدیدی این فاز لازم نبود (فقط CORS + دو باگ واقعی رفع شد). سه باگ واقعی کشف‌شده مستقیماً روی درستی/امنیت اثر داشتند و پیش از این فاز کاملاً پنهان بودند.

**ریسک/نیازمند توجه:**
- Build واقعی Native (iOS/Android) در این محیط تست نشد -- قبل از هر EAS Build واقعی، `app.json`’s `forcesRTL: true` باید اضافه شود و کل جریان روی یک Dev Client واقعی تأیید شود.
- Push Notification کاملاً غیرفعال (طبق ADR-0015) -- فقط زیرساخت ثبت/لغو Device Token واقعی است.
- Matches (فوتبال) هنوز بدون راه‌حل -- نیازمند یا WebView یا API جدید روی Django (تصمیم آینده).
- next-auth معادل ندارد این‌جا (expo-auth-session پایدار v5 است، نه Beta) -- ریسک کمتری نسبت به ADR-0016 دارد.
