import type { FC } from "react";
import Svg, { Circle, Path, Rect } from "react-native-svg";

// دقیقاً همان مسیرهای SVG استفاده‌شده در docs/architecture/ui-ux/design-system.html
// (بخش «ناوبری پایین — اپ موبایل») -- برای هماهنگی بصری کامل با Design System.

type IconProps = { color: string; size?: number };
export type IconComponent = FC<IconProps>;

export function HomeIcon({ color, size = 22 }: IconProps) {
  return (
    <Svg width={size} height={size} viewBox="0 0 24 24" fill="none" stroke={color} strokeWidth={2}>
      <Path d="M3 11l9-7 9 7" />
      <Path d="M5 10v9h14v-9" />
    </Svg>
  );
}

export function NewsIcon({ color, size = 22 }: IconProps) {
  return (
    <Svg width={size} height={size} viewBox="0 0 24 24" fill="none" stroke={color} strokeWidth={2}>
      <Rect x={3} y={5} width={18} height={14} rx={2} />
      <Path d="M3 10h18" />
    </Svg>
  );
}

export function TicketIcon({ color, size = 22 }: IconProps) {
  return (
    <Svg width={size} height={size} viewBox="0 0 24 24" fill="none" stroke={color} strokeWidth={2}>
      <Path d="M4 8a2 2 0 0 1 2-2h12a2 2 0 0 1 2 2v2a2 2 0 0 0 0 4v2a2 2 0 0 1-2 2H6a2 2 0 0 1-2-2v-2a2 2 0 0 0 0-4Z" />
    </Svg>
  );
}

export function ShopIcon({ color, size = 22 }: IconProps) {
  return (
    <Svg width={size} height={size} viewBox="0 0 24 24" fill="none" stroke={color} strokeWidth={2}>
      <Circle cx={9} cy={20} r={1.4} />
      <Circle cx={17} cy={20} r={1.4} />
      <Path d="M3 4h2l2.2 11.2A2 2 0 0 0 9.2 17H17a2 2 0 0 0 2-1.6L20.5 8H6" />
    </Svg>
  );
}

export function WalletIcon({ color, size = 22 }: IconProps) {
  return (
    <Svg width={size} height={size} viewBox="0 0 24 24" fill="none" stroke={color} strokeWidth={2}>
      <Rect x={3} y={6} width={18} height={13} rx={2} />
      <Path d="M16 12h2" />
      <Path d="M3 10h18" />
    </Svg>
  );
}

export function ProfileIcon({ color, size = 22 }: IconProps) {
  return (
    <Svg width={size} height={size} viewBox="0 0 24 24" fill="none" stroke={color} strokeWidth={2}>
      <Circle cx={12} cy={8} r={4} />
      <Path d="M4 20c0-4 4-6 8-6s8 2 8 6" />
    </Svg>
  );
}

export function BellIcon({ color, size = 22 }: IconProps) {
  return (
    <Svg width={size} height={size} viewBox="0 0 24 24" fill="none" stroke={color} strokeWidth={2}>
      <Path d="M6 8a6 6 0 0 1 12 0c0 5 2 6 2 6H4s2-1 2-6Z" />
      <Path d="M10 20a2 2 0 0 0 4 0" />
    </Svg>
  );
}

export function GiftIcon({ color, size = 22 }: IconProps) {
  return (
    <Svg width={size} height={size} viewBox="0 0 24 24" fill="none" stroke={color} strokeWidth={2}>
      <Rect x={3} y={8} width={18} height={13} rx={1} />
      <Path d="M3 12h18" />
      <Path d="M12 8v13" />
      <Path d="M12 8c-2 0-4-1-4-3s2-3 4-1c2-2 4-1 4 1s-2 3-4 3Z" />
    </Svg>
  );
}

export function ArrowIcon({ color, size = 20 }: IconProps) {
  return (
    <Svg width={size} height={size} viewBox="0 0 24 24" fill="none" stroke={color} strokeWidth={2}>
      <Path d="M15 6l-6 6 6 6" />
    </Svg>
  );
}

