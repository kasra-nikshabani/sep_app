import { useEffect, useState } from "react";
import { FlatList, ActivityIndicator, Pressable } from "react-native";
import { useLocalSearchParams, Stack, router } from "expo-router";
import { Screen } from "@/components/Screen";
import { ThemedText } from "@/components/ThemedText";
import { Button } from "@/components/Button";
import { ErrorState } from "@/components/ErrorState";
import { ConfirmDialog } from "@/components/ConfirmDialog";
import { spacing, radius, palette } from "@/theme";
import { useTheme } from "@/hooks/useTheme";
import { useEventSeats, useReserveSeat, usePayReservation, useCancelReservation, type EventSeat } from "@/features/tickets/api";
import { describeError } from "@/lib/api";
import { payAndWaitForCallback } from "@/lib/payment";
import { formatRial, toPersianDigits } from "@/lib/format";

const UNAVAILABLE_LABEL: Record<Exclude<EventSeat["status"], "available">, string> = {
  held: "رزرو موقت",
  sold: "فروخته‌شده",
};

function seatLabel(seat: EventSeat): string {
  return `سکو ${seat.section} · ردیف ${toPersianDigits(seat.rowLabel)} · صندلی ${toPersianDigits(seat.seatNumber)}`;
}

// Backend مهلت رزرو (expiresAt) را برمی‌گرداند ولی قبلاً نمایش داده نمی‌شد -- کاربر فقط
// «برای مدت محدودی» می‌دید و بعد از انقضا، پرداخت بی‌دلیل شکست می‌خورد.
function useSecondsLeft(expiresAt: string | null): number | null {
  const [now, setNow] = useState(() => Date.now());
  useEffect(() => {
    if (!expiresAt) return;
    setNow(Date.now());
    const timer = setInterval(() => setNow(Date.now()), 1000);
    return () => clearInterval(timer);
  }, [expiresAt]);
  const expiresAtMs = expiresAt ? new Date(expiresAt).getTime() : NaN;
  if (Number.isNaN(expiresAtMs)) return null;
  return Math.max(0, Math.floor((expiresAtMs - now) / 1000));
}

function formatCountdown(seconds: number): string {
  const mm = String(Math.floor(seconds / 60)).padStart(2, "0");
  const ss = String(seconds % 60).padStart(2, "0");
  return toPersianDigits(`${mm}:${ss}`);
}

