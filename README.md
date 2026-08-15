# SEPahan Super App

پلتفرم دیجیتال یکپارچه‌ی باشگاه فولاد مبارکه سپاهان — یک حساب کاربری مرکزی (**Fan ID**) برای دسترسی به اخبار، بلیط، فروشگاه، کیف‌پول، وفاداری، و سرویس‌های جانبی (بانکی، خودرو، بیمه، گردشگری).

این پروژه **مرحله‌به‌مرحله** ساخته می‌شود؛ هر فاز مستقل Review و تأیید می‌شود. جزئیات فرآیند و فهرست کامل فازها در `docs/architecture/overview.md` است.

## نقشه‌ی این Repository

```
sepapp/
├── backend/        Spring Boot — Modular Monolith (Phase 6+)
├── admin-panel/     Next.js + TypeScript (Phase 14+)
├── mobile/          React Native + TypeScript (Phase 15+)
├── infra/           Docker Compose، پیکربندی Keycloak، CI (Phase 3+)
└── docs/
    ├── architecture/   نمای کلی معماری + پایه‌ی UI/UX
    ├── adr/            تصمیم‌های معماری (Architecture Decision Records)
    ├── api/             (از Phase 6)
    ├── authentication/  (از Phase 4)
    ├── integrations/    (از Phase 13)
    ├── deployment/       (از Phase 20)
    └── database/         (از Phase 5/6)
```

## سیستم‌های مرتبط خارج از این Repository

این پروژه با دو سیستم موجود و مستقل، **فقط از طریق API/SSO** یکپارچه می‌شود (نه ادغام کد):

- **`ticket.sepahansc`** (Django) — سامانه‌ی فروش بلیط فوتبال، Production زنده. حفظ می‌شود، بازنویسی نمی‌شود ([ADR-0004](docs/adr/0004-django-ticketing-sso-integration.md)).
- **`sepahan-shop`** (Next.js) — صرفاً مرجع UI/UX برای دامنه‌ی Shop؛ بک‌اند آن استفاده نمی‌شود ([ADR-0002](docs/adr/0002-modular-monolith-and-module-boundaries.md)).

## شروع مطالعه

۱. [`docs/architecture/overview.md`](docs/architecture/overview.md) — نمای کلی معماری
۲. [`docs/adr/`](docs/adr/) — تصمیم‌های معماری و دلایل آن‌ها
۳. [Design System](docs/architecture/ui-ux/design-system.html) — پایه‌ی بصری (رنگ/تایپوگرافی/کامپوننت)
