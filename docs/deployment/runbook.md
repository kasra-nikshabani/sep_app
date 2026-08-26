# راهنمای Deployment واقعی (Phase 20، ADR-0022)

این سند قدم‌به‌قدم اولین Deploy واقعی روی یک VPS را توضیح می‌دهد. طبق تصمیم صریح کارفرما در
همین فاز، **این فاز خودش هیچ سروری را واقعاً Deploy نکرده** -- فقط تمام Artifact لازم
(Dockerfile‌ها، `docker-compose.prod.yml`، `Caddyfile`) ساخته و محلی تأیید شده‌اند. این سند
برای روزی است که یک VPS/دامنه‌ی واقعی در دسترس باشد.

## پیش‌نیازها

- یک VPS با Docker Engine + `docker-compose` (یا Plugin رسمی `docker compose`) نصب‌شده.
- چهار Subdomain واقعی که همه به IP همین سرور اشاره کنند (رکورد A/AAAA در DNS):
  - `api.<domain>` -- Backend
  - `admin.<domain>` -- Admin Panel
  - `auth.<domain>` -- Keycloak
  - `grafana.<domain>` -- Grafana
- پورت‌های ۸۰ و ۴۴۳ از اینترنت به این سرور باز باشند (برای HTTPS خودکار Caddy از طریق
  Let's Encrypt -- چالش HTTP-01).
- آدرس واقعی و Token مشترک `ticket.sepahansc/football_tickets` (طبق ADR-0014؛ Django بخشی
  از این Compose نیست).

## قدم ۱: کلون + رازها

```bash
git clone <repo-url> && cd sepapp/infra
cp .env.prod.example .env.prod
```

مقادیر `.env.prod` را پر کنید:
- رمزهای Postgres/Redis/Keycloak/Grafana: با `openssl rand -base64 24` بسازید.
- `ADMIN_PANEL_AUTH_SECRET`: با `openssl rand -base64 32`.
- چهار دامنه‌ی واقعی + `CADDY_ACME_EMAIL`.
- `LOYALTY_DJANGO_SERVICE_TOKEN`/`INFRA_DJANGO_BASE_URL`: مقادیر واقعی Django.
- `ADMIN_PANEL_KEYCLOAK_CLIENT_SECRET` را فعلاً خالی بگذارید -- در قدم ۴ به دست می‌آید.

## قدم ۲: اولین بالا آمدن

```bash
docker compose -f docker-compose.yml -f docker-compose.prod.yml --env-file .env.prod up -d --build
```

اولین بار چند دقیقه طول می‌کشد (Build دو ایمیج + صدور اولین گواهی TLS واقعی توسط Caddy).
وضعیت را دنبال کنید:

```bash
docker compose -f docker-compose.yml -f docker-compose.prod.yml --env-file .env.prod ps
docker compose -f docker-compose.yml -f docker-compose.prod.yml --env-file .env.prod logs -f caddy
```

## قدم ۳: تأیید Import شدن Realm

`keycloak` با `--import-realm` بالا می‌آید -- فایل `infra/keycloak/import/sepahan-realm.json`
را خودکار (فقط اگر Realm از قبل وجود نداشته باشد -- طبق مستندات رسمی Keycloak، در Restartهای
بعدی بی‌اثر و امن Skip می‌شود) Import می‌کند. تأیید:

```bash
curl -s https://auth.<domain>/realms/sepahan/.well-known/openid-configuration | head -c 200
```

⚠️ همان نکته‌ی همیشگی این پروژه (`infra/README.md`): اگر بعداً `KEYCLOAK_ADMIN_PASSWORD` را در
`.env.prod` عوض کردید ولی Volume دیتابیس از قبل وجود داشت، تغییر اعمال نمی‌شود -- رمز Admin
فقط در اولین Boot واقعی خوانده می‌شود.

## قدم ۴: گرفتن Client Secret واقعی admin-panel

وارد `https://auth.<domain>` شوید (با `KEYCLOAK_ADMIN`/`KEYCLOAK_ADMIN_PASSWORD` از `.env.prod`)
→ Realm `sepahan` → Clients → `admin-panel` → Credentials → مقدار Secret را کپی کنید →
در `.env.prod` مقدار `ADMIN_PANEL_KEYCLOAK_CLIENT_SECRET` را بگذارید → سرویس را دوباره بسازید:

```bash
docker compose -f docker-compose.yml -f docker-compose.prod.yml --env-file .env.prod up -d admin-panel
```

## قدم ۵: تأیید نهایی

- `https://admin.<domain>` → ورود واقعی با یک کاربر نقش `admin`.
- `https://grafana.<domain>` → Dashboardهای Backend Overview/Business Metrics داده‌ی واقعی نشان دهند.
- `curl -I https://api.<domain>/actuator/health` → `200`.
- `curl -I https://api.<domain>/actuator/prometheus` → باید **403/404** بدهد (نه از Loopback/شبکه‌ی
  خصوصی -- طبق ADR-0021/ADR-0022؛ اگر ۲۰۰ برگرداند، یعنی `server.forward-headers-strategy=native`
  یا مسیر Caddy درست پیکربندی نشده -- بلافاصله بررسی شود، این رگرسیون امنیتی جدی است).

## عملیات روزمره

**لاگ یک سرویس:**
```bash
docker compose -f docker-compose.yml -f docker-compose.prod.yml --env-file .env.prod logs -f backend
```

**بروزرسانی بعد از یک تغییر کد:**
```bash
git pull
docker compose -f docker-compose.yml -f docker-compose.prod.yml --env-file .env.prod up -d --build backend admin-panel
```

**پشتیبان‌گیری Postgres** (از خودِ Host، چون پورت فقط روی `127.0.0.1` منتشر شده -- ADR-0021/۲۲):
```bash
docker compose -f docker-compose.yml -f docker-compose.prod.yml --env-file .env.prod exec postgres \
  pg_dump -U postgres sepahan_app > backup-$(date +%F).sql
```

**توقف کامل** (بدون از‌دست‌رفتن داده -- Volume نگه داشته می‌شود):
```bash
docker compose -f docker-compose.yml -f docker-compose.prod.yml --env-file .env.prod down
```

## عیب‌یابی شناخته‌شده

- **`docker-compose` نسخه‌ی قدیمی (v1.29.2):** طبق `infra/README.md`، هنگام Recreate یک
  Container موجود ممکن است با `KeyError: 'ContainerConfig'` مواجه شوید -- `docker rm -f
  <name>` قبل از `up` رفع می‌کند.
- **کلید `ports` در Override واقعاً Replace نمی‌شود:** حین همین فاز کشف شد -- برخلاف
  `volumes`/`command`/`environment`، در این نسخه‌ی `docker-compose`، مقادیر `ports` بین
  فایل‌ها Append می‌شوند نه Replace. به همین دلیل محدودسازی Loopback کیکلوک/Grafana در خودِ
  `docker-compose.yml` (نه یک Override) با یک متغیر Env (`KEYCLOAK_BIND_ADDRESS`/
  `GRAFANA_BIND_ADDRESS`) انجام شده -- اگر روزی سرویس تازه‌ای با همین نیاز اضافه شود، همین
  الگو را تکرار کنید، نه `ports: []`.
- **گواهی TLS صادر نمی‌شود:** معمولاً یعنی DNS هنوز به این سرور اشاره نمی‌کند یا پورت ۸۰ از
  اینترنت بسته است -- لاگ `caddy` دقیقاً همین را گزارش می‌کند.
