import { FlatList, View, ActivityIndicator } from "react-native";
import { Screen, Card } from "@/components/Screen";
import { ThemedText } from "@/components/ThemedText";
import { BrandMark } from "@/components/BrandMark";
import { Pill } from "@/components/Pill";
import { ErrorState } from "@/components/ErrorState";
import { HistoryIcon, ShopIcon, TicketIcon, type IconComponent } from "@/components/Icon";
import { palette, radius, spacing } from "@/theme";
import { useTheme } from "@/hooks/useTheme";
import { useMyOrders, ORDER_STATUS, type Order } from "@/features/shop/api";
import { useMyTickets, TICKET_STATUS, type Ticket } from "@/features/tickets/api";
import { formatDateTime, formatRial, toPersianDigits } from "@/lib/format";

type Activity = {
  key: string;
  date: string;
  Icon: IconComponent;
  title: string;
  detail: string;
  amount?: string;
  status: { label: string; tone: "success" | "warning" | "danger" | "info" };
};

// Backend هنوز Endpoint تجمیعی برای سوابق ندارد -- فعلاً دو API موجود (سفارش‌ها و بلیط‌ها) همین‌جا
// ادغام می‌شوند؛ سرویس‌های «به‌زودی» (شارژ، قبض و...) با راه‌اندازی به همین فهرست اضافه می‌شوند.
function fromOrder(order: Order): Activity {
  return {
    key: `order-${order.id}`,
    date: order.createdAt,
    Icon: ShopIcon,
    title: "خرید از فروشگاه",
    detail: order.items.map((i) => `${i.productName} × ${toPersianDigits(i.quantity)}`).join("، "),
    amount: order.totalAmount,
    status: ORDER_STATUS[order.status] ?? { label: order.status, tone: "info" },
  };
}

function fromTicket(ticket: Ticket): Activity {
  return {
    key: `ticket-${ticket.id}`,
    date: ticket.issuedAt,
    Icon: TicketIcon,
    title: "خرید بلیط تئاتر",
    detail: `کد بلیط: ${ticket.ticketNumber}`,
    status: TICKET_STATUS[ticket.status],
  };
}

function ActivityRow({ activity }: { activity: Activity }) {
  const { colors } = useTheme();
  return (
    <Card style={{ flexDirection: "row", alignItems: "flex-start", gap: spacing.md }}>
      <View
        style={{
          width: 44,
          height: 44,
          borderRadius: radius.card,
          backgroundColor: palette.n900,
          alignItems: "center",
          justifyContent: "center",
        }}
      >
        <activity.Icon color={palette.n0} size={22} />
      </View>
      <View style={{ flex: 1, gap: 4 }}>
        <ThemedText style={{ fontFamily: "Vazirmatn-Medium" }}>{activity.title}</ThemedText>
        <ThemedText variant="caption" muted numberOfLines={1}>
          {activity.detail}
        </ThemedText>
        <ThemedText variant="caption" muted>
          {formatDateTime(activity.date)}
        </ThemedText>
      </View>
      <View style={{ alignItems: "flex-end", gap: spacing.sm }}>
        {activity.amount ? (
          <ThemedText style={{ fontFamily: "Vazirmatn-Medium" }} color={colors.goldText}>
            {formatRial(activity.amount)}
          </ThemedText>
        ) : null}
        <View style={{ alignSelf: "flex-end" }}>
          <Pill label={activity.status.label} tone={activity.status.tone} />
        </View>
      </View>
    </Card>
  );
}

export default function HistoryScreen() {
  const { colors } = useTheme();
  const orders = useMyOrders();
  const tickets = useMyTickets();

  const activities = [...(orders.data ?? []).map(fromOrder), ...(tickets.data ?? []).map(fromTicket)].sort(
    (a, b) => Date.parse(b.date) - Date.parse(a.date),
  );
  const isLoading = orders.isLoading || tickets.isLoading;
  const isError = orders.isError || tickets.isError;

  const retry = () => {
    if (orders.isError) orders.refetch();
    if (tickets.isError) tickets.refetch();
  };

  return (
    <Screen scroll={false}>
      <FlatList
        contentContainerStyle={{ padding: spacing.lg, gap: spacing.md }}
        data={isLoading ? [] : activities}
        keyExtractor={(a) => a.key}
        ListHeaderComponent={
          <View style={{ gap: spacing.md }}>
            <BrandMark />
            <ThemedText variant="h1">سوابق</ThemedText>
            {/* خطای یکی از دو منبع هم نمایش داده می‌شود -- وگرنه فهرست ناقص، کامل به نظر می‌رسید. */}
            {isError ? <ErrorState onRetry={retry} /> : null}
          </View>
        }
        ListEmptyComponent={
          isLoading ? (
            <ActivityIndicator color={colors.accent} />
          ) : isError ? null : (
            <View style={{ alignItems: "center", gap: spacing.sm, paddingVertical: spacing.xl }}>
              <HistoryIcon color={colors.textMuted} size={40} />
              <ThemedText muted>هنوز فعالیتی ثبت نشده</ThemedText>
              <ThemedText variant="caption" muted style={{ textAlign: "center" }}>
                خریدها و پرداخت‌هایی که در اپ انجام می‌دهید این‌جا نمایش داده می‌شوند.
              </ThemedText>
            </View>
          )
        }
        renderItem={({ item }) => <ActivityRow activity={item} />}
      />
    </Screen>
  );
}
