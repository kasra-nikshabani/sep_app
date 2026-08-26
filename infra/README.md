# Infra

محیط توسعه‌ی محلی (Development) با Docker Compose: PostgreSQL، Redis، Keycloak، Prometheus/Grafana/Loki/Promtail (Observability، Phase 17). این فایل (`docker-compose.yml`) برای **Development** است -- برای Production، `docker-compose.prod.yml` روی همین فایل Override می‌شود (Phase 20، پایین‌تر).

> **وضعیت:** این Stack به‌صورت واقعی بالا آورده شد و تست شد (نه فقط بررسی Syntax) — هر سه سرویس `Up (healthy)` هستند، Realm `sepahan` با موفقیت Import شد (۴ Client، ۵ نقش، Client Scope `fan-identity` با هر دو Protocol Mapper تأیید شدند مستقیماً از دیتابیس Keycloak).

## پیش‌نیاز: دسترسی Docker

اگر با خطای `permission denied ... docker.sock` مواجه شدید (کاربر فعلی عضو گروه `docker` نیست)، این را در ترمینال خودتان اجرا کنید:

```bash
sudo usermod -aG docker $USER
```

⚠️ **نکته‌ی مهم که در راه‌اندازی اول این پروژه کشف شد:** فقط باز کردن یک ترمینال جدید کافی نیست — چون هر ترمینال گروه‌های خودش را از Session دسکتاپی که از آن باز شده به ارث می‌برد، نه از فایل `/etc/group` زنده. برای اعمال شدن واقعی عضویت گروه، باید **کامل از سیستم Log out و دوباره Log in کنید** (یا ساده‌تر: `reboot`). بعد از آن `groups` باید `docker` را نشان بدهد.

## راه‌اندازی

```bash
cd infra
cp .env.example .env
# مقادیر changeme-local-only را در .env با مقادیر واقعی (فقط برای Dev) جایگزین کنید
docker-compose --env-file .env up -d
docker-compose ps
./keycloak/fix-user-profile.sh    # اجباری — بدون این هیچ کاربری نمی‌تواند وارد شود؛ دلیل در docs/authentication/keycloak-realm.md
```

⚠️ اگر مقدار رمزی در `.env` کاراکتر `$` یا `` ` `` یا `"` داشته باشد (مثلاً یک رمز پیچیده‌ی Admin)، آن را داخل تک‌کوتیشن بگذارید (`KEYCLOAK_ADMIN_PASSWORD='...'`) — وگرنه اگر جایی این فایل با `source` خوانده شود، آن کاراکترها به‌عنوان کد Shell تفسیر می‌شوند و رمز را بی‌صدا خراب می‌کنند (در Phase 6 دقیقاً همین اتفاق افتاد).

**اگر بعد از بالا آمدن، رمز Admin که در `.env` گذاشتید کار نکرد:** طبیعی است — طبق نکته‌ی زیر (بخش Keycloak Realm)، رمز Admin فقط در همان اولین Boot واقعی اعمال می‌شود، نه در هر تغییر بعدی `.env`. برای اطمینان از هماهنگی کامل، `docker-compose down -v && docker-compose --env-file .env up -d` را با `.env` نهایی خود اجرا کنید.

سرویس‌ها:

