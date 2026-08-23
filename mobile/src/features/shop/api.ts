import { useMutation, useQuery, useQueryClient } from "@tanstack/react-query";
import { useApi } from "@/lib/api";

export type Category = { id: string; name: string; slug: string; parentId: string | null };
export type ProductVariant = {
  id: string;
  sku: string;
  attributes: Record<string, string>;
  price: string;
  stockQuantity: number;
  weightGrams: number;
  active: boolean;
};
export type Product = {
  id: string;
  categoryId: string;
  categoryName: string;
  name: string;
  slug: string;
  description: string | null;
  imageUrl: string | null;
  basePrice: string;
  active: boolean;
  variants: ProductVariant[];
};
export type ShippingMethod = { id: string; name: string; baseRate: string; perKgRate: string };
export type CartItem = {
  id: string;
  productVariantId: string;
  productName: string;
  sku: string;
  unitPrice: string;
  quantity: number;
  lineSubtotal: string;
};
export type OrderItem = { productName: string; sku: string; unitPrice: string; quantity: number; subtotal: string };
export type Order = {
  id: string;
  status: string;
  subtotalAmount: string;
  discountAmount: string;
  shippingAmount: string;
  totalAmount: string;
  shippingMethodName: string;
  shippingRecipientName: string;
  shippingCity: string;
  expiresAt: string | null;
  requiresManualReview: boolean;
  createdAt: string;
  items: OrderItem[];
};

export function useCategories() {
  const { apiFetch } = useApi();
  return useQuery({ queryKey: ["shop", "categories"], queryFn: () => apiFetch<Category[]>("/api/v1/shop/categories") });
}

export function useProducts(categoryId?: string) {
  const { apiFetch } = useApi();
  return useQuery({
    queryKey: ["shop", "products", categoryId ?? "all"],
    queryFn: () => apiFetch<Product[]>(`/api/v1/shop/products${categoryId ? `?categoryId=${categoryId}` : ""}`),
  });
}

export function useProduct(productId: string) {
  const { apiFetch } = useApi();
  return useQuery({
    queryKey: ["shop", "product", productId],
    queryFn: () => apiFetch<Product>(`/api/v1/shop/products/${productId}`),
    enabled: !!productId,
  });
}

export function useShippingMethods() {
  const { apiFetch } = useApi();
  return useQuery({
    queryKey: ["shop", "shipping-methods"],
    queryFn: () => apiFetch<ShippingMethod[]>("/api/v1/shop/shipping-methods"),
  });
}

export function useCart() {
  const { apiFetch } = useApi();
  return useQuery({ queryKey: ["shop", "cart"], queryFn: () => apiFetch<CartItem[]>("/api/v1/shop/cart") });
}

export function useAddToCart() {
  const { apiFetch } = useApi();
  const qc = useQueryClient();
  return useMutation({
    mutationFn: (body: { productVariantId: string; quantity: number }) =>
      apiFetch<CartItem>("/api/v1/shop/cart/items", { method: "POST", body: JSON.stringify(body) }),
    onSuccess: () => qc.invalidateQueries({ queryKey: ["shop", "cart"] }),
  });
}

export function useUpdateCartItem() {
  const { apiFetch } = useApi();
  const qc = useQueryClient();
  return useMutation({
    mutationFn: ({ itemId, quantity }: { itemId: string; quantity: number }) =>
      apiFetch<void>(`/api/v1/shop/cart/items/${itemId}`, { method: "PUT", body: JSON.stringify({ quantity }) }),
    onSuccess: () => qc.invalidateQueries({ queryKey: ["shop", "cart"] }),
  });
}

export function useRemoveCartItem() {
  const { apiFetch } = useApi();
  const qc = useQueryClient();
  return useMutation({
    mutationFn: (itemId: string) => apiFetch<void>(`/api/v1/shop/cart/items/${itemId}`, { method: "DELETE" }),
    onSuccess: () => qc.invalidateQueries({ queryKey: ["shop", "cart"] }),
  });
}

export function useCheckout() {
  const { apiFetch } = useApi();
  const qc = useQueryClient();
  return useMutation({
    mutationFn: (body: {
      shippingMethodId: string;
      couponCode?: string;
      recipientName: string;
      phone: string;
      province: string;
      city: string;
      addressLine: string;
      postalCode?: string;
    }) => apiFetch<Order>("/api/v1/shop/checkout", { method: "POST", body: JSON.stringify(body) }),
    onSuccess: () => {
      qc.invalidateQueries({ queryKey: ["shop", "cart"] });
      qc.invalidateQueries({ queryKey: ["shop", "orders"] });
    },
  });
}

export function usePayOrder() {
  const { apiFetch } = useApi();
  return useMutation({
    mutationFn: (orderId: string) =>
      apiFetch<{ paymentId: string; redirectUrl: string }>(`/api/v1/shop/orders/${orderId}/pay`, { method: "POST" }),
  });
}

export function useMyOrders() {
  const { apiFetch } = useApi();
  return useQuery({ queryKey: ["shop", "orders"], queryFn: () => apiFetch<Order[]>("/api/v1/shop/orders") });
}
