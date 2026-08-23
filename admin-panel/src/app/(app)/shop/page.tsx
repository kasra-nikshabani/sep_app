import { backendFetch } from "@/lib/backend";
import { ShopCatalog, type Category, type Product, type ShippingMethod } from "./ShopCatalog";

export default async function ShopPage() {
  const [categories, products, shippingMethods] = await Promise.all([
    backendFetch<Category[]>("/api/v1/shop/categories"),
    backendFetch<Product[]>("/api/v1/shop/products"),
    backendFetch<ShippingMethod[]>("/api/v1/shop/shipping-methods"),
  ]);

  return (
    <>
      <h2 style={{ marginBottom: 16 }}>فروشگاه</h2>
      <ShopCatalog categories={categories} products={products} shippingMethods={shippingMethods} />
    </>
  );
}