| سرویس | پورت پیش‌فرض | یادداشت |
|---|---|---|
| PostgreSQL | `127.0.0.1:5433` (نه ۵۴۳۲) | دو دیتابیس مجزا می‌سازد: `sepahan_app` (برای Backend، از Phase 6) و `keycloak`؛ پورت ۵۴۳۲ عمداً استفاده نشد چون یک PostgreSQL سیستمی از قبل روی این ماشین در حال اجراست |
| Redis | `127.0.0.1:6380` (نه ۶۳۷۹) | با رمز عبور (`--requirepass`)؛ همان دلیل بالا برای تغییر پورت |
| Keycloak | `8080` | حالت `start-dev`؛ Admin Console: `http://localhost:8080` -- عمداً روی `0.0.0.0` باقی مانده (پشت Login واقعی است، طبق ADR-0021) |
| Prometheus | `127.0.0.1:9090` | Scrape خودکار Backend روی `host.docker.internal:8081/actuator/prometheus` -- Backend باید جدا (روی Host، طبق `backend/README.md`) بالا باشد |
| Grafana | `3030` (نه ۳۰۰۰ -- Admin Panel همان پورت را می‌خواهد) | `http://localhost:3030`، ورود با `GRAFANA_ADMIN_USER`/`GRAFANA_ADMIN_PASSWORD` از `.env`؛ Datasourceها و دو Dashboard (`Backend Overview`, `Business Metrics`) خودکار Provision می‌شوند؛ عمداً روی `0.0.0.0` باقی مانده (پشت Login واقعی است، طبق ADR-0021) |
| Loki | `127.0.0.1:3100` | فقط توسط Promtail/Grafana استفاده می‌شود؛ حالت Filesystem تک‌باینری -- فقط توسعه (ADR-0019) |

**نکته‌ی Phase 19 (ممیزی امنیتی):** پورت‌های PostgreSQL/Redis/Prometheus/Loki از `"host:port"` به `"127.0.0.1:host:port"` تغییر کردند -- دیگر از ماشین‌های دیگر همان شبکه در دسترس نیستند (بدون هیچ اثری روی ارتباط بین‌Containeری یا دسترسی از `localhost` خودِ همین Host). Keycloak/Grafana عمداً کنار گذاشته شدند چون هر دو پشت یک صفحه‌ی ورود واقعی هستند. Realm `sepahan` هم سخت‌تر شد: یک Redirect URI اشتباه Client موبایل اصلاح و Password Policy تقویت شد (طول حداقل، پیچیدگی، تاریخچه). جزئیات کامل در [ADR-0021](../docs/adr/0021-security-audit.md).

**نکته‌ی Phase 17:** Prometheus/Loki/Promtail/Grafana به این Stack اضافه شدند. جزئیات کامل معماری (چرا خودمیزبان، چرا Correlation ID سبک به‌جای OpenTelemetry، چرا Promtail یک فایل روی Host را می‌خواند نه Docker Socket) در [ADR-0019](../docs/adr/0019-observability.md).

## تصمیم‌ها و محدودیت‌های شناخته‌شده‌ی این فاز

