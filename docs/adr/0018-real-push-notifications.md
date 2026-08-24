# ADR-0018: Push واقعی با FCM مستقیم

- **وضعیت:** پذیرفته‌شده (Phase 16)
- **تصمیم‌گیرندگان:** Architect + کارفرما (دو تصمیم صریح از طریق AskUserQuestion)

## Context

طبق بریف، Phase 16 رسماً «Notifications» است. محتوای واقعی و آماده‌ی آن زمان (SMS.ir، SMTP، دفترکل ارسال، اسکلت Partners) در Phase 13 زیر [ADR-0015](0015-notifications-and-partners.md) از قبل پیاده و تحویل شد -- چون آن فاز بدون این کار عملاً خالی می‌ماند و SMS.ir/SMTP هر دو همان زمان قابل تحقیق و پیاده‌سازی واقعی بودند. تنها بخش ناتمام آن ADR، Push بود: **بدون اپ موبایل واقعی، اتصال FCM/APNs حدس‌زدن یک API بود** -- طبق تصمیم صریح کارفرما همان زمان کنار گذاشته شد و فقط `FakePushProvider` ساخته شد؛ ثبت/لغو Device Token واقعی ماند.

حالا که اپ موبایل واقعی (Phase 15) وجود دارد، این AskUserQuestion دوباره صریح از کارفرما پرسیده شد: فاز ۱۶ باید چه چیزی را پوشش دهد؟ کارفرما **Push واقعی با FCM** را انتخاب کرد.

## Decision 1: FCM مستقیم (نه Expo Push Service)

برای ارسال واقعی، دو مسیر معماری واقعاً وجود داشت که با AskUserQuestion صریح به کارفرما ارائه شد:

| مسیر | مزیت | هزینه |
|---|---|---|
| **FCM مستقیم (انتخاب‌شده)** | هم‌الگوی SMS.ir/SMTP/Zibal -- بدون واسطه‌ی شخص‌ثالث بین Backend و Provider واقعی | نیاز به Dependency جدید (`firebase-admin`) + Service Account از کارفرما؛ فقط Android را واقعاً پوشش می‌دهد (iOS نیاز به حساب پولی Apple Developer دارد که این پروژه ندارد) |
| Expo Push Service | بدون Dependency جدید Backend (یک HTTP POST ساده به `exp.host`)؛ iOS و Android را با هم پوشش می‌دهد | یک واسط شخص‌ثالث (سرورهای Expo) بین Backend و FCM/APNs اضافه می‌کند -- بر خلاف الگوی «بدون واسطه»ی بقیه‌ی Providerهای پروژه |

کارفرما **FCM مستقیم** را انتخاب کرد -- سازگاری با الگوی معماری موجود پروژه بر سادگی پیاده‌سازی ارجحیت داده شد.

## Decision 2: SDK رسمی Google (`firebase-admin`)، نه پیاده‌سازی دستی HTTP v1

FCM HTTP v1 نیازمند یک OAuth2 Access Token تازه از روی Service Account JSON است (جایگزین Server Key قدیمی و ساده‌ی Legacy API که Google آن را کنار گذاشته). پیاده‌سازی دستی این امضا/تبادل توکن هم ریسک امنیتی دارد (اشتباه رایج: پیاده‌سازی نادرست JWT signing) هم نگهداری مداوم می‌خواهد (Google می‌تواند فرمت را تغییر دهد). کتابخانه‌ی رسمی `com.google.firebase:firebase-admin:9.10.0` (تأییدشده به‌عنوان آخرین نسخه‌ی پایدار در Maven Central، بررسی‌شده در همین فاز) این پیچیدگی را کاملاً مخفی می‌کند: `GoogleCredentials.fromStream(...)` + `FirebaseMessaging.getInstance().send(message)`.

طبق قانون «هیچ Dependency جدیدی بدون توجیه»، این نیاز مستقیماً از تصمیم AskUserQuestion بالا (که خودِ گزینه صراحتاً همین Dependency را نام برد) تأیید شده تلقی شد.

## پیاده‌سازی Backend

