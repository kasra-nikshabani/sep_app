# ADR-0022: Deployment — Docker Multi-Stage، Caddy، شبکه‌ی داخلی ثابت (Phase 20)

- **وضعیت:** پذیرفته‌شده (Phase 20 -- فاز پایانی طبق فهرست رسمی بریف)
- **تصمیم‌گیرندگان:** Architect + کارفرما (سه تصمیم صریح از طریق AskUserQuestion)

## Context

طبق بند ۲۱ بریف («پروژه باید Dockerized باشد... Deployment Target فعلاً می‌تواند VPS/Cloud
باشد... Infrastructure را طوری طراحی کن که بعداً مهاجرت به Cloud ساده باشد»)، تا پایان
Phase 19 هیچ Dockerfile‌ای برای Backend/Admin Panel وجود نداشت -- هر دو همیشه مستقیم روی
Host اجرا می‌شدند (`mvnw spring-boot:run` / `npm run dev`، طبق روال ثابت این پروژه از
Phase 6/14). `infra/docker-compose.yml` فقط زیرساخت (Postgres/Redis/Keycloak/Observability)
را در بر می‌گرفت. این پروژه هنوز هیچ Git Remote‌ای هم ندارد.

با AskUserQuestion، سه تصمیم صریح از کارفرما گرفته شد:

1. **دامنه‌ی این فاز:** فقط آماده‌سازی (Dockerfile‌ها + `docker-compose.prod.yml` کامل +
   مستندسازی/Runbook) -- بدون Deploy واقعی روی یک سرور اینترنتی (چون هیچ VPS/دامنه‌ی واقعی‌ای
   در اختیار من نیست).
2. **Reverse Proxy:** Caddy (به‌جای Nginx+Certbot یا Traefik) -- HTTPS خودکار با Let's Encrypt،
   کم‌ترین پیکربندی دستی.
3. **Git Remote:** فعلاً محلی بماند -- CI نوشته‌شده‌ی Phase 18 (ADR-0020) همچنان غیرفعال
   می‌ماند تا تصمیم جدا.

## Decision 1: چه چیزی Dockerized می‌شود، چه چیزی نه

**Dockerized:** Backend (Spring Boot، Multi-stage: `eclipse-temurin:21-jdk-alpine` برای Build
→ `eclipse-temurin:21-jre-alpine` برای Runtime) و Admin Panel (Next.js، Multi-stage با
`output: "standalone"` -- الگوی رسمی خودِ Next.js برای این نسخه‌ی نصب‌شده، طبق
`node_modules/next/dist/docs/.../self-hosting.md`).

**عمداً خارج از دامنه‌ی این فاز:**
- **اپ موبایل (Expo):** توزیع آن از اساس مکانیزم دیگری است (App Store/Play Store یا
  EAS Build/Update) -- «Docker روی یک VPS» اصلاً مفهوم متناظری برای آن ندارد؛ نسخه‌ی Web آن هم
  طبق ADR-0017 صرفاً برای Preview/تست محلی است، نه یک محصول قابل‌Deploy جدا.
- **`ticket.sepahansc` (Django):** طبق مرز مستندشده‌ی ADR-0014، این سیستم از قبل مستقل و
  خارج از این Monorepo است (Local-commits-only، بدون Push/Merge بدون تأیید جدا) -- این فاز
  فقط به آدرس/Token واقعی آن (خارج از این Compose) نیاز دارد.

## Decision 2: Caddy + یک شبکه‌ی داخلی با Subnet ثابت -- تنها ورودی عمومی

`infra/Caddyfile` چهار Site Block دارد (Backend/Admin Panel/Keycloak/Grafana، هرکدام روی یک
Subdomain واقعی، آدرس از Env). **به‌جز Caddy (پورت‌های ۸۰/۴۴۳)، هیچ سرویس دیگری پورتی روی
تمام Interfaceهای Host منتشر نمی‌کند** -- Postgres/Redis/Prometheus/Loki همچنان فقط
Loopback (ADR-0021)، Keycloak/Grafana هم از همین فاز به بعد فقط Loopback (پایین‌تر توضیح
داده شده)، Backend/Admin Panel اصلاً پورتی منتشر نمی‌کنند (فقط از طریق نام سرویس روی شبکه‌ی
داخلی Docker در دسترس‌اند). یعنی مسیر واقعی هر Client بیرونی، همیشه از Caddy عبور می‌کند.

