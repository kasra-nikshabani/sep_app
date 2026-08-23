import { useState } from "react";
import { View, FlatList, ActivityIndicator, Pressable } from "react-native";
import { useLocalSearchParams, Stack, router } from "expo-router";
import { Screen } from "@/components/Screen";
import { ThemedText } from "@/components/ThemedText";
import { Button } from "@/components/Button";
import { spacing, radius } from "@/theme";
import { useTheme } from "@/hooks/useTheme";
import { useEventSeats, useReserveSeat, usePayReservation, useCancelReservation, type EventSeat } from "@/features/tickets/api";
import { payAndWaitForCallback } from "@/lib/payment";
import { formatRial } from "@/lib/format";

export default function EventSeatsScreen() {
  const { eventId } = useLocalSearchParams<{ eventId: string }>();
  const { colors } = useTheme();
  const { data: seats, isLoading } = useEventSeats(eventId);
  const reserveSeat = useReserveSeat();
  const cancelReservation = useCancelReservation();
  const payReservation = usePayReservation();
  const [reservation, setReservation] = useState<{ id: string; seat: EventSeat } | null>(null);
  const [error, setError] = useState<string>();

  async function handleReserve(seat: EventSeat) {
    setError(undefined);
    try {
      const result = await reserveSeat.mutateAsync(seat.id);
      setReservation({ id: result.id, seat });
    } catch (e) {
      setError(e instanceof Error ? e.message : "این صندلی دیگر در دسترس نیست");
    }
  }

  async function handleCancel() {
    if (!reservation) return;
    await cancelReservation.mutateAsync(reservation.id);
    setReservation(null);
  }

  async function handlePay() {
    if (!reservation) return;
    const payment = await payReservation.mutateAsync(reservation.id);
    await payAndWaitForCallback(payment.redirectUrl, payment.paymentId);
    router.replace("/my-tickets");
  }

  if (reservation) {
    return (
      <Screen>
        <Stack.Screen options={{ headerShown: true, title: "پرداخت بلیط" }} />
        <ThemedText variant="h1">
          سکو {reservation.seat.section} · ردیف {reservation.seat.rowLabel} · صندلی {reservation.seat.seatNumber}
        </ThemedText>
        <ThemedText variant="numeric" color={colors.goldText}>
          {formatRial(reservation.seat.price)}
        </ThemedText>
        <ThemedText variant="caption" muted>
          این صندلی برای مدت محدودی برای شما رزرو شده -- پرداخت را کامل کنید.
        </ThemedText>
        <Button title="پرداخت" variant="gold" loading={payReservation.isPending} onPress={handlePay} />
        <Button title="انصراف" variant="ghost" onPress={handleCancel} />
      </Screen>
    );
  }

  return (
    <Screen scroll={false}>
      <Stack.Screen options={{ headerShown: true, title: "انتخاب صندلی" }} />
      {error && (
        <ThemedText style={{ padding: spacing.lg }} color={colors.textMuted}>
          {error}
        </ThemedText>
      )}
      <FlatList
        contentContainerStyle={{ padding: spacing.lg, gap: spacing.sm }}
        data={seats ?? []}
        keyExtractor={(s) => s.id}
        ListEmptyComponent={isLoading ? <ActivityIndicator color={colors.accent} /> : <ThemedText muted>صندلی موجود نیست</ThemedText>}
        renderItem={({ item }) => (
          <Pressable
            disabled={item.status !== "available"}
            onPress={() => handleReserve(item)}
            style={{
              flexDirection: "row-reverse",
              justifyContent: "space-between",
              alignItems: "center",
              padding: spacing.md,
              borderRadius: radius.field,
              borderWidth: 1,
              borderColor: colors.border,
              opacity: item.status === "available" ? 1 : 0.4,
            }}
          >
            <ThemedText>
              سکو {item.section} · ردیف {item.rowLabel} · صندلی {item.seatNumber}
            </ThemedText>
            <ThemedText color={colors.goldText}>{formatRial(item.price)}</ThemedText>
          </Pressable>
        )}
      />
    </Screen>
  );
}