- `FirebaseCloudMessagingPushProvider implements PushProvider` -- فقط وقتی `sepahan.notifications.push.provider=fcm` باشد فعال می‌شود (`@ConditionalOnProperty`، هم‌الگوی `SmsIrProvider`)؛ پیش‌فرض همچنان `fake` است (بدون تماس شبکه‌ای/هزینه‌ی واقعی، هم‌الگوی FakeSmsProvider/FakePaymentProvider).
- `FirebaseMessagingConfig` -- `FirebaseApp`/`FirebaseMessaging` را فقط وقتی Provider واقعاً `fcm` باشد می‌سازد؛ بدون این شرط، نبود فایل Service Account (طبیعی در توسعه‌ی محلی) کل Startup برنامه را می‌شکست، نه فقط یک Provider را غیرفعال می‌کرد.
- **رفع یک نقص واقعی کشف‌شده حین همین فاز:** FCM برای Tokenهای مربوط به اپ‌های حذف‌شده کد خطای مشخص `UNREGISTERED` برمی‌گرداند. بدون تشخیص این حالت، `NotificationService` دوباره و دوباره به همان Token مرده تلاش می‌کرد. یک استثنای اختصاصی (`DeviceTokenUnregisteredException`) و مسیر جدا در `NotificationService.sendPushToUser` (که برخلاف `sendPush` تک‌توکنی، مستقیم به Entity دسترسی دارد) این حالت را می‌گیرد و `DeviceToken.active=false` می‌کند -- بدون شکستن قرارداد مستندشده‌ی `attempt()` («ارسال هرگز Exception به فراخوانی‌کننده پرتاب نمی‌کند»، که برای sendSms/sendEmail دست‌نخورده ماند).
- Resilience4j (Circuit Breaker/Retry/Timeout) برای `fcm` هم‌الگوی `sms_ir`/`zibal` اضافه شد.

## کشف جانبی: نقص عمیق‌تر و واقعی در الگوی JIT Provisioning (Self-Invocation)

حین نوشتن تست هم‌زمانی واقعی برای اثبات درستی Push (تست کردن این‌که ثبت هم‌زمان چند Device Token شکست نمی‌خورد)، یک باگ واقعی و مستقل کشف شد که به Push هیچ ربطی ندارد اما همان زیرساخت JIT Provisioning (ADR-0008، Phase 5) را که Phase 15 قبلاً یک‌بار در آن `saveAndFlush` را رفع کرده بود، دوباره درگیر می‌کرد -- این‌بار عمیق‌تر.

**علائم:** با یک تست `@SpringBootTest` واقعی (نه Mock) که چند Thread واقعاً هم‌زمان (با `CountDownLatch`، نه صرفاً نزدیک‌به‌هم) `ensureUserForToken` را برای یک کاربر کاملاً تازه صدا می‌زدند، همچنان اکثر تلاش‌ها با ۵۰۰ (`SQLState=25P02`) شکست می‌خوردند -- حتی بعد از رفع Phase 15 (saveAndFlush).

**ریشه‌ی واقعی:** `createUserSafely`/`createFanProfileSafely` (در `UserProvisioningService`) و `createAccountSafely`/`tryInsertTransaction` (در `LoyaltyAccountService`) با `@Transactional(propagation = REQUIRES_NEW)` علامت‌گذاری شده بودند، اما از *داخل همان کلاس* (Self-Invocation، یعنی `this.method()` نه از طریق یک Bean دیگر) صدا زده می‌شدند. طبق مستندات رسمی اسپرینگ، در حالت Proxy-based AOP (پیش‌فرض Spring Boot)، `@Transactional` **فقط روی فراخوانی‌هایی که از طریق Proxy رد می‌شوند اثر دارد** -- Self-Invocation کاملاً از این مکانیزم عبور می‌کند، بدون هیچ خطا یا هشداری. نتیجه: REQUIRES_NEW این‌جا از همان روز اول (Phase 5) هرگز واقعاً یک Transaction/Connection مستقل نساخته بود؛ کل `ensureUserForToken`/`getOrCreateAccount` روی یک Transaction مشترک اجرا می‌شدند. وقتی یک Thread مسابقه را می‌باخت (Insert با Unique Constraint رد می‌شد)، همان یک Transaction مشترک تا پایان عمرش «Aborted» می‌ماند و هر Query بعدی -- حتی SELECT بازیابی در catch -- با ۵۰۰ رد می‌شد.

