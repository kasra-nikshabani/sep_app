# Infra

محیط توسعه‌ی محلی (Development) با Docker Compose: PostgreSQL، Redis، Keycloak. **این تنظیمات برای Production نیستند** — Deployment واقعی موضوع Phase 20 است.

## پیش‌نیاز: دسترسی Docker

در Phase 0 مشخص شد کاربر فعلی این ماشین به Docker Socket دسترسی ندارد (`permission denied ... docker.sock`). قبل از اجرای دستورات زیر، این را روی ماشین خودتان اجرا کنید (نیاز به sudo دارد، من این کار را خودم انجام نمی‌دهم چون تغییر سطح دسترسی سیستم است):

```bash
sudo usermod -aG docker $USER
```

بعد از این دستور باید یک‌بار از سیستم Log out/Log in کنید (یا `newgrp docker` بزنید) تا عضویت گروه اعمال شود.

## راه‌اندازی

```bash
cd infra
cp .env.example .env
# مقادیر changeme-local-only را در .env با مقادیر واقعی (فقط برای Dev) جایگزین کنید
docker-compose --env-file .env up -d
docker-compose ps
```

سرویس‌ها:

| سرویس | پورت پیش‌فرض | یادداشت |
|---|---|---|
| PostgreSQL | `5432` | دو دیتابیس مجزا می‌سازد: `sepahan_app` (برای Backend، از Phase 6) و `keycloak` |
| Redis | `6379` | با رمز عبور (`--requirepass`) |
| Keycloak | `8080` | حالت `start-dev`؛ Admin Console: `http://localhost:8080` |

## تصمیم‌ها و محدودیت‌های شناخته‌شده‌ی این فاز

- **جداسازی دیتابیس:** Keycloak و Backend روی یک Instance مشترک Postgres اما در دو Database جدا قرار می‌گیرند (`infra/postgres/init/01-databases.sql`) — نه ادغام، نه Instance جدا؛ مطابق [ADR-0007](../docs/adr/0007-database-strategy.md).
- **ریسک شناخته‌شده (فعلاً پذیرفته‌شده برای Dev):** اتصال Keycloak به Postgres در حال حاضر از همان کاربر Superuser استفاده می‌کند، نه یک نقش با حداقل دسترسی (Least Privilege). ساخت یک کاربر اختصاصی محدود به دیتابیس `keycloak` نیازمند تزریق مقدار از `.env` داخل اسکریپت `init` است (پیچیدگی اضافه‌ی غیرضروری برای این فاز)؛ این مورد باید پیش از Phase 20 (Production Deployment) اصلاح شود.
- **Keycloak نسخه‌ی Pin‌شده:** `quay.io/keycloak/keycloak:26.0` — قبل از Phase 4 بررسی کنید که نسخه‌ی پایدارتر منتشر نشده باشد.
- **Phase 4:** فایل Realm (`keycloak/import/sepahan-realm.json`) اضافه شد — با بالا آمدن Keycloak به‌صورت خودکار Import می‌شود. جزئیات و چک‌لیست تأیید: [docs/authentication/keycloak-realm.md](../docs/authentication/keycloak-realm.md).
- هیچ مقدار واقعی (رمز عبور و غیره) در Git commit نشده؛ فقط `.env.example` با مقادیر Placeholder.

## توقف و پاک‌سازی

```bash
docker-compose down          # نگه‌داشتن Volume (داده باقی می‌ماند)
docker-compose down -v       # حذف کامل Volume (داده از بین می‌رود — فقط برای شروع تمیز Dev)
```
