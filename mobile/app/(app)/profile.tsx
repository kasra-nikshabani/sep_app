import { View } from "react-native";
import { Stack } from "expo-router";
import { Screen, Card } from "@/components/Screen";
import { ThemedText } from "@/components/ThemedText";
import { Button } from "@/components/Button";
import { spacing } from "@/theme";
import { useTheme } from "@/hooks/useTheme";
import { useMe } from "@/features/users/api";
import { useAuth } from "@/lib/auth";
import { formatDate } from "@/lib/format";

function Row({ label, value }: { label: string; value: string }) {
  return (
    <View style={{ flexDirection: "row-reverse", justifyContent: "space-between" }}>
      <ThemedText muted>{label}</ThemedText>
      <ThemedText>{value}</ThemedText>
    </View>
  );
}

export default function ProfileScreen() {
  const { colors } = useTheme();
  const { data: me } = useMe();
  const { session, logout } = useAuth();

  return (
    <Screen>
      <Stack.Screen options={{ headerShown: true, title: "پروفایل" }} />
      <Card style={{ gap: spacing.sm }}>
        <ThemedText variant="h2">{me?.displayName ?? "—"}</ThemedText>
        <Row label="موبایل" value={me?.phoneNumber ?? "—"} />
        <Row label="کد ملی" value={me?.nationalCode ?? "—"} />
        <Row label="شماره عضویت" value={me?.membershipNumber ?? "—"} />
        <Row label="شهر" value={me?.city ?? "—"} />
        <Row label="تاریخ عضویت" value={formatDate(me?.joinedAt)} />
        {session?.roles.includes("vip") && <Row label="نوع عضویت" value="VIP" />}
      </Card>
      <Button title="خروج از حساب" variant="danger" onPress={logout} />
    </Screen>
  );
}
