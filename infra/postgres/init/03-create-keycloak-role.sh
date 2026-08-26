#!/bin/bash
# فقط در اولین initdb اجرا می‌شود (مثل 01-databases.sql/02-create-app-role.sh).
# طبق نکته‌ی صریح "ریسک شناخته‌شده" در همین پوشه (infra/README.md) -- "باید پیش از Phase 20
# اصلاح شود": Keycloak تا این‌جا با کاربر Superuser به Postgres وصل می‌شد. این نقش فقط مالک
# دیتابیس keycloak است؛ نه دسترسی به sepahan_app، نه هیچ Privilege سراسری دیگر (Phase 20، ADR-0022).
set -e

psql -v ON_ERROR_STOP=1 --username "$POSTGRES_USER" <<-EOSQL
    CREATE ROLE keycloak WITH LOGIN PASSWORD '${KEYCLOAK_DB_PASSWORD}';
    ALTER DATABASE keycloak OWNER TO keycloak;
    -- هم‌الگوی 02-create-app-role.sh -- بدون این، نقش keycloak هم می‌توانست به sepahan_app وصل شود.
    REVOKE CONNECT ON DATABASE sepahan_app FROM PUBLIC;
EOSQL
