#!/bin/bash
# فقط در اولین initdb اجرا می‌شود (مثل 01-databases.sql).
# طبق ADR-0007 / بند ۱۰ بریف (Least Privilege): Backend با کاربر Superuser به Postgres وصل نمی‌شود.
# این نقش فقط مالک دیتابیس sepahan_app است؛ نه دسترسی به دیتابیس keycloak، نه هیچ Privilege سراسری دیگر.
set -e

psql -v ON_ERROR_STOP=1 --username "$POSTGRES_USER" <<-EOSQL
    CREATE ROLE sepahan_backend WITH LOGIN PASSWORD '${SEPAHAN_BACKEND_DB_PASSWORD}';
    ALTER DATABASE sepahan_app OWNER TO sepahan_backend;
    -- Postgres می‌دهد CONNECT روی هر دیتابیس را پیش‌فرض به PUBLIC می‌دهد؛ بدون این خط
    -- sepahan_backend می‌توانست به دیتابیس keycloak هم وصل شود (کشف‌شده و اصلاح‌شده در Phase 6)
    REVOKE CONNECT ON DATABASE keycloak FROM PUBLIC;
EOSQL
