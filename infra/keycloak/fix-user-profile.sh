#!/bin/bash
# اجرای دستی، یک‌بار، بعد از هر بار Import تازه‌ی Realm (docker-compose up روی Volume خالی).
#
# چرا لازم است (دو باگ واقعی که در تست Runtime واقعی Phase 6 پیدا شدند، نه تنظیم اختیاری):
#  ۱. پروفایل اعلامی (Declarative User Profile) پیش‌فرض Keycloak فیلدهای email/firstName/
#     lastName را Required می‌کند. مدل Fan ID ما بر پایه‌ی تلفن/کد ملی است (بدون ایمیل در
#     ثبت‌نام)، پس بدون این اسکریپت هر کاربر با خطای «Account is not fully set up»
#     (VERIFY_PROFILE) از ورود Direct Grant/Password باز می‌ماند.
#  ۲. همان Declarative User Profile به‌صورت پیش‌فرض یک Allowlist سخت‌گیرانه است: تا وقتی
#     national_code/phone_number صریحاً در این Schema تعریف نشوند، Keycloak آن‌ها را هنگام
#     ساخت/ویرایش کاربر بی‌صدا نادیده می‌گیرد (نه خطا، فقط ذخیره نمی‌شوند) — یعنی Claim Mapperهای
#     Client Scope «fan-identity» همیشه خالی برمی‌گشتند چون خودِ Attribute اصلاً ذخیره نشده بود.
#
# این تنظیم بخشی از RealmRepresentation قابل Import نیست (به‌صورت Component داخلی ذخیره
# می‌شود)، بنابراین نمی‌تواند مثل بقیه‌ی sepahan-realm.json به‌صورت کاملاً Declarative باشد.
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

ADMIN_TOKEN=$(curl -s -X POST "$KC_URL/realms/master/protocol/openid-connect/token" \
  -H "Content-Type: application/x-www-form-urlencoded" \
  -d "grant_type=password" -d "client_id=admin-cli" \
  -d "username=${KEYCLOAK_ADMIN}" -d "password=${KEYCLOAK_ADMIN_PASSWORD}" \
  | python3 -c "import json,sys; print(json.load(sys.stdin).get('access_token',''))")

if [ -z "$ADMIN_TOKEN" ]; then
  echo "خطا: دریافت توکن Admin ناموفق بود. KEYCLOAK_ADMIN/KEYCLOAK_ADMIN_PASSWORD در .env را بررسی کنید." >&2
  exit 1
fi

HTTP_CODE=$(curl -s -o /dev/null -w "%{http_code}" -X PUT "$KC_URL/admin/realms/sepahan/users/profile" \
  -H "Authorization: Bearer $ADMIN_TOKEN" -H "Content-Type: application/json" \
  -d '{
    "attributes": [
      {"name": "username", "displayName": "${username}",
       "validations": {"length": {"min": 3, "max": 255}, "username-prohibited-characters": {}, "up-username-not-idn-homograph": {}},
       "permissions": {"view": ["admin","user"], "edit": ["admin","user"]}, "multivalued": false},
      {"name": "email", "displayName": "${email}",
       "validations": {"email": {}, "length": {"max": 255}},
       "permissions": {"view": ["admin","user"], "edit": ["admin","user"]}, "multivalued": false},
      {"name": "firstName", "displayName": "${firstName}",
       "validations": {"length": {"max": 255}, "person-name-prohibited-characters": {}},
       "permissions": {"view": ["admin","user"], "edit": ["admin","user"]}, "multivalued": false},
      {"name": "lastName", "displayName": "${lastName}",
       "validations": {"length": {"max": 255}, "person-name-prohibited-characters": {}},
       "permissions": {"view": ["admin","user"], "edit": ["admin","user"]}, "multivalued": false},
      {"name": "national_code", "displayName": "کد ملی",
       "permissions": {"view": ["admin","user"], "edit": ["admin","user"]}, "multivalued": false},
      {"name": "phone_number", "displayName": "شماره تلفن",
       "permissions": {"view": ["admin","user"], "edit": ["admin","user"]}, "multivalued": false}
    ],
    "groups": [{"name": "user-metadata", "displayHeader": "User metadata", "displayDescription": "Attributes, which refer to user metadata"}]
  }')

if [ "$HTTP_CODE" = "200" ]; then
  echo "OK — email/firstName/lastName دیگر Required نیستند؛ national_code/phone_number حالا قابل ذخیره‌اند."
else
  echo "خطا: HTTP $HTTP_CODE" >&2
  exit 1
fi
