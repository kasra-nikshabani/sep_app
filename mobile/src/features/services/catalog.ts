import {
  TvIcon,
  BallIcon,
  NewsIcon,
  ShopIcon,
  CardTransferIcon,
  TransferTrackIcon,
  BalanceIcon,
  SimCardIcon,
  GlobeIcon,
  MobileIcon,
  TowerIcon,
  BoltIcon,
  FlameIcon,
  DropIcon,
  PhoneIcon,
  RoadIcon,
  CarIcon,
  TrafficLightIcon,
  IdCardIcon,
  CardSearchIcon,
  PlateIcon,
  ShieldIcon,
  MotorcycleIcon,
  QuakeIcon,
  FirstAidIcon,
  BuildingIcon,
  AcademyIcon,
  LuggageIcon,
  BedIcon,
  PlaneIcon,
  type IconComponent,
} from "@/components/Icon";

// external: href یک آدرس وب است و در مرورگر داخل اپ باز می‌شود، نه یک مسیر خود اپ.
export type ServiceItem = { label: string; Icon: IconComponent; href: string; soon?: boolean; external?: boolean };
export type ServiceSection = { title: string; items: ServiceItem[] };

// سایت بلیط فوتبال (ticket.sepahansc.com، سیستم جدای Django). آدرس از env می‌آید تا بعد از
// روشن شدن «ورود با Fan ID» روی سایت، بدون تغییر کد به همان ورود مستقیم اشاره کند.
const TICKET_SITE_URL = process.env.EXPO_PUBLIC_TICKET_SITE_URL as string;

// پخش زنده‌ی سپاهان TV. سایت باشگاه خودش به همین آدرس لینک می‌دهد (livekadeh)؛
// تیم پخش مسابقه را آنجا می‌گذارد. آدرس از env می‌آید تا بدون تغییر کد عوض شود.
const SEPAHAN_TV_URL = process.env.EXPO_PUBLIC_SEPAHAN_TV_URL ?? "https://livekadeh.com/sepahantv/";

// مدارس ورزشی سپاهان (سامانه‌ی جدا: academy.sepahansc.com).
const ACADEMY_URL = process.env.EXPO_PUBLIC_ACADEMY_URL ?? "https://academy.sepahansc.com/";

// عنوان و آیکن صفحه‌ی soon/[key] هم از همین فهرست خوانده می‌شود (href = /soon/{key}).
export const SERVICE_SECTIONS: ServiceSection[] = [
  {
    title: "باشگاه هواداری",
    items: [
      { label: "سپاهان TV", Icon: TvIcon, href: SEPAHAN_TV_URL, external: true },
      { label: "بلیط مسابقات", Icon: BallIcon, href: TICKET_SITE_URL, external: true },
      { label: "اخبار باشگاه", Icon: NewsIcon, href: "/news" },
      { label: "فروشگاه", Icon: ShopIcon, href: "/shop" },
      { label: "آکادمی", Icon: AcademyIcon, href: ACADEMY_URL, external: true },
    ],
  },
  {
    title: "خدمات بانکی",
    items: [
      { label: "کارت به کارت", Icon: CardTransferIcon, href: "/soon/banking-card", soon: true },
      { label: "پیگیری انتقال پول", Icon: TransferTrackIcon, href: "/soon/banking-transfer-tracking", soon: true },
      { label: "موجودی کارت", Icon: BalanceIcon, href: "/soon/banking-card-balance", soon: true },
    ],
  },
  {
    title: "موبایل، شارژ و اینترنت",
    items: [
      { label: "خرید شارژ", Icon: SimCardIcon, href: "/soon/topup-credit", soon: true },
      { label: "بسته اینترنت", Icon: GlobeIcon, href: "/soon/topup-internet", soon: true },
    ],
  },
  {
    title: "قبوض",
    items: [
      { label: "قبض ایرانسل", Icon: MobileIcon, href: "/soon/bill-irancell", soon: true },
      { label: "قبض همراه اول", Icon: MobileIcon, href: "/soon/bill-mci", soon: true },
      { label: "قبض مخابرات", Icon: TowerIcon, href: "/soon/bill-telecom", soon: true },
      { label: "قبض برق", Icon: BoltIcon, href: "/soon/bill-electricity", soon: true },
      { label: "قبض گاز", Icon: FlameIcon, href: "/soon/bill-gas", soon: true },
      { label: "قبض آب", Icon: DropIcon, href: "/soon/bill-water", soon: true },
      { label: "قبض تلفن ثابت", Icon: PhoneIcon, href: "/soon/bill-landline", soon: true },
    ],
  },
  {
    title: "خودرو",
    items: [
      { label: "عوارض آزادراهی", Icon: RoadIcon, href: "/soon/vehicle-toll", soon: true },
      { label: "خلافی", Icon: CarIcon, href: "/soon/vehicle-violations", soon: true },
      { label: "طرح ترافیک", Icon: TrafficLightIcon, href: "/soon/vehicle-traffic-plan", soon: true },
      { label: "گواهی‌نامه", Icon: IdCardIcon, href: "/soon/vehicle-license", soon: true },
      { label: "استعلام کارت خودرو", Icon: CardSearchIcon, href: "/soon/vehicle-card-inquiry", soon: true },
      { label: "استعلام پلاک", Icon: PlateIcon, href: "/soon/vehicle-plate-inquiry", soon: true },
    ],
  },
  {
    title: "بیمه",
    items: [
      { label: "بیمه بدنه", Icon: CarIcon, href: "/soon/insurance-comprehensive", soon: true },
      { label: "بیمه شخص ثالث", Icon: ShieldIcon, href: "/soon/insurance-third-party", soon: true },
      { label: "بیمه موتورسیکلت", Icon: MotorcycleIcon, href: "/soon/insurance-motorcycle", soon: true },
      { label: "بیمه آتش‌سوزی", Icon: FlameIcon, href: "/soon/insurance-fire", soon: true },
      { label: "بیمه زلزله", Icon: QuakeIcon, href: "/soon/insurance-earthquake", soon: true },
      { label: "بیمه حوادث انفرادی", Icon: FirstAidIcon, href: "/soon/insurance-accident", soon: true },
      { label: "بیمه آتش‌سوزی ساختمان", Icon: BuildingIcon, href: "/soon/insurance-building-fire", soon: true },
      { label: "بیمه موبایل", Icon: MobileIcon, href: "/soon/insurance-mobile", soon: true },
    ],
  },
  {
    title: "گردشگری",
    items: [
      { label: "تور", Icon: LuggageIcon, href: "/soon/tour", soon: true },
      { label: "هتل", Icon: BedIcon, href: "/soon/hotel", soon: true },
      { label: "بلیط هواپیما", Icon: PlaneIcon, href: "/soon/flight", soon: true },
    ],
  },
];
