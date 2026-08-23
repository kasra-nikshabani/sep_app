import NextAuth from "next-auth";
import Keycloak from "next-auth/providers/keycloak";

function decodeJwtPayload(token: string): Record<string, unknown> {
  const payload = token.split(".")[1];
  return JSON.parse(Buffer.from(payload, "base64url").toString("utf-8"));
}

function extractRoles(accessToken: string): string[] {
  const payload = decodeJwtPayload(accessToken);
  const realmAccess = payload.realm_access as { roles?: string[] } | undefined;
  return realmAccess?.roles ?? [];
}

type RefreshResult = { access_token: string; refresh_token?: string; expires_in: number };

/**
 * proxy.ts و رندر RSC (AppLayout/Page) هر کدام auth() را جدا -- در دو فاز کاملاً
 * مجزای Next.js، نه یک Render واحد -- صدا می‌زنند، پس React.cache() (که فقط در
 * یک Render واحد Dedupe می‌کند) کافی نبود (کشف در Phase 14). چون Keycloak با
 * revokeRefreshToken=true هر Refresh Token را فقط یک‌بار می‌پذیرد، این Map
 * سطح‌ماژول (در کل Node.js Process مشترک) تضمین می‌کند هر Refresh Token واقعی
 * فقط یک‌بار واقعاً به Keycloak فرستاده شود؛ صداهای هم‌زمان دیگر همان Promise را می‌گیرند.
 */
const inFlightRefreshes = new Map<string, Promise<RefreshResult>>();

function refreshAccessToken(refreshToken: string): Promise<RefreshResult> {
  const existing = inFlightRefreshes.get(refreshToken);
  if (existing) {
    return existing;
  }

  const promise = (async () => {
    const response = await fetch(`${process.env.KEYCLOAK_ISSUER}/protocol/openid-connect/token`, {
      method: "POST",
      headers: { "Content-Type": "application/x-www-form-urlencoded" },
      body: new URLSearchParams({
        grant_type: "refresh_token",
        refresh_token: refreshToken,
        client_id: process.env.KEYCLOAK_CLIENT_ID!,
        client_secret: process.env.KEYCLOAK_CLIENT_SECRET!,
      }),
    });
    if (!response.ok) {
      throw new Error(`رفرش توکن Keycloak با کد ${response.status} رد شد`);
    }
    return response.json() as Promise<RefreshResult>;
  })();

  inFlightRefreshes.set(refreshToken, promise);
  // promise.finally() یک Promise جدید و مستقل برمی‌گرداند که اگر promise اصلی
  // reject شود، آن هم reject می‌شود -- بدون catch جداگانه، Node آن را
  // unhandledRejection گزارش می‌دهد، حتی وقتی خودِ promise اصلی را فراخوان‌ها
  // به‌درستی catch کرده باشند (کشف در Phase 14).
  promise.finally(() => setTimeout(() => inFlightRefreshes.delete(refreshToken), 10_000)).catch(() => {});
  return promise;
}

type TokenExtras = {
  accessToken?: string;
  refreshToken?: string;
  accessTokenExpiresAt?: number;
  roles?: string[];
  error?: "RefreshTokenMissing" | "RefreshAccessTokenError";
};

export const { handlers, auth, signIn, signOut } = NextAuth({
  providers: [
    Keycloak({
      clientId: process.env.KEYCLOAK_CLIENT_ID,
      clientSecret: process.env.KEYCLOAK_CLIENT_SECRET,
      issuer: process.env.KEYCLOAK_ISSUER,
    }),
  ],
  session: { strategy: "jwt" },
  callbacks: {
    async jwt({ token, account }) {
      const current = token as typeof token & TokenExtras;

      // ورود اولیه: Account فقط همین یک‌بار (بلافاصله بعد از OAuth Callback) در دسترس است
      if (account?.access_token) {
        return {
          ...current,
          accessToken: account.access_token,
          refreshToken: account.refresh_token,
          accessTokenExpiresAt: (account.expires_at ?? 0) * 1000,
          roles: extractRoles(account.access_token),
        } satisfies typeof current;
      }

      if (current.accessTokenExpiresAt && Date.now() < current.accessTokenExpiresAt) {
        return current;
      }

      // منقضی شده -- تلاش برای Refresh (طبق ADR-0003: Refresh Token Rotation در سطح Realm)
      if (!current.refreshToken) {
        return { ...current, error: "RefreshTokenMissing" };
      }
      try {
        const refreshed = await refreshAccessToken(current.refreshToken);
        return {
          ...current,
          accessToken: refreshed.access_token,
          refreshToken: refreshed.refresh_token ?? current.refreshToken,
          accessTokenExpiresAt: Date.now() + refreshed.expires_in * 1000,
          roles: extractRoles(refreshed.access_token),
          error: undefined,
        };
      } catch {
        return { ...current, error: "RefreshAccessTokenError" };
      }
    },
    async session({ session, token }) {
      const current = token as typeof token & TokenExtras;
      session.accessToken = current.accessToken;
      session.roles = current.roles ?? [];
      session.error = current.error;
      return session;
    },
  },
});
