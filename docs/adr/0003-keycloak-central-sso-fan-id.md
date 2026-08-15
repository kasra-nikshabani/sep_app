# ADR-0003: Keycloak به‌عنوان SSO مرکزی / Fan ID

- **وضعیت:** پیشنهادی — نیازمند تأیید کارفرما
- **تصمیم‌گیرندگان:** کارفرما + Architect

## Context

طبق بند ۵ بریف، مهم‌ترین بخش پروژه یک Identity مرکزی به‌نام **Fan ID** است که Mobile App، Admin Panel، و در آینده Django Ticketing و سیستم‌های Partner را پوشش دهد. Provider مشخص‌شده: **Keycloak**. استانداردها: OAuth 2.0 / OIDC / JWT / Authorization Code Flow / PKCE برای Mobile / Refresh Token / Roles / Permissions / Client Management.

از Phase 0 مشخص شد که در حال حاضر **هیچ** مکانیزم OAuth/OIDC/JWT در هیچ سیستم موجودی وجود ندارد؛ این یعنی SSO کاملاً Greenfield ساخته می‌شود، اما باید به شناسه‌های موجود (`national_code`, `phone_number` در Django؛ `phone`/JWT سفارشی در Shop) پل بزند.

## Decision

### 3.1 توپولوژی Realm/Client

- یک Realm واحد: `sepahan`
- Clientها (هرکدام Confidential/Public طبق نوع):
  - `mobile-app` (Public، PKCE اجباری، Authorization Code Flow)
  - `admin-panel` (Confidential، Authorization Code Flow، بدون PKCE چون Server-side Next.js است)
  - `backend-service` (Confidential، برای اعتبارسنجی توکن‌ها و در صورت نیاز Service Account برای فراخوانی‌های بین‌سرویسی)
  - `django-ticketing` (Confidential، مخصوص Phase 7 — اعتبارسنجی توکن در Django)
  - Clientهای Partner آینده به‌صورت جداگانه، طبق نیاز Phase مربوطه

### 3.2 جریان احراز هویت

- **Mobile:** Authorization Code + PKCE، بدون Client Secret (چون Public Client است).
- **Admin Panel:** Authorization Code استاندارد (Confidential Client)، Token Exchange سمت سرور Next.js (نه مرورگر).
- **Refresh Token:** فعال، با چرخش (Rotation) — جزئیات دقیق TTL و سیاست Rotation در Phase 4 (پیاده‌سازی Keycloak) با تست امنیتی نهایی می‌شود.
- **Django (Phase 7):** اعتبارسنجی مستقیم JWT با Public Key/JWKS Endpoint کیکلوک، بدون نیاز به Redirect کامل OIDC در Django (چون Session Auth فعلی حفظ می‌شود — جزئیات در [ADR-0004](0004-django-ticketing-sso-integration.md)).

### 3.3 نگاشت هویت به سیستم‌های موجود

Keycloak Subject (UUID) به‌عنوان شناسه‌ی اصلی داخلی استفاده می‌شود. `national_code` و `phone_number` به‌عنوان **User Attributes** در Keycloak ذخیره و در JWT Claims قرار می‌گیرند تا:

- سیستم Django بتواند کاربر Keycloak را به رکورد `User` موجود خودش (بر اساس `national_code` یکتا) نگاشت کند، بدون نیاز به Migration داده در Django.
- در آینده اگر بلیط فوتبال هم به این معماری منتقل شد، شناسه‌ی یکتا از قبل موجود است.

### 3.4 نقش‌ها (Roles) — سطح اول، قابل توسعه در Phase 4/5

نقش‌های Realm-level اولیه (نهایی نمی‌شوند، فقط نقطه‌ی شروع): `fan` (کاربر عادی)، `vip`، `admin`، `gate-operator` (معادل کاربران گیت Django)، `partner`. جزئیات RBAC دقیق‌تر (Permission per module) در Phase 5 طراحی می‌شود.

## Alternatives Considered

| گزینه | چرا رد شد |
|---|---|
| Auth0 / سرویس‌های SaaS مشابه | بریف صراحتاً Keycloak را مشخص کرده (Self-hosted، کنترل کامل داده روی زیرساخت خودمان — مهم برای داده‌های حساس هواداران ایرانی) |
| ساخت سیستم Auth سفارشی به‌جای Keycloak | نقض مستقیم بند ۵ بریف؛ ریسک امنیتی بسیار بالاتر از استفاده از یک IdP استاندارد و Battle-tested |
| چند Realm جدا (یکی برای Fan، یکی برای Partner) | پیچیدگی غیرضروری در این مرحله؛ یک Realm با Client/Role جدا برای هر مصرف‌کننده کافی است و مطابق اصل «Microservice غیرضروری نسازیم» است |

## Consequences

**مثبت:** یک منبع حقیقت برای هویت در کل اکوسیستم؛ امکان افزودن سرویس‌های آینده (بند ۳ بریف) بدون بازطراحی Auth.

**ریسک/نیازمند توجه:**
- تنظیم دقیق Token TTL و Refresh Rotation باید قبل از Production با تست امنیتی تأیید شود (Phase 19: Security Audit).
- نگاشت `national_code` به Django باید Case خطا (کد ملی تکراری/نامعتبر) را پوشش دهد — جزئیات در Phase 5.
- Django باید یک Client Secret جدید (`django-ticketing`) دریافت کند؛ با توجه به این‌که تاریخچه‌ی Git این Repo قبلاً یک‌بار نشت Secret داشته (طبق گزارش موجود)، این Secret باید فقط در `.env` سرور Production قرار گیرد، هرگز در کد یا Commit.

## Open Questions

- سیاست دقیق TTL Access/Refresh Token → Phase 4
- جزئیات کامل RBAC/Permission Matrix → Phase 5
