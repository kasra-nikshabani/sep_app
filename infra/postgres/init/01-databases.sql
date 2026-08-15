-- اجرا فقط در اولین بالا آمدن Volume (initdb) — Postgres این اسکریپت‌ها را فقط یک‌بار روی دیتابیس خالی اجرا می‌کند.
-- هدف: جدا نگه‌داشتن دیتابیس Backend از دیتابیس Keycloak روی همان Instance، طبق ADR-0007
-- (Schema-per-module داخل هر دیتابیس؛ اما خود Keycloak به‌عنوان یک سیستم مجزا، دیتابیس مجزا می‌گیرد).

CREATE DATABASE sepahan_app;
CREATE DATABASE keycloak;
