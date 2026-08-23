import { backendFetch } from "@/lib/backend";
import { NewsAdmin, type Article, type Category, type NewsTag } from "./NewsAdmin";

export default async function NewsPage() {
  const [articles, categories, tags] = await Promise.all([
    backendFetch<Article[]>("/api/v1/news/admin/articles"),
    backendFetch<Category[]>("/api/v1/news/categories"),
    backendFetch<NewsTag[]>("/api/v1/news/tags"),
  ]);

  return (
    <>
      <h2 style={{ marginBottom: 16 }}>اخبار</h2>
      <NewsAdmin articles={articles} categories={categories} tags={tags} />
    </>
  );
}
