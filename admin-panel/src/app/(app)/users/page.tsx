import { backendFetch } from "@/lib/backend";
import { UsersTable, type AdminUser } from "./UsersTable";

export default async function UsersPage() {
  const users = await backendFetch<AdminUser[]>("/api/v1/users/admin");
  return (
    <>
      <h2 style={{ marginBottom: 16 }}>کاربران و هواداران</h2>
      <UsersTable data={users} />
    </>
  );
}
