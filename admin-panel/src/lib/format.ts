const dateTimeFormatter = new Intl.DateTimeFormat("fa-IR-u-ca-persian", {
  dateStyle: "medium",
  timeStyle: "short",
  timeZone: "Asia/Tehran",
});

export function formatDateTime(value: string | null | undefined): string {
  if (!value) return "—";
  return dateTimeFormatter.format(new Date(value));
}

export function formatNumber(value: number | null | undefined): string {
  if (value === null || value === undefined) return "—";
  return value.toLocaleString("fa-IR");
}

export function formatRial(value: number | string | null | undefined): string {
  if (value === null || value === undefined) return "—";
  return `${Number(value).toLocaleString("fa-IR")} ریال`;
}