یک Subnet ثابت (`172.28.238.0/24`) برای شبکه‌ی پیش‌فرض Compose تعریف شد -- نه فقط برای نظم:
پیش‌نیاز مستقیم Decision 3 پایین است.

`/actuator/prometheus` علاوه بر بررسی IP خودِ Backend (ADR-0021)، در خودِ Caddy هم صریح
`respond 404` می‌شود -- دو لایه‌ی مستقل، نه فقط یکی.

## Decision 3: `server.forward-headers-strategy: native` -- نه FRAMEWORK، نه پیش‌فرض

با یک Reverse Proxy جلوی Backend، بدون این تنظیم، `HttpServletRequest.getRemoteAddr()` همیشه
IP خودِ Caddy (یک Container کناری روی همان شبکه) را برمی‌گرداند -- نه IP واقعی Client. این
دقیقاً همان ریسکی است که در ADR-0021 صریح TODO شده بود («اگر Phase 20 یک Reverse Proxy اضافه
کند و این تنظیم به‌روز نشود، `MonitoringNetworkAuthorizationManager` دیگر نمی‌تواند یک Client
عمومی واقعی را از یک درخواست داخلی تشخیص دهد -- چون هر دو از دید آن از پشت یک IP خصوصی
می‌آیند»).

**چرا NATIVE، نه FRAMEWORK (تفاوت واقعی، نه اسمی):** با WebSearch تأیید شد که `FRAMEWORK`
(فعال‌کننده‌ی `ForwardedHeaderFilter` خودِ Spring) فقط Scheme/Host/Port/Prefix را عوض
می‌کند -- `getRemoteAddr()` دست‌نخورده می‌ماند. فقط `NATIVE` (فعال‌کننده‌ی `RemoteIpValve`
بومی Tomcat) واقعاً `remoteAddr` را بازنویسی می‌کند.

**آیا این کار امن است؟** فقط با اعتماد کورکورانه به X-Forwarded-For خطرناک می‌شد. تأیید شد
(مستقیم از سورس واقعی `ServerProperties.java` نسخه‌ی دقیق نصب‌شده -- ۳.۵.۱۶ -- نه حدس و نه
یک خلاصه‌ی ثانویه‌ی احتمالاً نادرست که ابتدا در جست‌وجوها دیده شد) که `internal-proxies`
پیش‌فرض Tomcat از قبل کل `172.16.0.0/12` (به‌همراه ۱۰/۸، ۱۹۲.۱۶۸/۱۶، ۱۲۷/۸، `100.64.0.0/10`،
Loopback/Link-local IPv6) را می‌پوشاند -- یعنی Subnet ثابت انتخاب‌شده (`172.28.238.0/24`)
خودکار «Immediate Peer مورد اعتماد» است، بدون نیاز به بازنویسی این پیش‌فرض. نتیجه: فقط وقتی
درخواست واقعاً از داخل همان شبکه (یعنی عملاً فقط Caddy) برسد، `X-Forwarded-For` اعتماد
می‌شود؛ یک Client مستقیم (بدون عبور از Caddy) IP واقعی خودش را نشان می‌دهد، نه یک مقدار
جعل‌شده -- چون اصلاً چیزی جز Caddy به این Container روی این شبکه دسترسی ندارد (Decision 2).

## Decision 4: Keycloak در حالت Production واقعی

`start` (نه `start-dev`) + `--import-realm` (طبق مستندات رسمی، اگر Realm از قبل وجود داشته
باشد -- مثلاً بعد از یک Restart -- صرفاً Skip می‌شود، نه Overwrite؛ پس تغییرات زنده‌ی بعدی
پاک نمی‌شوند). TLS روی Caddy Terminate می‌شود (Edge Termination) -- طبق مستندات رسمی، این
یعنی `KC_HTTP_ENABLED=true` الزامی است. `KC_PROXY_HEADERS=xforwarded` +
`KC_PROXY_TRUSTED_ADDRESSES=172.28.238.0/24` (همان Subnet Decision 2/3). `KC_HOSTNAME` روی
آدرس عمومی واقعی (`https://auth.$DOMAIN`).

