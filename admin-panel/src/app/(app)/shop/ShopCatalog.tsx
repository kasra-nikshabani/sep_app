"use client";

import { useActionState, useState, useTransition } from "react";
import { Tabs, Table, Card, Form, Input, InputNumber, Select, Button, Alert, Space, Tag, Collapse } from "antd";
import { formatRial } from "@/lib/format";
import {
  createCategoryAction,
  createProductAction,
  addVariantAction,
  adjustStockAction,
  createShippingMethodAction,
  createCouponAction,
  type ActionState,
} from "./actions";

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
export type Coupon = { id: string; code: string; discountType: string; discountValue: string; active: boolean };

const initial: ActionState = {};

function CategoriesTab({ data }: { data: Category[] }) {
  const [state, formAction, pending] = useActionState(createCategoryAction, initial);
  const [parentId, setParentId] = useState<string>();
  return (
    <Space orientation="vertical" style={{ width: "100%" }} size="large">
      <Table
        rowKey="id"
        dataSource={data}
        columns={[
          { title: "نام", dataIndex: "name" },
          { title: "Slug", dataIndex: "slug" },
          {
            title: "دسته‌ی والد",
            dataIndex: "parentId",
            render: (v: string | null) => data.find((c) => c.id === v)?.name ?? "—",
          },
        ]}
        pagination={false}
      />
      <Card size="small" title="دسته‌بندی جدید">
        <form action={formAction} key={state.success ? "1" : "0"} style={{ display: "flex", gap: 8, flexWrap: "wrap", alignItems: "flex-end" }}>
          {state.error && <Alert type="error" title={state.error} style={{ width: "100%", marginBottom: 12 }} />}
          <input type="hidden" name="parentId" value={parentId ?? ""} />
          <Form.Item>
            <Input name="name" placeholder="نام" required />
          </Form.Item>
          <Form.Item>
            <Input name="slug" placeholder="slug" dir="ltr" required />
          </Form.Item>
          <Form.Item>
            <Select
              placeholder="دسته‌ی والد (اختیاری)"
              allowClear
              style={{ width: 200 }}
              value={parentId}
              onChange={setParentId}
              options={data.map((c) => ({ value: c.id, label: c.name }))}
            />
          </Form.Item>
          <Button type="primary" htmlType="submit" loading={pending}>
            افزودن
          </Button>
        </form>
      </Card>
    </Space>
  );
}

function VariantForm({ productId }: { productId: string }) {
  const [state, formAction, pending] = useActionState(addVariantAction.bind(null, productId), initial);
  return (
    <form action={formAction} key={state.success ? "1" : "0"} style={{ display: "flex", gap: 8, flexWrap: "wrap", alignItems: "flex-end" }}>
      {state.error && <Alert type="error" title={state.error} style={{ width: "100%", marginBottom: 12 }} />}
      <Form.Item>
        <Input name="sku" placeholder="SKU" dir="ltr" required />
      </Form.Item>
      <Form.Item>
        <Input name="attributes" placeholder="سایز:XL,رنگ:قرمز" style={{ width: 200 }} />
      </Form.Item>
      <Form.Item>
        <InputNumber name="initialStock" placeholder="موجودی اولیه" min={0} required />
      </Form.Item>
      <Form.Item>
        <InputNumber name="weightGrams" placeholder="وزن (گرم)" min={0} defaultValue={500} />
      </Form.Item>
      <Form.Item>
        <InputNumber name="priceOverride" placeholder="قیمت جایگزین (اختیاری)" min={0} />
      </Form.Item>
      <Button htmlType="submit" loading={pending}>
        افزودن Variant
      </Button>
    </form>
  );
}

function VariantsTable({ variants }: { variants: ProductVariant[] }) {
  const [isPending, startTransition] = useTransition();
  return (
    <Table
      size="small"
      rowKey="id"
      dataSource={variants}
      pagination={false}
      columns={[
        { title: "SKU", dataIndex: "sku" },
        { title: "ویژگی‌ها", dataIndex: "attributes", render: (v: Record<string, string>) => Object.entries(v ?? {}).map(([k, val]) => `${k}:${val}`).join("، ") || "—" },
        { title: "قیمت", dataIndex: "price", render: (v) => formatRial(v) },
        { title: "موجودی", dataIndex: "stockQuantity" },
        {
          title: "عملیات",
          render: (_, record: ProductVariant) => (
            <Space>
              <Button size="small" loading={isPending} onClick={() => startTransition(() => { adjustStockAction(record.id, 1); })}>
                +۱
              </Button>
              <Button size="small" loading={isPending} onClick={() => startTransition(() => { adjustStockAction(record.id, -1); })}>
                −۱
              </Button>
            </Space>
          ),
        },
      ]}
    />
  );
}

