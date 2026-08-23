"use client";

import { AntdRegistry } from "@ant-design/nextjs-registry";
import { ConfigProvider } from "antd";
import faIR from "antd/locale/fa_IR";
import { lightTheme, darkTheme } from "@/theme";
import { ThemeModeProvider, useThemeMode } from "./theme-mode";

function ConfiguredAntd({ children }: { children: React.ReactNode }) {
  const { mode } = useThemeMode();
  return (
    <ConfigProvider direction="rtl" locale={faIR} theme={mode === "dark" ? darkTheme : lightTheme}>
      {children}
    </ConfigProvider>
  );
}

export function Providers({ children }: { children: React.ReactNode }) {
  return (
    <AntdRegistry>
      <ThemeModeProvider>
        <ConfiguredAntd>{children}</ConfiguredAntd>
      </ThemeModeProvider>
    </AntdRegistry>
  );
}