export function BallIcon({ color, size = 22 }: IconProps) {
  return (
    <Svg width={size} height={size} viewBox="0 0 24 24" fill="none" stroke={color} strokeWidth={2}>
      <Circle cx={12} cy={12} r={8} />
      <Path d="M12 8.6l2.4 1.7-.9 2.8h-3l-.9-2.8L12 8.6Z" strokeLinejoin="round" />
    </Svg>
  );
}

export function ShieldIcon({ color, size = 22 }: IconProps) {
  return (
    <Svg width={size} height={size} viewBox="0 0 24 24" fill="none" stroke={color} strokeWidth={2}>
      <Path d="M12 3.5 19 6v6c0 5-3 7.6-7 8.5-4-.9-7-3.5-7-8.5V6l7-2.5Z" strokeLinejoin="round" />
      <Path d="M9.3 12.2 11.2 14l3.5-4" strokeLinecap="round" strokeLinejoin="round" />
    </Svg>
  );
}

export function SuitcaseIcon({ color, size = 22 }: IconProps) {
  return (
    <Svg width={size} height={size} viewBox="0 0 24 24" fill="none" stroke={color} strokeWidth={2}>
      <Rect x={3} y={8} width={18} height={12} rx={2} />
      <Path d="M9 8V6a2 2 0 0 1 2-2h2a2 2 0 0 1 2 2v2" />
      <Path d="M3 13h18" />
    </Svg>
  );
}

export function CarIcon({ color, size = 22 }: IconProps) {
  return (
    <Svg width={size} height={size} viewBox="0 0 24 24" fill="none" stroke={color} strokeWidth={2}>
      <Path d="M4 16v-2.6c0-.4.1-.8.3-1.1L6 9h12l1.7 3.3c.2.3.3.7.3 1.1V16" strokeLinejoin="round" />
      <Path d="M4 16h16" />
      <Circle cx={7.5} cy={18} r={1.5} />
      <Circle cx={16.5} cy={18} r={1.5} />
    </Svg>
  );
}

export function FilmIcon({ color, size = 22 }: IconProps) {
  return (
    <Svg width={size} height={size} viewBox="0 0 24 24" fill="none" stroke={color} strokeWidth={2}>
      <Rect x={3} y={4} width={18} height={16} rx={3} />
      <Path d="M10 9.3v5.4l4.5-2.7L10 9.3Z" strokeLinejoin="round" />
    </Svg>
  );
}

export function ReceiptIcon({ color, size = 22 }: IconProps) {
  return (
    <Svg width={size} height={size} viewBox="0 0 24 24" fill="none" stroke={color} strokeWidth={2}>
      <Rect x={5} y={3} width={14} height={18} rx={2} />
      <Path d="M8.5 8h7M8.5 12h7M8.5 16h4" strokeLinecap="round" />
    </Svg>
  );
}

export function BankIcon({ color, size = 22 }: IconProps) {
  return (
    <Svg width={size} height={size} viewBox="0 0 24 24" fill="none" stroke={color} strokeWidth={2}>
      <Path d="M3 10l9-5.5L21 10Z" strokeLinejoin="round" />
      <Path d="M5 10v8M9.5 10v8M14.5 10v8M19 10v8" />
      <Path d="M3.5 18h17" strokeLinecap="round" />
    </Svg>
  );
}

export function SimCardIcon({ color, size = 22 }: IconProps) {
  return (
    <Svg width={size} height={size} viewBox="0 0 24 24" fill="none" stroke={color} strokeWidth={2}>
      <Path d="M8 3h9a2 2 0 0 1 2 2v14a2 2 0 0 1-2 2H7a2 2 0 0 1-2-2V8Z" strokeLinejoin="round" />
      <Rect x={8.5} y={7.5} width={7} height={5} rx={1} />
      <Path d="M8.5 16h7" strokeLinecap="round" />
    </Svg>
  );
}

export function TvIcon({ color, size = 22 }: IconProps) {
  return (
    <Svg width={size} height={size} viewBox="0 0 24 24" fill="none" stroke={color} strokeWidth={2}>
      <Rect x={3} y={6} width={18} height={13} rx={2} />
      <Path d="M9 19.5h6M8 3l4 3 4-3" strokeLinecap="round" strokeLinejoin="round" />
      <Path d="M10.3 10.2v4.6l3.9-2.3-3.9-2.3Z" strokeLinejoin="round" />
    </Svg>
  );
}
