# Admin Panel — Next.js + TypeScript

**وضعیت:** اسکلت ساختاری فقط (Phase 2). بوت‌استرپ واقعی (`create-next-app` یا معادل، Dependencyها) موضوع **Phase 14 — Admin Panel** است.

## مرجع تصمیم‌ها

- `sepahan-shop` (پروژه‌ی موجود روی Desktop) **فقط مرجع UI/UX** است؛ کد/Prisma/SQLite آن import نمی‌شود — [ADR-0002](../docs/adr/0002-modular-monolith-and-module-boundaries.md)
- پایه‌ی بصری (رنگ/تایپوگرافی/کامپوننت مشترک با Mobile): [Design System](../docs/architecture/ui-ux/design-system.html)
- احراز هویت: Authorization Code Flow با Keycloak (بدون PKCE، چون Server-side است) — [ADR-0003](../docs/adr/0003-keycloak-central-sso-fan-id.md)

## بخش‌های برنامه‌ریزی‌شده (طبق بند ۱۸ بریف)

Dashboard, Users, Fans, News, Matches, Tickets (شامل تئاتر داخلی), Shop, Orders, Payments, Insurance, Travel, Vehicle, Entertainment, Loyalty, Notifications, Partners, Integrations, Audit Logs, Settings — با RBAC مبتنی بر نقش‌های Keycloak.
