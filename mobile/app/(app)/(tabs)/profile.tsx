import { View, Pressable } from "react-native";
import { Link } from "expo-router";
import { Screen, Card } from "@/components/Screen";
import { ThemedText } from "@/components/ThemedText";
import { Button } from "@/components/Button";
import { spacing, palette } from "@/theme";
import { useTheme } from "@/hooks/useTheme";
import { useMe } from "@/features/users/api";
import { useAuth } from "@/lib/auth";
import { formatDate, toPersianDigits } from "@/lib/format";
import { ProfileIcon, BellIcon, ReceiptIcon, TicketIcon, GiftIcon, ArrowIcon, type IconComponent } from "@/components/Icon";
import { BrandMark } from "@/components/BrandMark";

function Row({ label, value }: { label: string; value: string }) {
  return (
    <View style={{ flexDirection: "row", justifyContent: "space-between" }}>
      <ThemedText muted>{label}</ThemedText>
      <ThemedText>{value}</ThemedText>
    </View>
  );
}

function AccountLink({ href, label, Icon }: { href: string; label: string; Icon: IconComponent }) {
  const { colors } = useTheme();
  return (
    <Link href={href as never} asChild>
      <Pressable
        style={{
          flexDirection: "row",
          alignItems: "center",
          justifyContent: "space-between",
          paddingVertical: spacing.md,
        }}
      >
        <View style={{ flexDirection: "row", alignItems: "center", gap: spacing.sm }}>
          <Icon color={palette.n900} size={18} />
          <ThemedText>{label}</ThemedText>
        </View>
        <ArrowIcon color={colors.textMuted} size={16} />
      </Pressable>
    </Link>
  );
}

export default function ProfileScreen() {
  const { colors } = useTheme();
  const { data: me } = useMe();
  const { session, logout } = useAuth();

  return (
    <Screen>
      <BrandMark />

      <View style={{ alignItems: "center", gap: spacing.sm }}>
        <View
          style={{
            width: 72,
            height: 72,
            borderRadius: 36,
            backgroundColor: colors.surfaceRaised,
            borderWidth: 1,
            borderColor: colors.border,
            alignItems: "center",
            justifyContent: "center",
          }}
        >
          <ProfileIcon color={palette.n900} size={34} />
        </View>
        <ThemedText variant="h2">{me?.displayName ?? session?.displayName ?? "—"}</ThemedText>
      </View>

      <Card style={{ gap: spacing.sm }}>
        <Row label="موبایل" value={toPersianDigits(me?.phoneNumber)} />
        <Row label="کد ملی" value={toPersianDigits(me?.nationalCode)} />
        <Row label="شماره عضویت" value={me?.membershipNumber ?? "—"} />
        <Row label="شهر" value={me?.city ?? "—"} />
        <Row label="تاریخ عضویت" value={formatDate(me?.joinedAt)} />
        {session?.roles.includes("vip") && <Row label="نوع عضویت" value="VIP" />}
      </Card>

      <Card style={{ paddingVertical: spacing.xs }}>
        <AccountLink href="/notifications" label="اعلان‌ها" Icon={BellIcon} />
        <AccountLink href="/orders" label="سفارش‌های من" Icon={ReceiptIcon} />
        <AccountLink href="/my-tickets" label="بلیط‌های من" Icon={TicketIcon} />
        <AccountLink href="/loyalty" label="باشگاه امتیاز" Icon={GiftIcon} />
      </Card>

      <Button title="خروج از حساب" variant="danger" onPress={logout} />
    </Screen>
  );
}
