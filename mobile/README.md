# Mobile — React Native + TypeScript

**وضعیت:** اسکلت ساختاری فقط (Phase 2). بوت‌استرپ واقعی (React Native CLI/Expo — تصمیم در Phase 15، Dependencyها) موضوع **Phase 15 — React Native App** است.

## مرجع تصمیم‌ها

- پایه‌ی بصری (رنگ/تایپوگرافی/کامپوننت مشترک با Admin Panel، شامل پیش‌نمایش ناوبری پایین): [Design System](../docs/architecture/ui-ux/design-system.html)
- احراز هویت: Authorization Code + PKCE با Keycloak (Public Client، بدون Secret) — [ADR-0003](../docs/adr/0003-keycloak-central-sso-fan-id.md)

## بخش‌های برنامه‌ریزی‌شده (طبق بند ۱۹ بریف)

SSO Login, Home, News, Matches, Tickets (فوتبال از طریق Django، تئاتر داخلی), Shop, Services, Wallet, Loyalty, Profile, Notifications.
