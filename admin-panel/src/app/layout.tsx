import type { Metadata } from "next";
import { vazirmatn } from "./fonts/vazirmatn";
import { Providers } from "./providers";
import "./globals.css";

export const metadata: Metadata = {
  title: "پنل مدیریت — باشگاه فولاد مبارکه سپاهان",
  description: "SEPahan Super App Admin Panel",
};

export default function RootLayout({ children }: LayoutProps<"/">) {
  return (
    <html lang="fa" dir="rtl" className={vazirmatn.variable}>
      <body>
        <Providers>{children}</Providers>
      </body>
    </html>
  );
}
