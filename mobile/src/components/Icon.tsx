import Svg, { Circle, Path, Rect } from "react-native-svg";

// دقیقاً همان مسیرهای SVG استفاده‌شده در docs/architecture/ui-ux/design-system.html
// (بخش «ناوبری پایین — اپ موبایل») -- برای هماهنگی بصری کامل با Design System.

type IconProps = { color: string; size?: number };

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