`KEYCLOAK_ADMIN`/`KEYCLOAK_ADMIN_PASSWORD` طبق مستندات رسمی در Keycloak 26 Deprecated شده‌اند
(جایگزین: `KC_BOOTSTRAP_ADMIN_USERNAME`/`KC_BOOTSTRAP_ADMIN_PASSWORD`) -- هر دو از همان دو
مقدار `.env.prod` ساخته می‌شوند تا کارفرما مجبور به تعریف چهار مقدار تکراری نباشد.

## Decision 5: Next.js `output: "standalone"` + Auth.js `AUTH_TRUST_HOST`

هر دو طبق مستندات رسمی (به‌ترتیب `node_modules/next/dist/docs` همین نسخه‌ی نصب‌شده -- طبق
هشدار خودِ `admin-panel/AGENTS.md` درباره‌ی تفاوت این نسخه با دانش عمومی -- و
`authjs.dev/getting-started/deployment`، نه حدس): `standalone` یک `server.js` مستقل و
حداقلی می‌سازد (بدون نیاز به `node_modules` کامل در ایمیج نهایی)؛ `AUTH_TRUST_HOST=true`
طبق مستندات رسمی «برای Deploy پشت هر Reverse Proxy لازم است» -- بدون آن Auth.js به
`X-Forwarded-Host` اعتماد نمی‌کند و Redirect/Callback واقعی می‌شکند.

`BACKEND_API_BASE_URL` در Prod به آدرس داخلی (`http://backend:8081`) اشاره می‌کند، نه دامنه‌ی
عمومی -- تماس Server Action با Backend کاملاً سرور-به-سرور و داخل همان شبکه است؛ برخلاف
`issuer-uri` خودِ Backend (پایین‌تر)، اینجا هیچ Round-trip اضافه‌ای توجیه‌پذیر نیست.

**استثنا: `issuer-uri` عمومی، نه داخلی.** ادعای `iss` واقعی هر JWT صادرشده دقیقاً همان آدرسی
است که کاربر واقعی از طریق آن وارد شده (`https://auth.$DOMAIN`) -- Backend باید JWKS را هم
از همان آدرس بگیرد، وگرنه اعتبارسنجی امضا/Issuer با یک Round-trip داخلی سریع‌تر ولی نادرست
شکست می‌خورد. هزینه‌ی پذیرفته‌شده: یک Round-trip داخلی→عمومی→داخلی اضافه، فقط در Refresh
دوره‌ای JWKS (نه هر درخواست).

## Decision 6: یک فایل Override (`docker-compose.prod.yml`)، نه یک Stack کامل جدا

Postgres/Redis/Prometheus/Loki/Grafana/Promtail هیچ تفاوت واقعی جز چند مورد مشخص (مسیر
Scrape/محل لاگ/Bind Address) ندارند -- کپی کامل یعنی دو نسخه از هر تغییر بعدی. طبق مستندات
Compose، `environment`/`volumes`/`command` به‌شکل Map یا با Replace کامل Merge می‌شوند (تأیید
شده با `docker-compose config`، نه فرض).

### کشف واقعی حین همین فاز: `ports` قانون Merge متفاوتی دارد

برخلاف `volumes`/`command` (که در تست واقعی درست Replace شدند)، تلاش برای حذف پورت منتشرشده‌ی
Keycloak/Grafana در Prod با `ports: []` در یک Override **شکست خورد** -- روی این نسخه‌ی
نصب‌شده‌ی `docker-compose` (v1.29.2 قدیمی، طبق مستند شناخته‌شده‌ی `infra/README.md`)، مقدار
پایه (`0.0.0.0:8080`) بی‌صدا باقی می‌ماند و مقدار جدید فقط به آن **اضافه** می‌شود (هر دو
هم‌زمان فعال -- یک رگرسیون امنیتی واقعی اگر کشف نمی‌شد، نه یک مسئله‌ی Cosmetic). با
`docker-compose config` مستقیم تأیید و کشف شد، نه فرض گرفته شد.