چرا در Phase 15 (با ۲-۳ درخواست نزدیک‌به‌هم از یک مرورگر واقعی، نه یک تست با هم‌زمانی دقیق) این مشکل «حل‌شده» به‌نظر رسید: تایمینگ واقعی شبکه/مرورگر به‌ندرت دو درخواست را *دقیقاً* هم‌زمان می‌رساند؛ معمولاً یکی زودتر Commit می‌شد و درخواست دوم رکورد را از قبل موجود می‌دید (مسیر خوش‌بینانه، بدون رسیدن به شاخه‌ی Insert اصلاً). یک تست واقعی با `CountDownLatch` این پنجره‌ی شانسی را حذف کرد و نقص واقعی را قطعی و همیشگی نشان داد.

**رفع:** چهار متد به دو Bean کوچک و جدا منتقل شدند (`ir.sepahan.app.users.JitInsertHelper`، `ir.sepahan.app.loyalty.LoyaltyJitInsertHelper`) -- فراخوانی از `UserProvisioningService`/`LoyaltyAccountService` به این Beanها اکنون واقعاً از طریق Proxy اسپرینگ رد می‌شود، پس REQUIRES_NEW این‌بار به‌طور واقعی یک Transaction/Connection مستقل می‌سازد. try/catch برای گرفتن `DataIntegrityViolationException` هم به فراخوانی‌کننده منتقل شد (نه داخل خودِ متد REQUIRES_NEW) -- چون Session/Persistence Context Hibernate بعد از شکست Flush برای بقیه‌ی عمر همان Transaction غیرقابل‌استفاده می‌ماند؛ Exception باید بدون دست‌کاری از متد REQUIRES_NEW خارج شود تا اسپرینگ آن Transaction را درست Rollback/ببندد.

**اثر جانبی واقعی دیگر، هم کشف‌شده در همین بررسی:** حالا که REQUIRES_NEW واقعاً یک Connection دوم و هم‌زمان لازم دارد، Pool پیش‌فرض HikariCP (۱۰) زیر بار هم‌زمانی واقعی (مثلاً تست موجود Loyalty با ۲۰ Thread هم‌زمان) کم می‌آورد. `maximum-pool-size: 30` در `application.yml` اضافه شد (Postgres این محیط تا ۱۰۰ Connection را پشتیبانی می‌کند). تست ۲۰-Threadی موجود هم بازطراحی شد: بذر امتیاز هر کاربر (که خودش JIT حساب را فعال می‌کرد) از *قبل* خط شروع هم‌زمانی منتقل شد، چون آن‌چه واقعاً سنجیده می‌شود رقابت روی موجودی جایزه است، نه ساخت هم‌زمان حساب.

**تست:** `UserProvisioningServiceTest` (جدید) و `LoyaltyAccountServiceTest.getOrCreateAccount_concurrentFirstAccess_...` (جدید) هر دو با Thread واقعاً هم‌زمان (`CountDownLatch`) این رفع را اثبات می‌کنند -- ۵ اجرای پیاپی هرکدام کاملاً سبز.

## پیاده‌سازی موبایل

- `expo-notifications` نصب و به `plugins` در `app.json` اضافه شد.
- طبق مستندات رسمی Expo SDK 57 (بررسی‌شده در همین فاز): **`getDevicePushTokenAsync()` (Native Token واقعی برای FCM/APNs) روی Web اصلاً پیاده نشده** (فقط `ios`/`android`) -- برخلاف فرض اولیه‌ی این پروژه که «حالا اپ موبایل واقعی داریم پس می‌شود در همین محیط Web تست کرد». صفحه‌ی اعلان‌ها این محدودیت را صادقانه نشان می‌دهد: روی Web پیام «فقط در Build واقعی Native در دسترس است» و روی iOS/Android (که در این محیط قابل Build نیستند -- محدودیت پذیرفته‌شده‌ی Phase 15) کد واقعی درخواست Permission + دریافت Token + ثبت در Backend.
- همچنین طبق مستندات: **از SDK 53، Push در Expo Go کلاً غیرفعال است** -- حتی اگر این محیط Emulator داشت، تست واقعی نیازمند یک Dev Client Build بود، نه صرفاً نصب Expo Go.

