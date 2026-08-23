import { backendFetch } from "@/lib/backend";
import { OrdersTable, type Order } from "./OrdersTable";
import { ReturnReviewCard } from "./ReturnReviewCard";

export default async function OrdersPage() {
  const orders = await backendFetch<Order[]>("/api/v1/shop/admin/orders");
  return (
    <>
      <h2 style={{ marginBottom: 16 }}>سفارش‌ها</h2>
      <OrdersTable data={orders} />
      <h3 style={{ margin: "24px 0 12px" }}>بررسی درخواست مرجوعی</h3>
      <ReturnReviewCard />
    </>
  );
}