export default function EventSeatsScreen() {
  const { eventId } = useLocalSearchParams<{ eventId: string }>();
  const { colors } = useTheme();
  const { data: seats, isLoading, isError, refetch } = useEventSeats(eventId);
  const reserveSeat = useReserveSeat();
  const cancelReservation = useCancelReservation();
  const payReservation = usePayReservation();
  // قبلاً لمس یک صندلی بلافاصله رزروش می‌کرد -- حالا اول تأیید (با مبلغ) گرفته می‌شود.
  const [seatToConfirm, setSeatToConfirm] = useState<EventSeat | null>(null);
  const [reservation, setReservation] = useState<{ id: string; seat: EventSeat; expiresAt: string } | null>(null);
  const [error, setError] = useState<string>();
  const secondsLeft = useSecondsLeft(reservation?.expiresAt ?? null);
  const expired = secondsLeft === 0;

  function handleReserve() {
    if (!seatToConfirm) return;
    const seat = seatToConfirm;
    setError(undefined);
    reserveSeat.mutate(seat.id, {
      onSuccess: (result) => {
        setSeatToConfirm(null);
        setReservation({ id: result.id, seat, expiresAt: result.expiresAt });
      },
      onError: (e) => {
        setSeatToConfirm(null);
        setError(describeError(e, "این صندلی دیگر در دسترس نیست. لطفاً صندلی دیگری انتخاب کنید."));
        refetch();
      },
    });
  }

  function backToSeats() {
    setReservation(null);
    setError(undefined);
    refetch();
  }

  async function handleCancel() {
    if (!reservation) return;
    try {
      await cancelReservation.mutateAsync(reservation.id);
    } catch {
      // لغو ناموفق مشکلی ایجاد نمی‌کند: رزرو در هر صورت با رسیدن expiresAt آزاد می‌شود.
    }
    backToSeats();
  }

  async function handlePay() {
    if (!reservation) return;
    setError(undefined);
    try {
      const payment = await payReservation.mutateAsync(reservation.id);
      await payAndWaitForCallback(payment.redirectUrl, payment.paymentId);
      router.replace("/my-tickets");
    } catch (e) {
      setError(describeError(e, "شروع پرداخت ممکن نشد؛ ممکن است مهلت رزرو به پایان رسیده باشد."));
    }
  }

  if (reservation) {
    return (
      <Screen>
        <Stack.Screen options={{ headerShown: true, title: "پرداخت بلیط" }} />
        <ThemedText variant="h1">{seatLabel(reservation.seat)}</ThemedText>
        <ThemedText variant="numeric" color={colors.goldText}>
          {formatRial(reservation.seat.price)}
        </ThemedText>
        {expired ? (
          <ThemedText color={palette.danger}>مهلت رزرو به پایان رسید و صندلی آزاد شد.</ThemedText>
        ) : (
          <ThemedText muted>
            این صندلی برای شما نگه داشته شده است. زمان باقی‌مانده برای پرداخت:{" "}
            <ThemedText style={{ fontFamily: "Vazirmatn-Medium" }}>{secondsLeft === null ? "—" : formatCountdown(secondsLeft)}</ThemedText>
          </ThemedText>
        )}
        {error ? <ThemedText color={palette.danger}>{error}</ThemedText> : null}
        {expired ? (
          <Button title="انتخاب دوباره‌ی صندلی" variant="gold" onPress={backToSeats} />
        ) : (
          <>
            <Button title="پرداخت" variant="gold" loading={payReservation.isPending} onPress={handlePay} />
            <Button title="انصراف" variant="ghost" disabled={payReservation.isPending} onPress={handleCancel} />
          </>
        )}
      </Screen>
    );
  }

  return (
    <Screen scroll={false}>
      <Stack.Screen options={{ headerShown: true, title: "انتخاب صندلی" }} />
      {error && (
        <ThemedText style={{ padding: spacing.lg, paddingBottom: 0 }} color={palette.danger}>
          {error}
        </ThemedText>
      )}
      <FlatList
        contentContainerStyle={{ padding: spacing.lg, gap: spacing.sm }}
        data={seats ?? []}
        keyExtractor={(s) => s.id}
        ListEmptyComponent={
          isLoading ? (
            <ActivityIndicator color={colors.accent} />
          ) : isError ? (
            <ErrorState onRetry={refetch} />
          ) : (
            <ThemedText muted>صندلی موجود نیست</ThemedText>
          )
        }
        renderItem={({ item }) => {
          const available = item.status === "available";
          return (
            <Pressable
              disabled={!available}
              onPress={() => setSeatToConfirm(item)}
              accessibilityRole="button"
              accessibilityState={{ disabled: !available }}
              style={{
                flexDirection: "row",
                justifyContent: "space-between",
                alignItems: "center",
                padding: spacing.md,
                borderRadius: radius.field,
                borderWidth: 1,
                borderColor: colors.border,
                opacity: available ? 1 : 0.45,
              }}
            >
              <ThemedText>{seatLabel(item)}</ThemedText>
              {item.status === "available" ? (
                <ThemedText color={colors.goldText}>{formatRial(item.price)}</ThemedText>
              ) : (
                <ThemedText variant="caption" muted>
                  {UNAVAILABLE_LABEL[item.status]}
                </ThemedText>
              )}
            </Pressable>
          );
        }}
      />

      <ConfirmDialog
        visible={!!seatToConfirm}
        title="رزرو صندلی"
        message={
          seatToConfirm
            ? `${seatLabel(seatToConfirm)} به مبلغ ${formatRial(seatToConfirm.price)}. صندلی برای مدت کوتاهی برای شما نگه داشته می‌شود تا پرداخت را کامل کنید.`
            : ""
        }
        confirmTitle="رزرو و ادامه"
        loading={reserveSeat.isPending}
        onConfirm={handleReserve}
        onCancel={() => setSeatToConfirm(null)}
      />
    </Screen>
  );
}
