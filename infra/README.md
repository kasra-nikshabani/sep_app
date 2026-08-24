# Infra

محیط توسعه‌ی محلی (Development) با Docker Compose: PostgreSQL، Redis، Keycloak، Prometheus/Grafana/Loki/Promtail (Observability، Phase 17). **این تنظیمات برای Production نیستند** — Deployment واقعی موضوع Phase 20 است.

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
| PostgreSQL | `5433` (نه ۵۴۳۲) | دو دیتابیس مجزا می‌سازد: `sepahan_app` (برای Backend، از Phase 6) و `keycloak`؛ پورت ۵۴۳۲ عمداً استفاده نشد چون یک PostgreSQL سیستمی از قبل روی این ماشین در حال اجراست |
| Redis | `6380` (نه ۶۳۷۹) | با رمز عبور (`--requirepass`)؛ همان دلیل بالا برای تغییر پورت |
| Keycloak | `8080` | حالت `start-dev`؛ Admin Console: `http://localhost:8080` |
| Prometheus | `9090` | Scrape خودکار Backend روی `host.docker.internal:8081/actuator/prometheus` -- Backend باید جدا (روی Host، طبق `backend/README.md`) بالا باشد |
| Grafana | `3030` (نه ۳۰۰۰ -- Admin Panel همان پورت را می‌خواهد) | `http://localhost:3030`، ورود با `GRAFANA_ADMIN_USER`/`GRAFANA_ADMIN_PASSWORD` از `.env`؛ Datasourceها و دو Dashboard (`Backend Overview`, `Business Metrics`) خودکار Provision می‌شوند |
| Loki | `3100` | فقط توسط Promtail/Grafana استفاده می‌شود؛ حالت Filesystem تک‌باینری -- فقط توسعه (ADR-0019) |

**نکته‌ی Phase 17:** Prometheus/Loki/Promtail/Grafana به این Stack اضافه شدند. جزئیات کامل معماری (چرا خودمیزبان، چرا Correlation ID سبک به‌جای OpenTelemetry، چرا Promtail یک فایل روی Host را می‌خواند نه Docker Socket) در [ADR-0019](../docs/adr/0019-observability.md).

## تصمیم‌ها و محدودیت‌های شناخته‌شده‌ی این فاز

- **جداسازی دیتابیس:** Keycloak و Backend روی یک Instance مشترک Postgres اما در دو Database جدا قرار می‌گیرند (`infra/postgres/init/01-databases.sql`) — نه ادغام، نه Instance جدا؛ مطابق [ADR-0007](../docs/adr/0007-database-strategy.md).
- **ریسک شناخته‌شده (فعلاً پذیرفته‌شده برای Dev):** اتصال Keycloak به Postgres همچنان از کاربر Superuser استفاده می‌کند، نه یک نقش Least-Privilege — این مورد باید پیش از Phase 20 (Production Deployment) اصلاح شود.
- **Phase 6:** Backend یک نقش اختصاصی Postgres دارد (`sepahan_backend`) که فقط مالک دیتابیس `sepahan_app` است — نه Superuser، و صریحاً از اتصال به دیتابیس `keycloak` هم منع شده (`REVOKE CONNECT ... FROM PUBLIC`، چون Postgres پیش‌فرض به همه‌ی نقش‌ها اجازه‌ی Connect به هر دیتابیسی را می‌دهد). رمز آن در `.env` به‌نام `BACKEND_DB_PASSWORD` است.
- **Keycloak نسخه‌ی Pin‌شده:** `keycloak/keycloak:26.7.1` روی **Docker Hub** (نه `quay.io`). نسخه‌ی اولیه‌ی ۲۶.۰ از Registry حذف شده بود؛ در تلاش بعدی هم `quay.io` مدام ۴۰۳ Forbidden روی دانلود Layer می‌داد (احتمالاً محدودیت دسترسی شبکه‌ای به quay.io) در حالی‌که Postgres/Redis از Docker Hub بدون مشکل Pull شدند — پس Image رسمی Keycloak را از Docker Hub (که Mirror فعال و به‌روز دارد) گرفتیم.
- **Phase 4:** فایل Realm (`keycloak/import/sepahan-realm.json`) اضافه شد و با موفقیت Import و تأیید شد. جزئیات کامل، باگ‌های پیداشده در این مسیر، و نکته‌ی مهم درباره‌ی رمز Admin: [docs/authentication/keycloak-realm.md](../docs/authentication/keycloak-realm.md).
- هیچ مقدار واقعی (رمز عبور و غیره) در Git commit نشده؛ فقط `.env.example` با مقادیر Placeholder.
- **`docker-compose` نسخه‌ی نصب‌شده روی این ماشین قدیمی و کنارگذاشته‌شده است (v1.29.2، نه Plugin رسمی `docker compose`).** یک باگ شناخته‌شده‌ی این نسخه با Docker Engine جدید دارد: هنگام Recreate کردن یک Container موجود (نه ساخت از صفر) با خطای `KeyError: 'ContainerConfig'` مواجه می‌شود. راه‌حل: قبل از `up`، Container مشکل‌دار را با `docker rm -f <name>` پاک کنید تا از مسیر «صفر تا صد بساز» عبور کند، نه «Recreate». نصب Docker Compose Plugin رسمی (`docker compose`) این مشکل را کامل حل می‌کند ولی نصب Package جدید نیاز به تأیید شماست.

## توقف و پاک‌سازی

```bash
docker-compose down          # نگه‌داشتن Volume (داده باقی می‌ماند)
docker-compose down -v       # حذف کامل Volume (داده از بین می‌رود — فقط برای شروع تمیز Dev)
```
