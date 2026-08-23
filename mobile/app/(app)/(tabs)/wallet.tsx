import { Screen } from "@/components/Screen";
import { ComingSoon } from "@/components/ComingSoon";

export default function WalletScreen() {
  return (
    <Screen scroll={false}>
      <ComingSoon
        title="کیف‌پول"
        reason="هیچ ماژول Wallet واقعی هنوز در Backend ساخته نشده -- طبق ADR-0002/ADR-0012، یکسان‌سازی احتمالی با کیف‌پول Django هنوز تصمیم‌گیری نشده."
      />
    </Screen>
  );
}
