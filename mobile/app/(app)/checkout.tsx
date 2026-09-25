import { useEffect, useState } from "react";
import { View, TextInput, ActivityIndicator, Pressable, type TextInputProps } from "react-native";
import { Stack, router } from "expo-router";
import { Screen, Card } from "@/components/Screen";
import { ThemedText } from "@/components/ThemedText";
import { Button } from "@/components/Button";
import { ErrorState } from "@/components/ErrorState";
import { spacing, radius, palette } from "@/theme";
import { useTheme } from "@/hooks/useTheme";
import { useShippingMethods, useCheckout, usePayOrder, useCart } from "@/features/shop/api";
import { useMe } from "@/features/users/api";
import { describeError } from "@/lib/api";
import { payAndWaitForCallback } from "@/lib/payment";
import { formatRial, toLatinDigits } from "@/lib/format";

type FieldName = "recipientName" | "phone" | "province" | "city" | "addressLine" | "postalCode" | "shippingMethodId";

function Field({
  label,
  value,
  onChangeText,
  error,
  optional = false,
  ...inputProps
}: {
  label: string;
  value: string;
  onChangeText: (t: string) => void;
  error?: string;
  optional?: boolean;
} & Pick<TextInputProps, "keyboardType" | "autoComplete" | "textContentType" | "maxLength" | "multiline">) {
  const { colors } = useTheme();
  return (
    <View style={{ gap: 4 }}>
      <ThemedText variant="caption" muted>
        {label}
        {optional ? " (اختیاری)" : ""}
      </ThemedText>
      <TextInput
        value={value}
        onChangeText={onChangeText}
        accessibilityLabel={label}
        style={{
          borderWidth: 1,
          borderColor: error ? palette.danger : colors.border,
          borderRadius: radius.field,
          padding: spacing.md,
          color: colors.text,
          fontFamily: "Vazirmatn-Regular",
          textAlign: "right",
          minHeight: inputProps.multiline ? 80 : undefined,
          textAlignVertical: inputProps.multiline ? "top" : undefined,
        }}
        placeholderTextColor={colors.textMuted}
        {...inputProps}
      />
      {error ? (
        <ThemedText variant="caption" color={palette.danger}>
          {error}
        </ThemedText>
      ) : null}
    </View>
  );
}

function SummaryRow({ label, value, strong = false }: { label: string; value: string; strong?: boolean }) {
  return (
    <View style={{ flexDirection: "row", justifyContent: "space-between", alignItems: "center" }}>
      <ThemedText muted={!strong} style={strong ? { fontFamily: "Vazirmatn-Medium" } : undefined}>
        {label}
      </ThemedText>
      <ThemedText style={strong ? { fontFamily: "Vazirmatn-Medium" } : undefined}>{value}</ThemedText>
    </View>
  );
}

