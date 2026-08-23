import * as WebBrowser from "expo-web-browser";

const API_BASE_URL = process.env.EXPO_PUBLIC_API_BASE_URL as string;

/**
 * زیبال مرورگر را به Callback خودِ Backend (نه یک Deep Link اپ) Redirect می‌کند
 * -- طبق ADR-0011، تأیید واقعی سرور-به-سرور همان‌جا انجام می‌شود. اپ فقط صبر
 * می‌کند تا مرورگر به آن مسیر برسد، آن را می‌بندد، و بعد خودش وضعیت را دوباره
 * از Backend می‌خواند (نه از پاسخ خودِ Callback که فقط JSON خام است).
 */
export async function payAndWaitForCallback(redirectUrl: string, paymentId: string): Promise<void> {
  const callbackPattern = `${API_BASE_URL}/api/v1/payments/${paymentId}/callback`;
  await WebBrowser.openAuthSessionAsync(redirectUrl, callbackPattern);
}
