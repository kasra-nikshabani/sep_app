import type { FC } from "react";
import Svg, { Circle, Path, Rect } from "react-native-svg";

// دقیقاً همان مسیرهای SVG استفاده‌شده در docs/architecture/ui-ux/design-system.html
// (بخش «ناوبری پایین — اپ موبایل») -- برای هماهنگی بصری کامل با Design System.

type IconProps = { color: string; size?: number };
export type IconComponent = FC<IconProps>;

const ROUND = { strokeLinecap: "round", strokeLinejoin: "round" } as const;

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
      <Path d="M12 8.6V4M14.4 10.3l5.2-.8M13.5 13.1l3.2 5.4M10.5 13.1l-3.2 5.4M9.6 10.3l-5.2-.8" {...ROUND} />
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

export function CarIcon({ color, size = 22 }: IconProps) {
  return (
    <Svg width={size} height={size} viewBox="0 0 24 24" fill="none" stroke={color} strokeWidth={2}>
      <Path d="M5 11l1.7-4.4A2 2 0 0 1 8.6 5.3h6.8a2 2 0 0 1 1.9 1.3L19 11" {...ROUND} />
      <Path d="M3 17.5V13a2 2 0 0 1 2-2h14a2 2 0 0 1 2 2v4.5ZM6 17.5V20M18 17.5V20" {...ROUND} />
      <Circle cx={7} cy={14.2} r={1} fill={color} stroke="none" />
      <Circle cx={17} cy={14.2} r={1} fill={color} stroke="none" />
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

export function CardTransferIcon({ color, size = 22 }: IconProps) {
  return (
    <Svg width={size} height={size} viewBox="0 0 24 24" fill="none" stroke={color} strokeWidth={2}>
      <Rect x={2} y={3} width={11} height={8} rx={1.5} />
      <Rect x={11} y={13} width={11} height={8} rx={1.5} />
      <Path d="M16 3.5h2a2 2 0 0 1 2 2V9M18 7l2 2 2-2" {...ROUND} />
      <Path d="M8 20.5H6a2 2 0 0 1-2-2V15M2 17l2-2 2 2" {...ROUND} />
    </Svg>
  );
}

export function TransferTrackIcon({ color, size = 22 }: IconProps) {
  return (
    <Svg width={size} height={size} viewBox="0 0 24 24" fill="none" stroke={color} strokeWidth={2}>
      <Path d="M12 21H6a2 2 0 0 1-2-2V5a2 2 0 0 1 2-2h8l5 5v2M8 9h4M8 13h3" {...ROUND} />
      <Circle cx={16.5} cy={16} r={3} />
      <Path d="m18.7 18.2 2.8 2.8" {...ROUND} />
    </Svg>
  );
}

export function BalanceIcon({ color, size = 22 }: IconProps) {
  return (
    <Svg width={size} height={size} viewBox="0 0 24 24" fill="none" stroke={color} strokeWidth={2}>
      <Path d="M18 7V4.5A1.5 1.5 0 0 0 16.5 3h-11a2.5 2.5 0 0 0 0 5H19a2 2 0 0 1 2 2v2.5" {...ROUND} />
      <Path d="M3 5.5V18a3 3 0 0 0 3 3h13a2 2 0 0 0 2-2v-2.5M21 12.5h-3.5a2 2 0 0 0 0 4H21Z" {...ROUND} />
    </Svg>
  );
}

export function GlobeIcon({ color, size = 22 }: IconProps) {
  return (
    <Svg width={size} height={size} viewBox="0 0 24 24" fill="none" stroke={color} strokeWidth={2}>
      <Circle cx={12} cy={12} r={9} />
      <Path d="M3 12h18M12 3c2.5 2.6 3.8 5.6 3.8 9s-1.3 6.4-3.8 9c-2.5-2.6-3.8-5.6-3.8-9s1.3-6.4 3.8-9Z" {...ROUND} />
    </Svg>
  );
}

export function MobileIcon({ color, size = 22 }: IconProps) {
  return (
    <Svg width={size} height={size} viewBox="0 0 24 24" fill="none" stroke={color} strokeWidth={2}>
      <Rect x={6} y={2.5} width={12} height={19} rx={2.5} />
      <Path d="M11 18h2" {...ROUND} />
    </Svg>
  );
}

export function TowerIcon({ color, size = 22 }: IconProps) {
  return (
    <Svg width={size} height={size} viewBox="0 0 24 24" fill="none" stroke={color} strokeWidth={2}>
      <Circle cx={12} cy={10} r={1.5} />
      <Path d="M9 21l3-9 3 9M10 18h4M8.5 7a5 5 0 0 1 7 0M6 4.5a8.5 8.5 0 0 1 12 0" {...ROUND} />
    </Svg>
  );
}

export function BoltIcon({ color, size = 22 }: IconProps) {
  return (
    <Svg width={size} height={size} viewBox="0 0 24 24" fill="none" stroke={color} strokeWidth={2}>
      <Path d="M13.5 2 4 13.5h7.5L10 22l10-12h-7.5l1-8Z" {...ROUND} />
    </Svg>
  );
}

export function FlameIcon({ color, size = 22 }: IconProps) {
  return (
    <Svg width={size} height={size} viewBox="0 0 24 24" fill="none" stroke={color} strokeWidth={2}>
      <Path
        d="M12 21.5c-3.6 0-6.5-2.6-6.5-6.2 0-3.9 3.1-6.3 4.3-10.8 2.4 1.7 3.4 3.9 3.4 6.2 1-.5 1.8-1.5 2.1-2.8 1.9 1.9 3.2 4.3 3.2 7.2 0 3.7-2.9 6.4-6.5 6.4Z"
        {...ROUND}
      />
    </Svg>
  );
}

export function DropIcon({ color, size = 22 }: IconProps) {
  return (
    <Svg width={size} height={size} viewBox="0 0 24 24" fill="none" stroke={color} strokeWidth={2}>
      <Path d="M12 3c3.2 4 6 7.2 6 10.8a6 6 0 0 1-12 0C6 10.2 8.8 7 12 3Z" {...ROUND} />
    </Svg>
  );
}

export function PhoneIcon({ color, size = 22 }: IconProps) {
  return (
    <Svg width={size} height={size} viewBox="0 0 24 24" fill="none" stroke={color} strokeWidth={2}>
      <Path
        d="M5 4h3.2l1.6 4-2 1.3a11 11 0 0 0 6.9 6.9l1.3-2 4 1.6V19a2 2 0 0 1-2 2A16 16 0 0 1 3 6a2 2 0 0 1 2-2Z"
        {...ROUND}
      />
    </Svg>
  );
}

export function RoadIcon({ color, size = 22 }: IconProps) {
  return (
    <Svg width={size} height={size} viewBox="0 0 24 24" fill="none" stroke={color} strokeWidth={2}>
      <Path d="M9 3 5 21M15 3l4 18M12 5v2M12 11v2M12 17v2.5" {...ROUND} />
    </Svg>
  );
}

export function TrafficLightIcon({ color, size = 22 }: IconProps) {
  return (
    <Svg width={size} height={size} viewBox="0 0 24 24" fill="none" stroke={color} strokeWidth={2}>
      <Rect x={7.5} y={2} width={9} height={20} rx={3} />
      <Circle cx={12} cy={6.5} r={1.9} fill={color} stroke="none" />
      <Circle cx={12} cy={12} r={1.9} fill={color} stroke="none" />
      <Circle cx={12} cy={17.5} r={1.9} fill={color} stroke="none" />
    </Svg>
  );
}

export function IdCardIcon({ color, size = 22 }: IconProps) {
  return (
    <Svg width={size} height={size} viewBox="0 0 24 24" fill="none" stroke={color} strokeWidth={2}>
      <Rect x={2} y={5} width={20} height={14} rx={2} />
      <Circle cx={8} cy={10.5} r={2} />
      <Path d="M5 16c.6-1.4 1.7-2.2 3-2.2s2.4.8 3 2.2M14 10h5M14 14h3" {...ROUND} />
    </Svg>
  );
}

export function CardSearchIcon({ color, size = 22 }: IconProps) {
  return (
    <Svg width={size} height={size} viewBox="0 0 24 24" fill="none" stroke={color} strokeWidth={2}>
      <Path d="M12 19H5a2 2 0 0 1-2-2V7a2 2 0 0 1 2-2h14a2 2 0 0 1 2 2v4M3 9.5h18M6 14h4" {...ROUND} />
      <Circle cx={17} cy={16.5} r={3} />
      <Path d="m19.2 18.7 2.3 2.3" {...ROUND} />
    </Svg>
  );
}

export function PlateIcon({ color, size = 22 }: IconProps) {
  return (
    <Svg width={size} height={size} viewBox="0 0 24 24" fill="none" stroke={color} strokeWidth={2}>
      <Rect x={2} y={7} width={20} height={10} rx={1.5} />
      <Path d="M6 7v10M17.5 7v10M9 10.5v3M11.75 10.5v3M14.5 10.5v3" {...ROUND} />
    </Svg>
  );
}

export function MotorcycleIcon({ color, size = 22 }: IconProps) {
  return (
    <Svg width={size} height={size} viewBox="0 0 24 24" fill="none" stroke={color} strokeWidth={2}>
      <Circle cx={5.5} cy={16.5} r={3.5} />
      <Circle cx={18.5} cy={16.5} r={3.5} />
      <Path d="M5.5 16.5h5L14 11M8 11h8.5M18.5 16.5 15 6.5h-2.5" {...ROUND} />
    </Svg>
  );
}

export function QuakeIcon({ color, size = 22 }: IconProps) {
  return (
    <Svg width={size} height={size} viewBox="0 0 24 24" fill="none" stroke={color} strokeWidth={2}>
      <Path d="M3 11 12 4l9 7M5 9.5V20h14V9.5M12.5 8.5l-2 3.5h3l-2 4 1 4" {...ROUND} />
    </Svg>
  );
}

export function FirstAidIcon({ color, size = 22 }: IconProps) {
  return (
    <Svg width={size} height={size} viewBox="0 0 24 24" fill="none" stroke={color} strokeWidth={2}>
      <Rect x={3} y={7} width={18} height={13} rx={2} />
      <Path d="M9 7V5.5A1.5 1.5 0 0 1 10.5 4h3A1.5 1.5 0 0 1 15 5.5V7M12 10.5v6M9 13.5h6" {...ROUND} />
    </Svg>
  );
}

export function BuildingIcon({ color, size = 22 }: IconProps) {
  return (
    <Svg width={size} height={size} viewBox="0 0 24 24" fill="none" stroke={color} strokeWidth={2}>
      <Rect x={5} y={3} width={14} height={18} rx={1.5} />
      <Path d="M9 7h1.5M13.5 7H15M9 11h1.5M13.5 11H15M9 15h1.5M13.5 15H15M10.5 21v-2.5h3V21" {...ROUND} />
    </Svg>
  );
}

export function HistoryIcon({ color, size = 22 }: IconProps) {
  return (
    <Svg width={size} height={size} viewBox="0 0 24 24" fill="none" stroke={color} strokeWidth={2}>
      <Path d="M3.5 12A8.5 8.5 0 1 0 6 6L3.5 8.5M3.5 4v4.5H8M12 7.5V12l3 2" {...ROUND} />
    </Svg>
  );
}

export function LuggageIcon({ color, size = 22 }: IconProps) {
  return (
    <Svg width={size} height={size} viewBox="0 0 24 24" fill="none" stroke={color} strokeWidth={2}>
      <Rect x={6} y={6} width={12} height={13} rx={2} />
      <Path d="M9.5 6V4a1 1 0 0 1 1-1h3a1 1 0 0 1 1 1v2M10 10v5M14 10v5M9 19v2M15 19v2" {...ROUND} />
    </Svg>
  );
}

export function BedIcon({ color, size = 22 }: IconProps) {
  return (
    <Svg width={size} height={size} viewBox="0 0 24 24" fill="none" stroke={color} strokeWidth={2}>
      <Path d="M3 5v14M3 12h16a2 2 0 0 1 2 2v5M3 16h18" {...ROUND} />
      <Rect x={5.5} y={8} width={5} height={4} rx={1.2} />
    </Svg>
  );
}

export function PlaneIcon({ color, size = 22 }: IconProps) {
  return (
    <Svg width={size} height={size} viewBox="0 0 24 24" fill="none" stroke={color} strokeWidth={2}>
      <Path
        d="M12 2.5c1 0 1.5 1.2 1.5 2.5v4.5l7.5 4.5v2l-7.5-2.5v4.5l2.5 2v1.5l-4-1.2-4 1.2V20l2.5-2v-4.5L3 16v-2l7.5-4.5V5c0-1.3.5-2.5 1.5-2.5Z"
        {...ROUND}
      />
    </Svg>
  );
}

export function AcademyIcon({ color, size = 22 }: IconProps) {
  return (
    <Svg width={size} height={size} viewBox="0 0 24 24" fill="none" stroke={color} strokeWidth={2}>
      <Path d="M2.5 9.5 12 5l9.5 4.5L12 14 2.5 9.5Z" {...ROUND} />
      <Path d="M6.5 11.5V16c0 0 2.2 2.5 5.5 2.5s5.5-2.5 5.5-2.5v-4.5M21.5 9.5V15" {...ROUND} />
    </Svg>
  );
}

export function MusicIcon({ color, size = 22 }: IconProps) {
  return (
    <Svg width={size} height={size} viewBox="0 0 24 24" fill="none" stroke={color} strokeWidth={2}>
      <Path d="M9 18V5.5l11-2V16" {...ROUND} />
      <Circle cx={6.5} cy={18} r={2.5} />
      <Circle cx={17.5} cy={16} r={2.5} />
    </Svg>
  );
}