export default function CheckoutScreen() {
  const { colors } = useTheme();
  const { data: shippingMethods, isLoading, isError, refetch } = useShippingMethods();
  const { data: cart } = useCart();
  const { data: me } = useMe();
  const checkout = useCheckout();
  const payOrder = usePayOrder();
  const [shippingMethodId, setShippingMethodId] = useState<string>();
  const [recipientName, setRecipientName] = useState("");
  const [phone, setPhone] = useState("");
  const [province, setProvince] = useState("");
  const [city, setCity] = useState("");
  const [addressLine, setAddressLine] = useState("");
  const [postalCode, setPostalCode] = useState("");
  const [fieldErrors, setFieldErrors] = useState<Partial<Record<FieldName, string>>>({});
  const [error, setError] = useState<string>();
  // اگر سفارش ساخته شد ولی شروع پرداخت شکست خورد، سبد دیگر خالی است -- تکرار «پرداخت»
  // دوباره Checkout می‌زند و بی‌معنی شکست می‌خورد؛ کاربر باید به «سفارش‌های من» هدایت شود.
  const [orderCreatedButUnpaid, setOrderCreatedButUnpaid] = useState(false);

  // پیش‌پرکردن از حساب کاربر (فقط فیلدهایی که کاربر هنوز چیزی در آن‌ها ننوشته).
  useEffect(() => {
    if (!me) return;
    setRecipientName((v) => v || me.displayName || "");
    setPhone((v) => v || me.phoneNumber || "");
    setCity((v) => v || me.city || "");
  }, [me]);

  // خطای هر فیلد با اولین تغییر همان فیلد پاک می‌شود (نه فقط با ارسال دوباره).
  function onChange(name: FieldName, setter: (value: string) => void) {
    return (value: string) => {
      setter(value);
      setFieldErrors((errors) => ({ ...errors, [name]: undefined }));
    };
  }

  const subtotal = (cart ?? []).reduce((sum, i) => sum + Number(i.lineSubtotal), 0);
  const selectedMethod = shippingMethods?.find((m) => m.id === shippingMethodId);
  // Backend هزینه‌ی ارسال را baseRate + perKgRate × وزن حساب می‌کند؛ وزن در CartItem نیست، پس
  // وقتی perKgRate صفر نیست مبلغ نهایی فقط «تقریبی» نمایش داده می‌شود (بدون حدس زدن وزن).
  const weightBased = !!selectedMethod && Number(selectedMethod.perKgRate) > 0;

  function validate(): Partial<Record<FieldName, string>> {
    const errors: Partial<Record<FieldName, string>> = {};
    const required = "این فیلد الزامی است";
    if (!recipientName.trim()) errors.recipientName = required;
    const normalizedPhone = toLatinDigits(phone).replace(/[\s-]/g, "");
    if (!normalizedPhone) errors.phone = required;
    else if (!/^09\d{9}$/.test(normalizedPhone)) errors.phone = "شماره موبایل باید ۱۱ رقم و با ۰۹ شروع شود";
    if (!province.trim()) errors.province = required;
    if (!city.trim()) errors.city = required;
    if (!addressLine.trim()) errors.addressLine = required;
    const normalizedPostal = toLatinDigits(postalCode).replace(/[\s-]/g, "");
    if (normalizedPostal && !/^\d{10}$/.test(normalizedPostal)) errors.postalCode = "کد پستی باید ۱۰ رقم باشد";
    if (!shippingMethodId) errors.shippingMethodId = "یک روش ارسال انتخاب کنید";
    return errors;
  }

  async function handleSubmit() {
    setError(undefined);
    const errors = validate();
    setFieldErrors(errors);
    if (Object.keys(errors).length > 0 || !shippingMethodId) {
      setError("لطفاً موارد مشخص‌شده را اصلاح کنید.");
      return;
    }
    let orderId: string | undefined;
    try {
      const postal = toLatinDigits(postalCode).replace(/[\s-]/g, "");
      const order = await checkout.mutateAsync({
        shippingMethodId,
        recipientName: recipientName.trim(),
        phone: toLatinDigits(phone).replace(/[\s-]/g, ""),
        province: province.trim(),
        city: city.trim(),
        addressLine: addressLine.trim(),
        postalCode: postal || undefined,
      });
      orderId = order.id;
      const payment = await payOrder.mutateAsync(order.id);
      await payAndWaitForCallback(payment.redirectUrl, payment.paymentId);
      router.replace("/orders");
    } catch (e) {
      if (orderId) {
        setOrderCreatedButUnpaid(true);
        setError("سفارش شما ثبت شد ولی پرداخت آغاز نشد. وضعیت سفارش را در «سفارش‌های من» ببینید.");
      } else {
        setError(describeError(e, "ثبت سفارش ممکن نشد؛ ممکن است موجودی یا قیمت برخی کالاها تغییر کرده باشد. سبد خرید را بررسی کنید."));
      }
    }
  }

  const isPending = checkout.isPending || payOrder.isPending;

  return (
    <Screen>
      <Stack.Screen options={{ headerShown: true, title: "تسویه حساب" }} />
      <Field
        label="نام گیرنده"
        value={recipientName}
        onChangeText={onChange("recipientName", setRecipientName)}
        error={fieldErrors.recipientName}
        autoComplete="name"
        textContentType="name"
      />
      <Field
        label="موبایل"
        value={phone}
        onChangeText={onChange("phone", setPhone)}
        error={fieldErrors.phone}
        keyboardType="phone-pad"
        autoComplete="tel"
        textContentType="telephoneNumber"
        maxLength={13}
      />
      <Field label="استان" value={province} onChangeText={onChange("province", setProvince)} error={fieldErrors.province} />
      <Field label="شهر" value={city} onChangeText={onChange("city", setCity)} error={fieldErrors.city} />
      <Field
        label="آدرس"
        value={addressLine}
        onChangeText={onChange("addressLine", setAddressLine)}
        error={fieldErrors.addressLine}
        autoComplete="street-address"
        textContentType="fullStreetAddress"
        multiline
      />
      <Field
        label="کد پستی"
        optional
        value={postalCode}
        onChangeText={onChange("postalCode", setPostalCode)}
        error={fieldErrors.postalCode}
        keyboardType="number-pad"
        autoComplete="postal-code"
        textContentType="postalCode"
        maxLength={11}
      />

      <ThemedText variant="caption" muted>
        روش ارسال
      </ThemedText>
      {isLoading ? (
        <ActivityIndicator color={colors.accent} />
      ) : isError ? (
        <ErrorState onRetry={refetch} />
      ) : (
        <View style={{ gap: spacing.sm }} accessibilityRole="radiogroup">
          {(shippingMethods ?? []).map((m) => {
            const selected = m.id === shippingMethodId;
            return (
              <Pressable
                key={m.id}
                onPress={() => onChange("shippingMethodId", setShippingMethodId)(m.id)}
                accessibilityRole="radio"
                accessibilityState={{ checked: selected }}
                style={{
                  flexDirection: "row",
                  justifyContent: "space-between",
                  alignItems: "center",
                  gap: spacing.md,
                  padding: spacing.md,
                  borderRadius: radius.field,
                  borderWidth: 1,
                  borderColor: selected ? colors.accent : fieldErrors.shippingMethodId ? palette.danger : colors.border,
                  backgroundColor: selected ? colors.surface : "transparent",
                }}
              >
                <View
                  style={{
                    width: 20,
                    height: 20,
                    borderRadius: 10,
                    borderWidth: 2,
                    borderColor: selected ? colors.accent : colors.textMuted,
                    alignItems: "center",
                    justifyContent: "center",
                  }}
                >
                  {selected ? <View style={{ width: 10, height: 10, borderRadius: 5, backgroundColor: colors.accent }} /> : null}
                </View>
                <ThemedText style={{ flex: 1 }}>{m.name}</ThemedText>
                <ThemedText muted>{formatRial(m.baseRate)}</ThemedText>
              </Pressable>
            );
          })}
          {fieldErrors.shippingMethodId ? (
            <ThemedText variant="caption" color={palette.danger}>
              {fieldErrors.shippingMethodId}
            </ThemedText>
          ) : null}
        </View>
      )}

      {/* قبلاً کاربر بدون دیدن جمع کالاها + هزینه‌ی ارسال مستقیم به درگاه پرداخت می‌رفت. */}
      <Card>
        <SummaryRow label="جمع کالاها" value={formatRial(subtotal)} />
        <SummaryRow
          label="هزینه‌ی ارسال"
          value={selectedMethod ? `${weightBased ? "از " : ""}${formatRial(selectedMethod.baseRate)}` : "پس از انتخاب روش ارسال"}
        />
        <View style={{ height: 1, backgroundColor: colors.border, marginVertical: spacing.xs }} />
        <SummaryRow
          label={weightBased ? "مبلغ قابل پرداخت (تقریبی)" : "مبلغ قابل پرداخت"}
          value={formatRial(subtotal + (selectedMethod ? Number(selectedMethod.baseRate) : 0))}
          strong
        />
        {weightBased ? (
          <ThemedText variant="caption" muted>
            هزینه‌ی نهایی ارسال بر اساس وزن سفارش محاسبه می‌شود.
          </ThemedText>
        ) : null}
      </Card>

      {error ? <ThemedText color={palette.danger}>{error}</ThemedText> : null}
      {orderCreatedButUnpaid ? (
        <Button title="سفارش‌های من" variant="gold" onPress={() => router.replace("/orders")} />
      ) : (
        <Button title="پرداخت" variant="gold" loading={isPending} onPress={handleSubmit} />
      )}
    </Screen>
  );
}
