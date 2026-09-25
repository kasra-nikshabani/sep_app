import { useEffect, useState } from "react";
import { View, Platform, Switch } from "react-native";
import { Stack } from "expo-router";
import * as Notifications from "expo-notifications";
import { Screen, Card } from "@/components/Screen";
import { ThemedText } from "@/components/ThemedText";
import { palette } from "@/theme";
import { useRegisterDeviceToken, useUnregisterDeviceToken } from "@/features/notifications/api";
import { getItem, setItem, deleteItem } from "@/lib/storage";

const STORAGE_KEY = "sepahan.device-token";

// expo-notifications روی Web اصلاً پیاده نشده (getDevicePushTokenAsync فقط ios/android) --
// طبق مستندات رسمی Expo SDK 57 -- پس این صفحه روی Web هرگز نباید این API را صدا بزند.
const PUSH_SUPPORTED = Platform.OS === "ios" || Platform.OS === "android";

export default function NotificationsScreen() {
  const register = useRegisterDeviceToken();
  const unregister = useUnregisterDeviceToken();
  const [enabled, setEnabled] = useState(false);
  const [loaded, setLoaded] = useState(false);
  const [error, setError] = useState<string | null>(null);

  useEffect(() => {
    getItem(STORAGE_KEY).then((token) => {
      setEnabled(!!token);
      setLoaded(true);
    });
  }, []);

  async function handleToggle(value: boolean) {
    setError(null);
    if (value) {
      try {
        if (Platform.OS === "android") {
          await Notifications.setNotificationChannelAsync("default", {
            name: "پیش‌فرض",
            importance: Notifications.AndroidImportance.MAX,
          });
        }
        const permission = await Notifications.requestPermissionsAsync();
        const granted =
          Platform.OS === "ios" ? permission.ios?.status === Notifications.IosAuthorizationStatus.AUTHORIZED : permission.granted;
        if (!granted) {
          setError("اجازه‌ی نمایش اعلان داده نشد. می‌توانید از تنظیمات گوشی آن را فعال کنید.");
          return;
        }
        const devicePushToken = await Notifications.getDevicePushTokenAsync();
        const platform = devicePushToken.type === "ios" ? "ios" : "android";
        await register.mutateAsync({ token: devicePushToken.data, platform });
        await setItem(STORAGE_KEY, devicePushToken.data);
      } catch {
        // در Expo Go (نه Build واقعی Native) getDevicePushTokenAsync همیشه شکست می‌خورد (ADR-0018).
        setError("فعال‌سازی اعلان‌ها ممکن نشد. لطفاً دوباره تلاش کنید.");
        return;
      }
    } else {
      const token = await getItem(STORAGE_KEY);
      if (token) {
        await unregister.mutateAsync(token);
        await deleteItem(STORAGE_KEY);
      }
    }
    setEnabled(value);
  }

  return (
    <Screen>
      <Stack.Screen options={{ headerShown: true, title: "اعلان‌ها" }} />
      {PUSH_SUPPORTED ? (
        <>
          <Card>
            <View style={{ flexDirection: "row", justifyContent: "space-between", alignItems: "center" }}>
              <ThemedText style={{ fontFamily: "Vazirmatn-Medium" }}>دریافت اعلان‌ها</ThemedText>
              <Switch value={enabled} onValueChange={handleToggle} disabled={!loaded || register.isPending || unregister.isPending} />
            </View>
          </Card>
          {error ? (
            <ThemedText variant="caption" style={{ color: palette.danger }}>
              {error}
            </ThemedText>
          ) : (
            // ارسال از طریق FCM (Firebase) طبق ADR-0018 -- دریافت واقعی روی دستگاه هنوز تست زنده نشده.
            // متن قبلی («این نسخه فقط در مرورگر قابل اجراست») دقیقاً روی همین مسیر Native نمایش داده می‌شد.
            <ThemedText variant="caption" muted>
              با فعال کردن این گزینه، خبرها، نتایج و اطلاع‌رسانی‌های باشگاه را به‌صورت اعلان روی گوشی دریافت می‌کنید.
            </ThemedText>
          )}
        </>
      ) : (
        <Card>
          {/* طبق مستندات Expo، getDevicePushTokenAsync روی Web پیاده نشده (ADR-0018). */}
          <ThemedText variant="caption" muted>
            دریافت اعلان فقط در اپ اندروید و iOS سپاهان امکان‌پذیر است.
          </ThemedText>
        </Card>
      )}
    </Screen>
  );
}
