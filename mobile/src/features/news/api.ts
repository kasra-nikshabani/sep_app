import { useQuery } from "@tanstack/react-query";
import { useApi } from "@/lib/api";

export type Article = {
  id: string;
  title: string;
  slug: string;
  summary: string | null;
  coverImageUrl: string | null;
  categoryName: string;
  status: string;
  publishedAt: string | null;
};

export type ArticleDetail = Article & {
  content: string;
  metaTitle: string | null;
  metaDescription: string | null;
  authorDisplayName: string | null;
  tags: { id: string; name: string; slug: string }[];
};

export function useArticles() {
  const { apiFetch } = useApi();
  return useQuery({
    queryKey: ["news", "articles"],
    queryFn: () => apiFetch<Article[]>("/api/v1/news/articles"),
  });
}

export function useArticle(slug: string) {
  const { apiFetch } = useApi();
  return useQuery({
    queryKey: ["news", "article", slug],
    queryFn: () => apiFetch<ArticleDetail>(`/api/v1/news/articles/${slug}`),
    enabled: !!slug,
  });
}
