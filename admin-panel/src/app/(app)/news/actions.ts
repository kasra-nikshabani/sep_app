"use server";

import { revalidatePath } from "next/cache";
import { backendFetch, backendUpload, BackendError } from "@/lib/backend";

export type ActionState = { error?: string; success?: boolean };
const ok: ActionState = { success: true };

function fail(e: unknown): ActionState {
  return { error: e instanceof BackendError ? e.message : "خطای غیرمنتظره" };
}

function tagIdsFrom(formData: FormData): string[] {
  return formData.getAll("tagIds").map(String).filter(Boolean);
}

export async function createCategoryAction(_prev: ActionState, formData: FormData): Promise<ActionState> {
  try {
    await backendFetch("/api/v1/news/admin/categories", {
      method: "POST",
      body: JSON.stringify({ name: String(formData.get("name") ?? ""), slug: String(formData.get("slug") ?? "") }),
    });
    revalidatePath("/news");
    return ok;
  } catch (e) {
    return fail(e);
  }
}

export async function createTagAction(_prev: ActionState, formData: FormData): Promise<ActionState> {
  try {
    await backendFetch("/api/v1/news/admin/tags", {
      method: "POST",
      body: JSON.stringify({ name: String(formData.get("name") ?? ""), slug: String(formData.get("slug") ?? "") }),
    });
    revalidatePath("/news");
    return ok;
  } catch (e) {
    return fail(e);
  }
}

export type UploadState = { error?: string; url?: string };

export async function uploadMediaAction(_prev: UploadState, formData: FormData): Promise<UploadState> {
  try {
    const file = formData.get("file");
    if (!(file instanceof File) || file.size === 0) {
      return { error: "فایلی انتخاب نشده است" };
    }
    const upload = new FormData();
    upload.set("file", file);
    const asset = await backendUpload<{ url: string }>("/api/v1/news/admin/media", upload);
    return { url: asset.url };
  } catch (e) {
    return { error: e instanceof BackendError ? e.message : "خطای غیرمنتظره" };
  }
}

function articleBody(formData: FormData) {
  return {
    categoryId: String(formData.get("categoryId") ?? ""),
    title: String(formData.get("title") ?? ""),
    slug: String(formData.get("slug") ?? ""),
    content: String(formData.get("content") ?? ""),
    summary: String(formData.get("summary") ?? "") || null,
    coverImageUrl: String(formData.get("coverImageUrl") ?? "") || null,
    metaTitle: String(formData.get("metaTitle") ?? "") || null,
    metaDescription: String(formData.get("metaDescription") ?? "") || null,
    tagIds: tagIdsFrom(formData),
  };
}

export async function createArticleAction(_prev: ActionState, formData: FormData): Promise<ActionState> {
  try {
    await backendFetch("/api/v1/news/admin/articles", { method: "POST", body: JSON.stringify(articleBody(formData)) });
    revalidatePath("/news");
    return ok;
  } catch (e) {
    return fail(e);
  }
}

export async function updateArticleAction(articleId: string, _prev: ActionState, formData: FormData): Promise<ActionState> {
  try {
    const { slug: _slug, ...body } = articleBody(formData);
    void _slug;
    await backendFetch(`/api/v1/news/admin/articles/${articleId}`, { method: "PUT", body: JSON.stringify(body) });
    revalidatePath("/news");
    return ok;
  } catch (e) {
    return fail(e);
  }
}

export async function publishArticleAction(articleId: string): Promise<ActionState> {
  try {
    await backendFetch(`/api/v1/news/admin/articles/${articleId}/publish`, { method: "POST" });
    revalidatePath("/news");
    return ok;
  } catch (e) {
    return fail(e);
  }
}

export async function archiveArticleAction(articleId: string): Promise<ActionState> {
  try {
    await backendFetch(`/api/v1/news/admin/articles/${articleId}/archive`, { method: "POST" });
    revalidatePath("/news");
    return ok;
  } catch (e) {
    return fail(e);
  }
}

export async function deleteArticleAction(articleId: string): Promise<ActionState> {
  try {
    await backendFetch(`/api/v1/news/admin/articles/${articleId}`, { method: "DELETE" });
    revalidatePath("/news");
    return ok;
  } catch (e) {
    return fail(e);
  }
}

export async function getArticleDetailAction(articleId: string) {
  return backendFetch(`/api/v1/news/admin/articles/${articleId}`);
}

export async function getArticleRevisionsAction(articleId: string) {
  return backendFetch(`/api/v1/news/admin/articles/${articleId}/revisions`);
}

export async function scheduleArticleAction(articleId: string, scheduledAtIso: string): Promise<ActionState> {
  try {
    await backendFetch(`/api/v1/news/admin/articles/${articleId}/schedule`, {
      method: "POST",
      body: JSON.stringify({ scheduledAt: scheduledAtIso }),
    });
    revalidatePath("/news");
    return ok;
  } catch (e) {
    return fail(e);
  }
}
