import { backendFetch } from "@/lib/backend";
import { PartnersTable, type Partner } from "./PartnersTable";

export default async function PartnersPage() {
  const partners = await backendFetch<Partner[]>("/api/v1/partners/admin");
  return (
    <>
      <h2 style={{ marginBottom: 16 }}>پارتنرها</h2>
      <PartnersTable data={partners} />
    </>
  );
}
