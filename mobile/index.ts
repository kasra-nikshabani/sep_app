import { I18nManager, Platform } from "react-native";

// این اپ فقط فارسی/راست‌به‌چپ است (بدون سوییچ زبان) -- طبق design-system.html
// (direction: rtl پیش‌فرض). روی Native باید قبل از اولین Render تنظیم شود؛
// برای Build واقعی Native، expo.android/ios نیازی به این ندارند چون app.json
// می‌تواند forcesRTL را مستقیم تنظیم کند (هنگام EAS Build اضافه شود).
if (!I18nManager.isRTL) {
  I18nManager.allowRTL(true);
  I18nManager.forceRTL(true);
}
if (Platform.OS === "web" && typeof document !== "undefined") {
  document.documentElement.dir = "rtl";
  document.documentElement.lang = "fa";
}

import "expo-router/entry";
