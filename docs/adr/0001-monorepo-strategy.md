# ADR-0001: استراتژی Monorepo برای اجزای جدید

- **وضعیت:** پذیرفته‌شده (تأیید کارفرما، 2026-08-15)
- **تصمیم‌گیرندگان:** کارفرما + Architect

## Context

پروژه نیاز به سه جزء کاملاً جدید دارد: Backend (Spring Boot)، Admin Panel (Next.js)، و Mobile App (React Native). هم‌زمان دو سیستم موجود و مستقل هم در کنار پروژه قرار دارند: `ticket.sepahansc` (Django، Production زنده) و `sepahan-shop` (پروتوتایپ Next.js). طبق بریف پروژه، سیستم Django **نباید بازنویسی یا در معماری جدید ادغام کد شود**، فقط باید از طریق SSO به آن وصل شویم.

سؤال: آیا اجزای جدید در یک Repository واحد (`sepapp`) قرار بگیرند یا هرکدام Repository جدا داشته باشند؟

## Decision

**Monorepo واحد** برای اجزای جدید:

```
sepapp/
├── backend/
├── admin-panel/
├── mobile/
├── infra/
└── docs/
```

`ticket.sepahansc` و `sepahan-shop` **در همین Repository نمی‌آیند** و به‌عنوان سیستم‌های خارجی، فقط از طریق API/OIDC یکپارچه می‌شوند.

## Alternatives Considered

| گزینه | مزیت | عیب | چرا رد شد |
|---|---|---|---|
| Poly-repo کامل (Repo جدا برای هرکدام از backend/admin/mobile) | استقلال کامل CI/CD و Versioning | هماهنگی Contract (DTO/OpenAPI) بین Repoها سخت‌تر می‌شود؛ سربار مدیریتی بالا برای تیم کوچک اولیه | سرعت توسعه و هماهنگی در این مرحله از پروژه اولویت بالاتری نسبت به استقلال کامل CI/CD دارد (طبق اولویت‌بندی بریف: Maintainability > DX > Speed، اما استقلال افراطی در این مقیاس هزینه‌ی Maintainability هم دارد) |
| ادغام کامل همه‌چیز شامل Django و Shop در یک Repo | یکپارچگی حداکثری تاریخچه‌ی Git | نقض صریح قانون «Django نباید بازنویسی/دست‌کاری ساختاری شود»؛ ریسک تاریخچه‌ی نشت‌داده‌ی قبلی Django را به Repo جدید منتقل می‌کند | رد شد — کارفرما هم صراحتاً تأیید کرد که این دو سیستم جدا بمانند |

## Consequences

**مثبت:**
- تغییرات هم‌زمان روی Backend/Admin/Mobile (مثلاً یک Endpoint جدید + مصرف آن در Admin) در یک PR قابل بررسی است.
- یک نسخه‌ی مشترک از تنظیمات CI/CD، Lint، و ابزارهای مشترک.
- بلاست‌ریدیوس (Blast Radius) تغییرات محدود به همین Repo است؛ سیستم Django کاملاً ایزوله می‌ماند.

**منفی / باید مدیریت شود:**
- نیاز به قوانین واضح Path-based CI (اجرای تست فقط روی بخش تغییریافته) تا Build زمان‌بر نشود — موضوع Phase 3.
- به اشتراک‌گذاری Contract با Django (که Repo جدا دارد) نیاز به مستندسازی صریح API/OpenAPI دارد، نه فرض ضمنی از کد مشترک.

## Open follow-up

نحوه‌ی هماهنگی Contract بین `sepapp` و `ticket.sepahansc` (نسخه‌بندی API، مستندسازی OIDC Claims مشترک) باید در `docs/authentication/` هنگام Phase 7 مستند شود.
