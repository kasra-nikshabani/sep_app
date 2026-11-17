import { useInfiniteQuery, useQuery } from "@tanstack/react-query";
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

export const NEWS_PAGE_SIZE = 20;

// اخبار هزاران مورد است (ورود خودکار از سایت باشگاه)؛ صفحه‌به‌صفحه می‌خوانیم.
export function useArticles() {
  const { apiFetch } = useApi();
  return useInfiniteQuery({
    queryKey: ["news", "articles"],
    initialPageParam: 0,
    queryFn: ({ pageParam }) =>
      apiFetch<Article[]>(`/api/v1/news/articles?page=${pageParam}&size=${NEWS_PAGE_SIZE}`),
    getNextPageParam: (lastPage, pages) => (lastPage.length < NEWS_PAGE_SIZE ? undefined : pages.length),
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
