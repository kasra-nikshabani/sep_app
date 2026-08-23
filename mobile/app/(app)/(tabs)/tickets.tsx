import { View, FlatList, Pressable, ActivityIndicator } from "react-native";
import { Link } from "expo-router";
import { Screen, Card } from "@/components/Screen";
import { ThemedText } from "@/components/ThemedText";
import { TicketIcon } from "@/components/Icon";
import { spacing } from "@/theme";
import { useTheme } from "@/hooks/useTheme";
import { useEvents } from "@/features/tickets/api";
import { formatDateTime, formatRial } from "@/lib/format";

export default function TicketsScreen() {
  const { colors } = useTheme();
  const { data, isLoading } = useEvents();

  return (
    <Screen scroll={false}>
      <FlatList
        contentContainerStyle={{ padding: spacing.lg, gap: spacing.md }}
        data={data ?? []}
        keyExtractor={(e) => e.id}
        ListHeaderComponent={
          <View style={{ flexDirection: "row-reverse", justifyContent: "space-between", alignItems: "center", marginBottom: spacing.md }}>
            <ThemedText variant="h1">بلیط تئاتر</ThemedText>
            <Link href="/my-tickets" asChild>
              <Pressable style={{ padding: spacing.sm }}>
                <TicketIcon color={colors.accent} />
              </Pressable>
            </Link>
          </View>
        }
        ItemSeparatorComponent={() => <View style={{ height: spacing.md }} />}
        ListEmptyComponent={isLoading ? <ActivityIndicator color={colors.accent} /> : <ThemedText muted>رویدادی موجود نیست</ThemedText>}
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
