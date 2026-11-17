#!/bin/bash
# پروفایل کاربر (Declarative User Profile) را از همان sepahan-realm.json روی Realm در حال اجرا اعمال می‌کند.
#
# Import تازه (--import-realm) این پروفایل را خودش می‌سازد: Component «declarative-user-profile»
# داخل sepahan-realm.json است (با یک Import واقعی روی Keycloak 26.7.1 تأیید شد). این اسکریپت فقط
# برای Realmی لازم است که از قبل وجود داشته -- Import، Realm موجود را بازنویسی نمی‌کند -- مثل
# Volume محلی قدیمی. روی Realm تازه بی‌اثر و امن است.
#
# پروفایل فقط یک منبع دارد: sepahan-realm.json. نسخه‌ی قبلی این اسکریپت کپی جداگانه‌ی خودش را
# داشت (پروفایل Phase 6) و اجرای آن، اعتبارسنجی‌های ثبت‌نام و «کد ملی = نام کاربری»
# (docs/adr/0024) را بی‌صدا برمی‌گرداند.
#
# ⚠️ روی Realmی که کاربر قدیمی دارد: اول نام کاربری هر کاربر را به کد ملی‌اش تغییر دهید
# (docs/adr/0024) -- Claim «national_code» از نام کاربری خوانده می‌شود.
#
# استفاده:
#   cd infra && ./keycloak/fix-user-profile.sh
set -eo pipefail

cd "$(dirname "$0")/.."

# عمداً به‌جای «source .env» از خواندن خط‌به‌خط استفاده شده: اگر مقداری در .env کاراکتر
# ویژه‌ی Shell داشته باشد (مثلاً $ در یک رمز عبور)، «source» آن را به‌عنوان کد Bash اجرا/
# Expand می‌کند — گاهی با خطا (set -u)، گاهی بدتر: به‌صورت خاموش به مقدار اشتباه (این باگ
# در تست Runtime واقعی Phase 6 پیدا شد).
while IFS='=' read -r key value; do
  [[ "$key" =~ ^[A-Za-z_][A-Za-z0-9_]*$ ]] || continue
  export "$key=$value"
done < <(grep -vE '^\s*#|^\s*$' .env)

KC_URL="http://localhost:${KEYCLOAK_PORT:-8080}"

PROFILE=$(python3 -c "import json; d = json.load(open('keycloak/import/sepahan-realm.json', encoding='utf-8')); print(d['components']['org.keycloak.userprofile.UserProfileProvider'][0]['config']['kc.user.profile.config'][0])")

ADMIN_TOKEN=$(curl -s -X POST "$KC_URL/realms/master/protocol/openid-connect/token" \
  -H "Content-Type: application/x-www-form-urlencoded" \
  -d "grant_type=password" -d "client_id=admin-cli" \
  -d "username=${KEYCLOAK_ADMIN}" -d "password=${KEYCLOAK_ADMIN_PASSWORD}" \
  | python3 -c "import json,sys; print(json.load(sys.stdin).get('access_token',''))")

if [ -z "$ADMIN_TOKEN" ]; then
  echo "خطا: دریافت توکن Admin ناموفق بود. KEYCLOAK_ADMIN/KEYCLOAK_ADMIN_PASSWORD در .env را بررسی کنید." >&2
  exit 1
fi

HTTP_CODE=$(printf '%s' "$PROFILE" | curl -s -o /dev/null -w "%{http_code}" -X PUT "$KC_URL/admin/realms/sepahan/users/profile" \
  -H "Authorization: Bearer $ADMIN_TOKEN" -H "Content-Type: application/json; charset=utf-8" --data-binary @-)

if [ "$HTTP_CODE" = "200" ]; then
  echo "OK — پروفایل کاربر Realm «sepahan» با sepahan-realm.json یکی شد."
else
  echo "خطا: HTTP $HTTP_CODE" >&2
  exit 1
fi
