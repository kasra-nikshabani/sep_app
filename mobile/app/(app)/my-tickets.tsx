import { FlatList, ActivityIndicator } from "react-native";
import { Stack } from "expo-router";
import { Screen, Card } from "@/components/Screen";
import { ThemedText } from "@/components/ThemedText";
import { Pill } from "@/components/Pill";
import { ErrorState } from "@/components/ErrorState";
import { spacing } from "@/theme";
import { useTheme } from "@/hooks/useTheme";
import { useMyTickets } from "@/features/tickets/api";
import { formatDateTime } from "@/lib/format";

const statusMeta = {
  valid: { label: "معتبر", tone: "success" as const },
  used: { label: "استفاده‌شده", tone: "info" as const },
  refunded: { label: "استردادشده", tone: "danger" as const },
};

export default function MyTicketsScreen() {
  const { colors } = useTheme();
  const { data, isLoading, isError, refetch } = useMyTickets();

  return (
    <Screen scroll={false}>
      <Stack.Screen options={{ headerShown: true, title: "بلیط‌های من" }} />
      <FlatList
        contentContainerStyle={{ padding: spacing.lg, gap: spacing.md }}
        data={data ?? []}
        keyExtractor={(t) => t.id}
        ListEmptyComponent={
          isLoading ? (
            <ActivityIndicator color={colors.accent} />
          ) : isError ? (
            <ErrorState onRetry={refetch} />
          ) : (
            <ThemedText muted>بلیطی ندارید</ThemedText>
          )
        }
        renderItem={({ item }) => (
          <Card>
            <ThemedText variant="caption" color={colors.goldText}>
              کد بلیط: {item.ticketNumber}
            </ThemedText>
            <Pill label={statusMeta[item.status].label} tone={statusMeta[item.status].tone} />
            <ThemedText variant="caption" muted>
              صادرشده: {formatDateTime(item.issuedAt)}
            </ThemedText>
          </Card>
        )}
      />
    </Screen>
  );
}
