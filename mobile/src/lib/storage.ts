import { Platform } from "react-native";
import * as SecureStore from "expo-secure-store";

// expo-secure-store روی Web پشتیبانی نمی‌شود (طبق مستندات SDK 57)؛ برای Preview
// وب از localStorage استفاده می‌شود -- امنیت کمتر، فقط برای تست در این فاز.
// Build واقعی Native همیشه از SecureStore (Keychain/Keystore) استفاده می‌کند.
export async function getItem(key: string): Promise<string | null> {
  if (Platform.OS === "web") {
    return typeof localStorage !== "undefined" ? localStorage.getItem(key) : null;
  }
  return SecureStore.getItemAsync(key);
}

export async function setItem(key: string, value: string): Promise<void> {
  if (Platform.OS === "web") {
    if (typeof localStorage !== "undefined") localStorage.setItem(key, value);
    return;
  }
  await SecureStore.setItemAsync(key, value);
}

export async function deleteItem(key: string): Promise<void> {
  if (Platform.OS === "web") {
    if (typeof localStorage !== "undefined") localStorage.removeItem(key);
    return;
  }
  await SecureStore.deleteItemAsync(key);
}