- **جداسازی دیتابیس:** Keycloak و Backend روی یک Instance مشترک Postgres اما در دو Database جدا قرار می‌گیرند (`infra/postgres/init/01-databases.sql`) — نه ادغام، نه Instance جدا؛ مطابق [ADR-0007](../docs/adr/0007-database-strategy.md).
- **رفع‌شده در Phase 20:** اتصال Keycloak به Postgres قبلاً از کاربر Superuser استفاده می‌کرد؛ حالا یک نقش اختصاصی `keycloak` (فقط مالک دیتابیس `keycloak`، هم‌الگوی `sepahan_backend`) این کار را انجام می‌دهد (`infra/postgres/init/03-create-keycloak-role.sh`، ADR-0022). ⚠️ چون این اسکریپت فقط در اولین `initdb` اجرا می‌شود، روی یک Volume Postgres از‌قبل‌موجود اعمال نمی‌شود — برای اعمال کامل روی یک محیط Dev قدیمی: `docker-compose down -v && docker-compose --env-file .env up -d` (هم‌الگوی نکته‌ی رمز Admin بالا؛ داده‌ی Dev از بین می‌رود، فقط برای شروع تمیز).
- **Phase 6:** Backend یک نقش اختصاصی Postgres دارد (`sepahan_backend`) که فقط مالک دیتابیس `sepahan_app` است — نه Superuser، و صریحاً از اتصال به دیتابیس `keycloak` هم منع شده (`REVOKE CONNECT ... FROM PUBLIC`، چون Postgres پیش‌فرض به همه‌ی نقش‌ها اجازه‌ی Connect به هر دیتابیسی را می‌دهد). رمز آن در `.env` به‌نام `BACKEND_DB_PASSWORD` است.
- **Keycloak نسخه‌ی Pin‌شده:** `keycloak/keycloak:26.7.1` روی **Docker Hub** (نه `quay.io`). نسخه‌ی اولیه‌ی ۲۶.۰ از Registry حذف شده بود؛ در تلاش بعدی هم `quay.io` مدام ۴۰۳ Forbidden روی دانلود Layer می‌داد (احتمالاً محدودیت دسترسی شبکه‌ای به quay.io) در حالی‌که Postgres/Redis از Docker Hub بدون مشکل Pull شدند — پس Image رسمی Keycloak را از Docker Hub (که Mirror فعال و به‌روز دارد) گرفتیم.
- **Phase 4:** فایل Realm (`keycloak/import/sepahan-realm.json`) اضافه شد و با موفقیت Import و تأیید شد. جزئیات کامل، باگ‌های پیداشده در این مسیر، و نکته‌ی مهم درباره‌ی رمز Admin: [docs/authentication/keycloak-realm.md](../docs/authentication/keycloak-realm.md).
- هیچ مقدار واقعی (رمز عبور و غیره) در Git commit نشده؛ فقط `.env.example` با مقادیر Placeholder.
- **`docker-compose` نسخه‌ی نصب‌شده روی این ماشین قدیمی و کنارگذاشته‌شده است (v1.29.2، نه Plugin رسمی `docker compose`).** یک باگ شناخته‌شده‌ی این نسخه با Docker Engine جدید دارد: هنگام Recreate کردن یک Container موجود (نه ساخت از صفر) با خطای `KeyError: 'ContainerConfig'` مواجه می‌شود. راه‌حل: قبل از `up`، Container مشکل‌دار را با `docker rm -f <name>` پاک کنید تا از مسیر «صفر تا صد بساز» عبور کند، نه «Recreate». نصب Docker Compose Plugin رسمی (`docker compose`) این مشکل را کامل حل می‌کند ولی نصب Package جدید نیاز به تأیید شماست.

## Production (Phase 20، ADR-0022)

`docker-compose.prod.yml` روی همین فایل Override می‌شود -- Backend/Admin Panel را از Dockerfile خودشان Build می‌کند، یک Caddy به‌عنوان تنها ورودی عمومی اضافه می‌کند، و Keycloak/Grafana را فقط Loopback می‌کند (بقیه از قبل Phase 19). این فاز فقط Artifact‌ها را ساخته و محلی تأیید کرده -- **هیچ Deploy واقعی روی یک سرور انجام نشده**. راهنمای کامل قدم‌به‌قدم: [docs/deployment/runbook.md](../docs/deployment/runbook.md).

```bash
cp .env.prod.example .env.prod   # پر کردن مقادیر واقعی -- رجوع به Runbook
docker-compose -f docker-compose.yml -f docker-compose.prod.yml --env-file .env.prod up -d --build
```

⚠️ کشف این فاز: کلید `ports` در یک فایل Override روی این نسخه‌ی `docker-compose` (v1.29.2) Replace نمی‌شود، فقط Append -- به همین دلیل محدودسازی Loopback Keycloak/Grafana در Prod با دو متغیر Env (`KEYCLOAK_BIND_ADDRESS`/`GRAFANA_BIND_ADDRESS`، پیش‌فرض `0.0.0.0` بدون تغییر رفتار Dev) در خودِ این فایل پیاده شده، نه در `docker-compose.prod.yml`.

## توقف و پاک‌سازی

```bash
docker-compose down          # نگه‌داشتن Volume (داده باقی می‌ماند)
docker-compose down -v       # حذف کامل Volume (داده از بین می‌رود — فقط برای شروع تمیز Dev)
```
