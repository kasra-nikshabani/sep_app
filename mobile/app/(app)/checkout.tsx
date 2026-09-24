import { useState } from "react";
import { View, TextInput, ActivityIndicator, Pressable } from "react-native";
import { Stack, router } from "expo-router";
import { Screen } from "@/components/Screen";
import { ThemedText } from "@/components/ThemedText";
import { Button } from "@/components/Button";
import { ErrorState } from "@/components/ErrorState";
import { spacing, radius } from "@/theme";
import { useTheme } from "@/hooks/useTheme";
import { useShippingMethods, useCheckout, usePayOrder } from "@/features/shop/api";
import { payAndWaitForCallback } from "@/lib/payment";
import { formatRial } from "@/lib/format";

function Field({ label, value, onChangeText }: { label: string; value: string; onChangeText: (t: string) => void }) {
  const { colors } = useTheme();
  return (
    <View style={{ gap: 4 }}>
      <ThemedText variant="caption" muted>
        {label}
      </ThemedText>
      <TextInput
        value={value}
        onChangeText={onChangeText}
        style={{
          borderWidth: 1,
          borderColor: colors.border,
          borderRadius: radius.field,
          padding: spacing.md,
          color: colors.text,
          fontFamily: "Vazirmatn-Regular",
          textAlign: "right",
        }}
        placeholderTextColor={colors.textMuted}
      />
    </View>
  );
}

export default function CheckoutScreen() {
  const { colors } = useTheme();
  const { data: shippingMethods, isLoading, isError, refetch } = useShippingMethods();
  const checkout = useCheckout();
  const payOrder = usePayOrder();
  const [shippingMethodId, setShippingMethodId] = useState<string>();
  const [recipientName, setRecipientName] = useState("");
  const [phone, setPhone] = useState("");
  const [province, setProvince] = useState("");
  const [city, setCity] = useState("");
  const [addressLine, setAddressLine] = useState("");
  const [error, setError] = useState<string>();

  async function handleSubmit() {
    setError(undefined);
    if (!shippingMethodId || !recipientName || !phone || !province || !city || !addressLine) {
      setError("همه‌ی فیلدهای اجباری را پر کنید");
      return;
    }
    try {
      const order = await checkout.mutateAsync({ shippingMethodId, recipientName, phone, province, city, addressLine });
      const payment = await payOrder.mutateAsync(order.id);
      await payAndWaitForCallback(payment.redirectUrl, payment.paymentId);
      router.replace("/orders");
    } catch (e) {
      setError(e instanceof Error ? e.message : "خطای غیرمنتظره");
    }
  }

  const isPending = checkout.isPending || payOrder.isPending;

  return (
    <Screen>
      <Stack.Screen options={{ headerShown: true, title: "تسویه حساب" }} />
      {error && <ThemedText color={colors.textMuted}>{error}</ThemedText>}
      <Field label="نام گیرنده" value={recipientName} onChangeText={setRecipientName} />
      <Field label="موبایل" value={phone} onChangeText={setPhone} />
      <Field label="استان" value={province} onChangeText={setProvince} />
      <Field label="شهر" value={city} onChangeText={setCity} />
      <Field label="آدرس" value={addressLine} onChangeText={setAddressLine} />

      <ThemedText variant="caption" muted>
        روش ارسال
      </ThemedText>
      {isLoading ? (
        <ActivityIndicator color={colors.accent} />
      ) : isError ? (
        <ErrorState onRetry={refetch} />
      ) : (
        <View style={{ gap: spacing.sm }}>
          {(shippingMethods ?? []).map((m) => (
            <Pressable
              key={m.id}
              onPress={() => setShippingMethodId(m.id)}
              style={{
                flexDirection: "row",
                justifyContent: "space-between",
                padding: spacing.md,
                borderRadius: radius.field,
                borderWidth: 1,
                borderColor: m.id === shippingMethodId ? colors.accent : colors.border,
              }}
            >
              <ThemedText>{m.name}</ThemedText>
              <ThemedText muted>{formatRial(m.baseRate)}</ThemedText>
            </Pressable>
          ))}
        </View>
      )}

      <Button title="پرداخت" variant="gold" loading={isPending} onPress={handleSubmit} />
    </Screen>
  );
}
