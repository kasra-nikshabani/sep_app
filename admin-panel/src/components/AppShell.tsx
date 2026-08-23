"use client";

import { useMemo, useState } from "react";
import { usePathname } from "next/navigation";
import Link from "next/link";
import { Layout, Menu, Button, Dropdown, Space, Typography } from "antd";
import {
  BulbOutlined,
  LogoutOutlined,
  MoonOutlined,
  UserOutlined,
} from "@ant-design/icons";
import { NAV_GROUPS } from "@/lib/nav";
import { useThemeMode } from "@/app/theme-mode";
import { signOutAction } from "@/app/actions/sign-out";

const { Sider, Header, Content } = Layout;

export function AppShell({
  children,
  displayName,
}: {
  children: React.ReactNode;
  displayName: string;
}) {
  const pathname = usePathname();
  const [collapsed, setCollapsed] = useState(false);
  const { mode, toggle } = useThemeMode();

  const menuItems = useMemo(
    () =>
      NAV_GROUPS.map((group) => ({
        key: group.key,
        label: group.label,
        type: "group" as const,
        children: group.items.map((item) => ({
          key: item.href,
          label: <Link href={item.href}>{item.label}</Link>,
        })),
      })),
    [],
  );

  return (
    <Layout style={{ minHeight: "100vh" }}>
      <Sider collapsible collapsed={collapsed} onCollapse={setCollapsed} theme="dark" width={240}>
        <div
          style={{
            height: 56,
            margin: 12,
            display: "flex",
            alignItems: "center",
            justifyContent: "center",
            color: "#E8A80E",
            fontWeight: 900,
            fontSize: collapsed ? 14 : 18,
          }}
        >
          {collapsed ? "سپاهان" : "پنل مدیریت سپاهان"}
        </div>
        <Menu
          theme="dark"
          mode="inline"
          selectedKeys={[pathname]}
          items={menuItems}
        />
      </Sider>
      <Layout>
        <Header
          style={{
            display: "flex",
            alignItems: "center",
            justifyContent: "space-between",
            paddingInline: 20,
          }}
        >
          <Typography.Text strong>سامانه مدیریت — باشگاه فولاد مبارکه سپاهان</Typography.Text>
          <Space size="middle">
            <Button
              type="text"
              icon={mode === "dark" ? <BulbOutlined /> : <MoonOutlined />}
              onClick={toggle}
              aria-label="تغییر پوسته"
            />
            <Dropdown
              menu={{
                items: [
                  {
                    key: "signout",
                    icon: <LogoutOutlined />,
                    label: (
                      <form action={signOutAction}>
                        <button type="submit" style={{ all: "unset", cursor: "pointer" }}>
                          خروج
                        </button>
                      </form>
                    ),
                  },
                ],
              }}
            >
              <Space style={{ cursor: "pointer" }}>
                <UserOutlined />
                {displayName}
              </Space>
            </Dropdown>
          </Space>
        </Header>
        <Content style={{ margin: 20 }}>{children}</Content>
      </Layout>
    </Layout>
  );
}
