import { View, FlatList, Pressable, ActivityIndicator } from "react-native";
import { Link, Stack } from "expo-router";
import { Screen, Card } from "@/components/Screen";
import { ThemedText } from "@/components/ThemedText";
import { ErrorState } from "@/components/ErrorState";
import { TicketIcon } from "@/components/Icon";
import { spacing } from "@/theme";
import { useTheme } from "@/hooks/useTheme";
import { useEvents } from "@/features/tickets/api";
import { formatDateTime, formatRial } from "@/lib/format";

export default function TicketsScreen() {
  const { colors } = useTheme();
  const { data, isLoading, isError, refetch } = useEvents();

  return (
    <Screen scroll={false}>
      <Stack.Screen
        options={{
          headerShown: true,
          title: "بلیط تئاتر",
          headerRight: () => (
            <Link href="/my-tickets" asChild>
              <Pressable accessibilityRole="button" accessibilityLabel="بلیط‌های من" hitSlop={8} style={{ padding: spacing.sm }}>
                <TicketIcon color={colors.accent} />
              </Pressable>
            </Link>
          ),
        }}
      />
      <FlatList
        contentContainerStyle={{ padding: spacing.lg, gap: spacing.md }}
        data={data ?? []}
        keyExtractor={(e) => e.id}
        ItemSeparatorComponent={() => <View style={{ height: spacing.md }} />}
        ListEmptyComponent={
          isLoading ? (
            <ActivityIndicator color={colors.accent} />
          ) : isError ? (
            <ErrorState onRetry={refetch} />
          ) : (
            <ThemedText muted>رویدادی موجود نیست</ThemedText>
          )
        }
        renderItem={({ item }) => (
          <Link href={`/event-seats/${item.id}`} asChild>
            <Pressable>
              <Card>
                <ThemedText variant="h2" style={{ fontSize: 17 }}>
                  {item.title}
                </ThemedText>
                <ThemedText variant="caption" muted>
                  {item.venueName} · {formatDateTime(item.startsAt)}
                </ThemedText>
                <ThemedText variant="numeric" color={colors.goldText}>
                  از {formatRial(item.basePrice)}
                </ThemedText>
              </Card>
            </Pressable>
          </Link>
        )}
      />
    </Screen>
  );
}