## محدودیت پذیرفته‌شده: بدون تست زنده‌ی واقعی دریافت Push

این محیط نه Emulator/دستگاه دارد (محدودیت Phase 15) نه امکان اجرای `getDevicePushTokenAsync()` روی Web (کشف همین فاز). یعنی **در هیچ مسیری امکان تست زنده‌ی دریافت واقعی یک Push روی یک دستگاه واقعی وجود نداشت** -- این محدودیت پیش از پیاده‌سازی صریحاً در AskUserQuestion دوم به کارفرما گفته شد و پذیرفته شد. آنچه واقعاً تأیید شد:
- Backend با `sepahan.notifications.push.provider=fake` (پیش‌فرض) بدون تغییر رفتار بالا می‌آید؛ Suite کامل تست سبز ماند.
- کد `FirebaseCloudMessagingPushProvider`/`FirebaseMessagingConfig` Compile می‌شود و منطق خطا (`UNREGISTERED`) طبق مستندات رسمی نوشته شده -- نه حدس.
- صفحه‌ی اعلان‌ها روی نسخه‌ی Web به‌درستی پیام محدودیت را نشان می‌دهد و کرش نمی‌کند (چون کد واقعی `expo-notifications` هرگز روی Web فراخوانی نمی‌شود).

## پیش‌نیاز قبل از Production (هنوز انجام‌نشده -- هم‌الگوی Zibal در Phase 9 / SMTP در Phase 13)

برای فعال‌سازی واقعی لازم است کارفرما:
1. یک پروژه‌ی Firebase بسازد (اگر ندارد) و یک اپ Android داخل آن با همان Package Name اپ موبایل ثبت کند.
2. از Firebase Console → Project Settings → Service Accounts، یک Private Key JSON بگیرد.
3. آن فایل را جایی خارج از Git نگه دارد و مسیرش را در `FCM_SERVICE_ACCOUNT_PATH` تنظیم کند؛ `sepahan.notifications.push.provider=fcm` را فعال کند.
4. برای اولین Build واقعی Native (EAS)، `google-services.json` همان پروژه را طبق مستندات EAS اضافه کند -- این یک نیاز جدا و مخصوص زیرساخت Build خودِ Expo/EAS است، ربطی به Service Account سمت Backend ندارد.

## Alternatives Considered

| گزینه | چرا رد شد |
|---|---|
| Expo Push Service | واسط شخص‌ثالث اضافه؛ کارفرما صراحتاً الگوی «بدون واسطه» را ترجیح داد |
| پیاده‌سازی دستی OAuth2/JWT برای HTTP v1 خام | ریسک امنیتی پیاده‌سازی نادرست امضا + نگهداری مداوم؛ کتابخانه‌ی رسمی Google دقیقاً همین را حل می‌کند |
| صبر تا Build واقعی Native برای پیاده‌سازی Push | این فاز را عملاً بی‌محتوا می‌کرد؛ Backend کاملاً مستقل از وجود Build واقعی قابل ساخت/تست بود |

## Consequences

**مثبت:** Backend اکنون یک مسیر واقعی و کامل Push دارد که فقط منتظر یک Credential واقعی است (نه هیچ کد ناقصی)؛ یک نقص واقعی (Token مرده‌ی تکرارشونده) پیش از وقوع در Production شناسایی و رفع شد. مهم‌تر: یک نقص عمیق‌تر و مستقل (Self-Invocation، از Phase 5 در کد موجود بوده) در JIT Provisioning کاربر و حساب وفاداری کشف و رفع شد -- این نقص تا امروز کشف نشده بود چون هیچ تست موجودی هم‌زمانی *دقیق* (با CountDownLatch) روی یک رکورد مشترک نداشت.

**ریسک/نیازمند توجه:** iOS از این مسیر پوشش داده نمی‌شود مگر پروژه در آینده حساب Apple Developer تهیه کند و APNs را جدا اضافه کند؛ کل مسیر سمت کلاینت تا اولین Build واقعی EAS عملاً تست‌نشده می‌ماند. افزایش Pool به ۳۰ یک تصمیم محافظه‌کارانه‌ی سریع بود، نه یک Capacity Planning کامل -- اندازه‌ی نهایی موضوع فازهای Observability/Performance بعدی است.
