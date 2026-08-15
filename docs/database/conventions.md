# قراردادهای دیتابیس (Cross-cutting)

طبق بند ۱۱ بریف: قبل از ساخت هر جدول جدید (در هر ماژولی)، این قراردادها باید رعایت شوند. این سند مرجع مشترک همه‌ی ماژول‌های آینده است — نه فقط `users`/`fan`.

## ستون‌های پایه‌ی هر جدول

هر جدول در هر Schema (طبق [ADR-0007](../adr/0007-database-strategy.md)) این ستون‌ها را دارد، مگر استثنای صریح و مستند:

| ستون | نوع | توضیح |
|---|---|---|
| `id` | `UUID` (PK, `DEFAULT gen_random_uuid()`) | نه Serial/Integer — تا شناسه‌ها قابل‌حدس یا قابل‌شمارش نباشند (امنیت) و در آینده اگر لازم شد Merge/Migrate بین سیستم‌ها ساده‌تر باشد |
| `created_at` | `TIMESTAMPTZ NOT NULL DEFAULT now()` | — |
| `updated_at` | `TIMESTAMPTZ NOT NULL DEFAULT now()` | در Phase 6 با Hook سطح JPA/Hibernate به‌روز می‌شود، نه Trigger دیتابیس (ساده‌تر برای Maintainability) |
| `deleted_at` | `TIMESTAMPTZ NULL` | `NULL` یعنی رکورد فعال است؛ مقداردار یعنی Soft-deleted — پایین توضیح داده شده |
| `created_by` / `updated_by` | `UUID NULL` (اشاره به `users.app_user.id`) | `NULL` مجاز برای عملیات سیستمی (مثلاً Migration، Job زمان‌بندی‌شده) |

## Soft Delete

- هیچ `DELETE` واقعی روی داده‌ی کسب‌وکاری در مسیر عادی برنامه انجام نمی‌شود؛ فقط `deleted_at` پر می‌شود.
- کوئری‌های پیش‌فرض باید `deleted_at IS NULL` را فیلتر کنند (در Phase 6 با یک Base Repository/Filter مشترک در Spring Data JPA، نه تکرار دستی در هر Query).
- Hard Delete واقعی (مثلاً برای درخواست قانونی حذف داده‌ی کاربر) یک عملیات جدا، Audit‌شده، و محدود به نقش `admin` است — جزئیات دقیق در فاز مربوط به هر ماژول حساس (مثلاً Users) تعیین می‌شود.

## Audit

- تغییرات روی جدول‌های حساس (`users.app_user`, `wallet.*`, `payments.*`, `ticketing.*`) توسط ماژول `audit` (طبق ADR-0002) ثبت می‌شوند.
- مکانیزم دقیق (Event-based از داخل Service Layer در برابر Trigger سطح دیتابیس) در Phase 6 با ذکر دلیل تعیین می‌شود — این‌جا فقط قرارداد ستون‌ها (`created_by`/`updated_by`) مشخص شده که پیش‌نیاز هر دو رویکرد است.

## کلیدهای خارجی بین Schemaها

طبق ADR-0007، هر ماژول Schema مجزا دارد اما همه در یک دیتابیس فیزیکی (`sepahan_app`) هستند. کلید خارجی بین Schemaها (مثلاً `wallet.wallet.user_id → users.app_user.id`) مجاز است (چون در یک دیتابیس فیزیکی‌اند)، اما **کوئری Cross-schema JOIN مستقیم از کد اپلیکیشن ممنوع است** — دسترسی فقط از طریق Service Layer همان ماژول صاحب داده انجام می‌شود (همان قانونی که در ADR-0007 به‌عنوان ریسک باز ثبت شده بود).

## Naming

- نام جدول‌ها: `snake_case`، مفرد نه جمع (مثلاً `app_user` نه `app_users`) — یک قرارداد ثابت برای خوانایی یکسان در کل پروژه.
- نام Schema برابر نام ماژول (طبق فهرست ADR-0002): `users`, `fan`, `wallet`, `ticketing`, ...