**رفع:** به‌جای Override کردن `ports` در `docker-compose.prod.yml`، خودِ Bind Address در
Base (`docker-compose.yml`) Env-driven شد: `${KEYCLOAK_BIND_ADDRESS:-0.0.0.0}` /
`${GRAFANA_BIND_ADDRESS:-0.0.0.0}` -- Dev بدون تغییر (پیش‌فرض ۰.۰.۰.۰ همان رفتار قبلی است)،
`infra/.env.prod.example` این دو را روی `127.0.0.1` می‌گذارد. دوباره با `docker-compose
config` روی هر دو مسیر (با/بدون این دو متغیر) تأیید شد.

## Verification (چه چیزی واقعاً تست شد، نه فقط استدلال)

- `npm run build` واقعی admin-panel با `output: standalone` -- خروجی `server.js` + پوشه‌ی
  `standalone` واقعاً ساخته شد.
- `docker build` کامل admin-panel موفق؛ Container واقعی بالا آمد، `HEALTHCHECK` بعد از رفع
  یک نقص واقعی (پایین) به `healthy` رسید؛ درخواست واقعی HTTP به آن (بدون Session) `307`
  به صفحه‌ی ورود برگرداند -- دقیقاً رفتار مورد انتظار.
- `docker build` کامل Backend به‌خاطر ناپایداری شبکه‌ی همین محیط Sandbox (خطای مکرر و
  واقعی TLS `bad_record_mac` روی دانلود Dependencyهای Maven Central، حتی بعد از چند تلاش
  مجدد -- نه یک نقص در Dockerfile) به‌طور کامل از صفر تکمیل نشد؛ به‌جای رد شدن از این
  محدودیت، منطق Dockerfile با یک Build تشخیصی جدا (همان `pom.xml`/`src/` واقعی + یک Cache
  محلی از قبل معتبر Maven، نه یک Mock) تأیید شد -- Jar واقعی Spring Boot با موفقیت ساخته و
  Repackage شد. این محدودیت مخصوص همین Sandbox است؛ یک VPS واقعی با دسترسی عادی به اینترنت
  این مشکل را نخواهد داشت.
- **نقص واقعی کشف‌شده حین تست زنده:** HEALTHCHECK اولیه‌ی admin-panel از `fetch` (Undici) در
  کد Node استفاده می‌کرد -- در Container واقعی این محیط، `fetch`/`wget` هر دو مصرانه
  `ECONNREFUSED` می‌دادند در حالی‌که `nc`/`curl` و ماژول قدیمی‌تر `http` Node به همان
  آدرس/پورت موفق بودند (تفاوت واقعی مشاهده‌شده بین Clientهای HTTP، نه یک فرض). رفع: تغییر
  HEALTHCHECK admin-panel به ماژول `http` توکار Node (بدون Dependency جدید)؛ با تست زنده
  تکراری (`healthy` پایدار) تأیید شد. Backend همچنان از `curl` استفاده می‌کند -- جدا و
  مستقیم تأیید شد که `curl` این مشکل خاص را ندارد (تست با یک Container مستقل).
- `infra/Caddyfile` با `caddy validate` واقعی (نه فقط بازبینی چشمی متن) `Valid configuration`
  گرفت.
- `docker-compose -f docker-compose.yml -f docker-compose.prod.yml config` با مقادیر Dummy
  کامل اجرا و خروجی نهایی (هر سرویس، هر پورت، هر Volume) سطر‌به‌سطر بازبینی شد -- هم مسیر
  Prod (با هر دو فایل) هم مسیر Dev (فقط فایل پایه، با/بدون `.env.example` واقعی) هرکدام
  جدا تأیید شدند.
