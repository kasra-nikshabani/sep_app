# Infra

**وضعیت:** اسکلت ساختاری فقط (Phase 2). محتوای واقعی از **Phase 3 — Infrastructure** و **Phase 4 — Keycloak/SSO** اضافه می‌شود.

## محتوای برنامه‌ریزی‌شده

- `docker-compose.yml` برای Development: PostgreSQL، Redis، Keycloak (Phase 3/4)
- Export تنظیمات Realm/Client کیکلوک (`keycloak/realm-export.json`) — [ADR-0003](../docs/adr/0003-keycloak-central-sso-fan-id.md)
- تنظیمات CI (مسیر مشخص در Phase 3، ابزار دقیق با تأیید کارفرما)

هیچ فایل Compose یا Dependency‌ای در این فاز اضافه نشده است.
