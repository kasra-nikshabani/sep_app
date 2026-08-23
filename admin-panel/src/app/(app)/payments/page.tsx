import { backendFetch } from "@/lib/backend";
import { PaymentsTable, type AdminPayment } from "./PaymentsTable";

export default async function PaymentsPage() {
  const payments = await backendFetch<AdminPayment[]>("/api/v1/payments/admin");
  return (
    <>
      <h2 style={{ marginBottom: 16 }}>پرداخت‌ها</h2>
      <PaymentsTable data={payments} />
    </>
  );
}