function ProductsTab({ categories, products }: { categories: Category[]; products: Product[] }) {
  const [state, formAction, pending] = useActionState(createProductAction, initial);
  const [categoryId, setCategoryId] = useState<string>();
  return (
    <Space orientation="vertical" style={{ width: "100%" }} size="large">
      <Collapse
        items={products.map((p) => ({
          key: p.id,
          label: (
            <Space>
              <strong>{p.name}</strong>
              <Tag>{p.categoryName}</Tag>
              {formatRial(p.basePrice)}
            </Space>
          ),
          children: (
            <>
              <VariantsTable variants={p.variants} />
              <div style={{ marginTop: 12 }}>
                <VariantForm productId={p.id} />
              </div>
            </>
          ),
        }))}
      />
      <Card size="small" title="محصول جدید">
        <form action={formAction} key={state.success ? "1" : "0"} style={{ display: "flex", gap: 8, flexWrap: "wrap", alignItems: "flex-end" }}>
          {state.error && <Alert type="error" title={state.error} style={{ width: "100%", marginBottom: 12 }} />}
          <input type="hidden" name="categoryId" value={categoryId ?? ""} />
          <Form.Item>
            <Select
              placeholder="دسته‌بندی"
              style={{ width: 180 }}
              value={categoryId}
              onChange={setCategoryId}
              options={categories.map((c) => ({ value: c.id, label: c.name }))}
            />
          </Form.Item>
          <Form.Item>
            <Input name="name" placeholder="نام محصول" required />
          </Form.Item>
          <Form.Item>
            <Input name="slug" placeholder="slug" dir="ltr" required />
          </Form.Item>
          <Form.Item>
            <InputNumber name="basePrice" placeholder="قیمت پایه (ریال)" min={0} required />
          </Form.Item>
          <Form.Item>
            <Input name="imageUrl" placeholder="آدرس تصویر (اختیاری)" dir="ltr" />
          </Form.Item>
          <Button type="primary" htmlType="submit" loading={pending}>
            افزودن
          </Button>
        </form>
      </Card>
    </Space>
  );
}

function ShippingTab({ data }: { data: ShippingMethod[] }) {
  const [state, formAction, pending] = useActionState(createShippingMethodAction, initial);
  return (
    <Space orientation="vertical" style={{ width: "100%" }} size="large">
      <Table
        rowKey="id"
        dataSource={data}
        columns={[
          { title: "نام", dataIndex: "name" },
          { title: "نرخ پایه", dataIndex: "baseRate", render: (v) => formatRial(v) },
          { title: "نرخ به ازای هر کیلو", dataIndex: "perKgRate", render: (v) => formatRial(v) },
        ]}
        pagination={false}
      />
      <Card size="small" title="روش ارسال جدید">
        <form action={formAction} key={state.success ? "1" : "0"} style={{ display: "flex", gap: 8, flexWrap: "wrap", alignItems: "flex-end" }}>
          {state.error && <Alert type="error" title={state.error} style={{ width: "100%", marginBottom: 12 }} />}
          <Form.Item>
            <Input name="name" placeholder="نام" required />
          </Form.Item>
          <Form.Item>
            <InputNumber name="baseRate" placeholder="نرخ پایه" min={0} required />
          </Form.Item>
          <Form.Item>
            <InputNumber name="perKgRate" placeholder="نرخ/کیلو" min={0} />
          </Form.Item>
          <Button type="primary" htmlType="submit" loading={pending}>
            افزودن
          </Button>
        </form>
      </Card>
    </Space>
  );
}

function CouponsTab() {
  const [state, formAction, pending] = useActionState(createCouponAction, initial);
  const [discountType, setDiscountType] = useState<string>();
  return (
    <Space orientation="vertical" style={{ width: "100%" }} size="large">
      <Alert type="info" showIcon title="بدون Endpoint فهرست کدهای تخفیف -- فقط ساخت از این‌جا ممکن است" />
      <Card size="small" title="کد تخفیف جدید">
        <form action={formAction} key={state.success ? "1" : "0"} style={{ display: "flex", gap: 8, flexWrap: "wrap", alignItems: "flex-end" }}>
          {state.error && <Alert type="error" title={state.error} style={{ width: "100%", marginBottom: 12 }} />}
          {state.success && <Alert type="success" title="ساخته شد" style={{ width: "100%", marginBottom: 12 }} />}
          <input type="hidden" name="discountType" value={discountType ?? ""} />
          <Form.Item>
            <Input name="code" placeholder="کد (مثلاً SEPAHAN10)" dir="ltr" required />
          </Form.Item>
          <Form.Item>
            <Select
              placeholder="نوع تخفیف"
              style={{ width: 160 }}
              value={discountType}
              onChange={setDiscountType}
              options={[
                { value: "percentage", label: "درصدی" },
                { value: "fixed_amount", label: "مبلغ ثابت" },
              ]}
            />
          </Form.Item>
          <Form.Item>
            <InputNumber name="discountValue" placeholder="مقدار" min={0} required />
          </Form.Item>
          <Form.Item>
            <InputNumber name="minOrderAmount" placeholder="حداقل مبلغ سفارش" min={0} />
          </Form.Item>
          <Form.Item>
            <InputNumber name="maxUsesTotal" placeholder="سقف کل استفاده" min={1} />
          </Form.Item>
          <Form.Item>
            <InputNumber name="maxUsesPerUser" placeholder="سقف هر کاربر" min={1} />
          </Form.Item>
          <Button type="primary" htmlType="submit" loading={pending}>
            افزودن
          </Button>
        </form>
      </Card>
    </Space>
  );
}

export function ShopCatalog({
  categories,
  products,
  shippingMethods,
}: {
  categories: Category[];
  products: Product[];
  shippingMethods: ShippingMethod[];
}) {
  return (
    <Tabs
      items={[
        { key: "products", label: "محصولات", children: <ProductsTab categories={categories} products={products} /> },
        { key: "categories", label: "دسته‌بندی‌ها", children: <CategoriesTab data={categories} /> },
        { key: "shipping", label: "روش‌های ارسال", children: <ShippingTab data={shippingMethods} /> },
        { key: "coupons", label: "کد تخفیف", children: <CouponsTab /> },
      ]}
    />
  );
}
