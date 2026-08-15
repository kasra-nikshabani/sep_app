# ADR-0007: استراتژی دیتابیس

- **وضعیت:** پیشنهادی — بخشی نیازمند تأیید کارفرما، بخشی موکول به Phase 6
- **تصمیم‌گیرندگان:** کارفرما + Architect

## Context

بند ۱۱ بریف: PostgreSQL به‌عنوان دیتابیس اصلی، با نیاز به بررسی ERD/روابط/کلیدها/ایندکس/Soft Delete/Audit قبل از ساخت جدول‌ها. Migration باید قابل بازگشت و قابل بررسی باشد.

از Phase 0 مشخص شد:
- Django از PostgreSQL مجزای خودش استفاده می‌کند (خارج از این پروژه، دست‌نخورده می‌ماند).
- `sepahan-shop` فعلاً روی SQLite بود؛ طبق تصمیم بازسازی کامل ([ADR-0002](0002-modular-monolith-and-module-boundaries.md))، این محدودیت دیگر موضوعیت ندارد چون از صفر ساخته می‌شود.

## Decision

### 7.1 یک نمونه‌ی PostgreSQL مجزا برای Backend جدید

Backend جدید (`sepapp/backend`) یک Instance/Database مستقل PostgreSQL دارد — **کاملاً جدا از دیتابیس Django**. هیچ Join یا دسترسی مستقیم بین این دو دیتابیس برقرار نمی‌شود؛ تنها راه تبادل داده، API/Integration Layer است (طبق [ADR-0004](0004-django-ticketing-sso-integration.md)). این تصمیم مستقیماً از اصل «Django نباید بازنویسی/ادغام ساختاری شود» و اصل اول بریف (Security) نتیجه می‌شود: دیتابیس Django یک سیستم پرداخت زنده است و نباید سطح دسترسی آن به سیستم‌های دیگر گسترش یابد.

### 7.2 قرارداد Schema-per-module (نه Database-per-module)

در همان یک دیتابیس PostgreSQL، هر ماژول (طبق ADR-0002) یک PostgreSQL Schema مجزا دارد (مثلاً `shop.products`, `ticketing.venues`, `wallet.transactions`). این یعنی:
- مرز منطقی بین ماژول‌ها در سطح دیتابیس هم دیده می‌شود (Maintainability).
- در آینده اگر واقعاً نیاز به جدا کردن یک ماژول به Microservice شد (طبق بند ۴ بریف، فقط در صورت نیاز واقعی)، مهاجرت دیتابیس آن ماژول ساده‌تر است چون از قبل ایزوله بوده.
- برخلاف Database-per-module، نیازی به مدیریت چند Connection Pool/چند Migration Pipeline از روز اول نیست — با اصل «Microservice غیرضروری نسازیم» هم‌راستاست.

### 7.3 موارد **تصمیم‌گیری‌نشده** در این ADR (عمداً)

- **ابزار Migration** (Flyway در برابر Liquibase): هر دو گزینه‌ی رایج و معتبر برای Spring Boot هستند؛ انتخاب نهایی و **نصب Dependency مربوطه** در Phase 6 با درخواست تأیید صریح انجام می‌شود (طبق قانون ۲۶ بریف — بدون تأیید، Dependency جدید اضافه نمی‌شود). پیشنهاد اولیه (غیرقطعی): Flyway، به‌دلیل سادگی و رواج بالا در اکوسیستم Spring Boot؛ در Phase 6 با ذکر Alternativeها مطرح می‌شود.
- **ERD کامل و ایندکس‌گذاری دقیق:** موضوع Phase 5 (User/Fan ID) و Phase 6 (Spring Boot Core) است، به‌ازای هر ماژول که واقعاً ساخته می‌شود.
- **استراتژی Soft Delete/Audit دقیق:** الگوی کلی (فیلدهای استاندارد `deleted_at`, `created_at`, `updated_at`, `created_by`, ردیابی از طریق ماژول `audit`) در Phase 5/6 به‌صورت یک Base Entity مشترک طراحی می‌شود، نه در این فاز.

## Alternatives Considered

| گزینه | چرا رد شد |
|---|---|
| Database-per-module از روز اول | سربار عملیاتی (Connection Pool، Backup، Migration مجزا برای ~۲۰ ماژول) در تناقض با تصمیم «Modular Monolith، نه Microservice» است |
| استفاده از همان دیتابیس Django برای داده‌های جدید | نقض مستقیم بند ۶ بریف و ریسک امنیتی/عملکردی مستقیم به سیستم Production زنده |

## Consequences

**مثبت:** مرز واضح بین ماژول‌ها از روز اول، بدون سربار عملیاتی چند-دیتابیسی؛ ایزوله‌بودن کامل از دیتابیس حساس Django.

**نیازمند توجه:** انضباط تیم توسعه لازم است تا کوئری‌های Cross-schema (که در یک دیتابیس فیزیکی وسوسه‌انگیز است) به‌جای فراخوانی از طریق Service Layer داخلی ماژول مقصد، مستقیماً SQL Join نزنند — این قانون باید در Code Review (Phase 2 به بعد) رعایت شود.