- Deploy واقعی روی یک سرور اینترنتی **انجام نشد** (تصمیم صریح کارفرما -- بدون VPS/دامنه‌ی
  واقعی در این نشست) -- صدور واقعی گواهی TLS، DNS واقعی، و تست End-to-End زنده هنوز باقی
  مانده‌اند؛ در `docs/deployment/runbook.md` به‌عنوان قدم‌های بعدی مستند شده‌اند.

## Alternatives Considered

| گزینه | چرا رد شد |
|---|---|
| Nginx + Certbot | پیکربندی/تمدید گواهی دستی‌تر؛ کارفرما صریح Caddy را ترجیح داد |
| Traefik | پیچیدگی/یادگیری بیشتر (Label-based Discovery) بدون مزیت واقعی برای این مقیاس (۴ Site Block ثابت، نه ده‌ها Container پویا) |
| `server.forward-headers-strategy: framework` | فقط Scheme/Host را عوض می‌کند، نه `remoteAddr` -- `MonitoringNetworkAuthorizationManager` (ADR-0021) را عملاً بی‌اثر می‌گذاشت |
| اعتماد کامل/بدون‌قید به X-Forwarded-For (بدون محدودسازی شبکه‌ی Docker) | یک Client مستقیم (اگر روزی پورت Backend اشتباهی Publish شود) می‌توانست IP دلخواه جعل کند؛ محدودیت پیش‌فرض Tomcat + شبکه‌ی بسته این ریسک را عملاً حذف می‌کند |
| کپی کامل `docker-compose.yml` برای Prod (به‌جای Override) | دو نسخه از هر سرویس مشترک (Postgres/Redis/...) که باید هم‌زمان به‌روز بمانند |
| `ports: []` برای بستن پورت Keycloak/Grafana در Override | کشف شد که در این نسخه‌ی `docker-compose` واقعاً کار نمی‌کند (Append، نه Replace) -- رجوع به Decision 6 |
| Deploy واقعی همین الان روی یک VPS انتخابی من | بدون VPS/دامنه‌ی واقعی در اختیار؛ کارفرما صریح «فقط آماده‌سازی» را انتخاب کرد |
| Push به یک Git Remote تازه همین الان (برای فعال‌سازی CI) | کارفرما صریح رد کرد؛ تصمیم به فاز/درخواست جدا موکول شد |
| Dockerize کردن اپ موبایل هم | مفهوم متناظر ندارد (توزیع از طریق Store/EAS، نه یک Container سرور) |

## Consequences

**مثبت:** برای اولین بار، `docker compose -f docker-compose.yml -f docker-compose.prod.yml
up -d --build` با یک `.env.prod` واقعی، کل Stack (Backend+Admin Panel+Infra+TLS خودکار) را از
صفر بالا می‌آورد -- بدون نیاز به نصب دستی Java/Node/Maven روی سرور. لایه‌های دفاعی Phase 19
(محدودیت IP، RBAC، دفاع دومیه‌ی Admin Panel) با معماری Reverse Proxy جدید هماهنگ و به‌طور
فعال حفظ شدند (نه صرفاً دست‌نخورده رها شدند) -- ریسک TODO صریح ADR-0021 درباره‌ی
X-Forwarded-For واقعاً بسته شد. یک نقص واقعی Merge در ابزار Compose (نه در طراحی خودِ این
پروژه) کشف و با یک الگوی مستند/تکرارپذیر رفع شد.

**ریسک/نیازمند توجه:** هیچ Deploy واقعی روی اینترنت انجام نشده -- صدور اولین گواهی TLS واقعی
(و رفتار Caddy در برابر یک DNS/Firewall واقعی) اولین‌بار روی یک سرور واقعی آزموده خواهد شد،
نه این‌جا. CI (ADR-0020) و هرگونه CD همچنان کاملاً دستی/غیرفعال است -- بدون Git Remote، هیچ
Pipeline خودکاری Deploy را اجرا نمی‌کند؛ هر Deploy واقعی فعلاً نیازمند دسترسی SSH دستی طبق
Runbook است. Django/اپ موبایل عمداً خارج از این Compose باقی ماندند (طبق مرز از‌قبل‌مستند).
