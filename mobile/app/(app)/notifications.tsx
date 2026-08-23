import { useEffect, useState } from "react";
import { View, Platform, Switch } from "react-native";
import { Stack } from "expo-router";
import * as Crypto from "expo-crypto";
import { Screen, Card } from "@/components/Screen";
import { ThemedText } from "@/components/ThemedText";
import { spacing } from "@/theme";
import { useTheme } from "@/hooks/useTheme";
import { useRegisterDeviceToken, useUnregisterDeviceToken } from "@/features/notifications/api";
import { getItem, setItem, deleteItem } from "@/lib/storage";

const STORAGE_KEY = "sepahan.device-token";

export default function NotificationsScreen() {
  const { colors } = useTheme();
  const register = useRegisterDeviceToken();
  const unregister = useUnregisterDeviceToken();
  const [enabled, setEnabled] = useState(false);
  const [loaded, setLoaded] = useState(false);

  useEffect(() => {
    getItem(STORAGE_KEY).then((token) => {
      setEnabled(!!token);
      setLoaded(true);
    });
  }, []);

  async function handleToggle(value: boolean) {
    if (value) {
      const token = Crypto.randomUUID();
      const platform = Platform.OS === "ios" ? "ios" : Platform.OS === "android" ? "android" : "web";
      await register.mutateAsync({ token, platform });
      await setItem(STORAGE_KEY, token);
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
      <Card>
        <View style={{ flexDirection: "row-reverse", justifyContent: "space-between", alignItems: "center" }}>
          <ThemedText style={{ fontFamily: "Vazirmatn-Medium" }}>دریافت اعلان Push</ThemedText>
          <Switch value={enabled} onValueChange={handleToggle} disabled={!loaded || register.isPending || unregister.isPending} />
        </View>
      </Card>
      <ThemedText variant="caption" muted>
        در این نسخه فقط زیرساخت ثبت/لغو Device Token واقعی است -- هیچ Provider واقعی Push (FCM/APNs) هنوز انتخاب نشده
        (طبق ADR-0015)، پس اعلانی واقعاً ارسال نمی‌شود.
      </ThemedText>
    </Screen>
  );
}
