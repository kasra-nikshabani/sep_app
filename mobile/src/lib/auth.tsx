import React, { createContext, useCallback, useContext, useEffect, useMemo, useState } from "react";
import { Platform } from "react-native";
import * as AuthSession from "expo-auth-session";
import * as WebBrowser from "expo-web-browser";
import { getItem, setItem, deleteItem } from "./storage";

WebBrowser.maybeCompleteAuthSession();

const KEYCLOAK_ISSUER = process.env.EXPO_PUBLIC_KEYCLOAK_ISSUER as string;
const CLIENT_ID = process.env.EXPO_PUBLIC_KEYCLOAK_CLIENT_ID as string;

const STORAGE_KEY = "sepahan.auth.tokens";
const PENDING_VERIFIER_KEY = "sepahan.auth.pending_verifier";

type StoredTokens = {
  accessToken: string;
  refreshToken: string;
  expiresAt: number; // epoch ms
};

type Session = {
  accessToken: string;
  roles: string[];
  displayName: string | null;
};

type AuthContextValue = {
  session: Session | null;
  isLoading: boolean;
  login: () => Promise<void>;
  logout: () => Promise<void>;
  getValidAccessToken: () => Promise<string | null>;
  completeExchange: (code: string) => Promise<boolean>;
  isDiscoveryReady: boolean;
};

const AuthContext = createContext<AuthContextValue | null>(null);

function decodeJwtPayload(token: string): Record<string, unknown> {
  const payload = token.split(".")[1];
  const base64 = payload.replace(/-/g, "+").replace(/_/g, "/");
  // atob از RN 0.72 به‌بعد Global است (بدون Polyfill)؛ escape/unescape برای
  // درست خواندن UTF-8 فارسی از خروجی Latin1 خودِ atob لازم است.
  const json = atob(base64);
  return JSON.parse(decodeURIComponent(escape(json)));
}

function sessionFromAccessToken(accessToken: string): Session {
  const payload = decodeJwtPayload(accessToken);
  const realmAccess = payload.realm_access as { roles?: string[] } | undefined;
  return {
    accessToken,
    roles: realmAccess?.roles ?? [],
    displayName: (payload.name as string) ?? (payload.preferred_username as string) ?? null,
  };
}

// از تجربه‌ی Phase 14 (Admin Panel): چند تماس هم‌زمان API نباید هرکدام جدا
// Refresh Token را مصرف کنند (Keycloak با revokeRefreshToken=true هر Refresh
// Token را فقط یک‌بار می‌پذیرد) -- یک Promise مشترک برای تمام صداهای هم‌زمان.
let inFlightRefresh: Promise<StoredTokens | null> | null = null;

async function refreshTokens(discovery: AuthSession.DiscoveryDocument, refreshToken: string): Promise<StoredTokens | null> {
  try {
    const result = await AuthSession.refreshAsync({ clientId: CLIENT_ID, refreshToken }, discovery);
    if (!result.accessToken) return null;
    const tokens: StoredTokens = {
      accessToken: result.accessToken,
      refreshToken: result.refreshToken ?? refreshToken,
      expiresAt: Date.now() + (result.expiresIn ?? 300) * 1000,
    };
    await setItem(STORAGE_KEY, JSON.stringify(tokens));
    return tokens;
  } catch {
    await deleteItem(STORAGE_KEY);
    return null;
  }
}

export function AuthProvider({ children }: { children: React.ReactNode }) {
  const discovery = AuthSession.useAutoDiscovery(KEYCLOAK_ISSUER);
  const [session, setSession] = useState<Session | null>(null);
  const [isLoading, setIsLoading] = useState(true);

  const redirectUri = AuthSession.makeRedirectUri({ scheme: "sepahan", path: "auth/callback" });

  const [request, , promptAsync] = AuthSession.useAuthRequest(
    {
      clientId: CLIENT_ID,
      redirectUri,
      scopes: ["openid", "profile", "fan-identity"],
      usePKCE: true,
    },
    discovery,
  );

  const loadFromStorage = useCallback(async () => {
    const raw = await getItem(STORAGE_KEY);
    if (!raw) {
      setIsLoading(false);
      return;
    }
    const tokens = JSON.parse(raw) as StoredTokens;
    if (Date.now() < tokens.expiresAt) {
      setSession(sessionFromAccessToken(tokens.accessToken));
      setIsLoading(false);
      return;
    }
    if (!discovery) {
      setIsLoading(false);
      return;
    }
    const refreshed = await refreshTokens(discovery, tokens.refreshToken);
    setSession(refreshed ? sessionFromAccessToken(refreshed.accessToken) : null);
    setIsLoading(false);
  }, [discovery]);

  useEffect(() => {
    if (discovery) {
      loadFromStorage();
    }
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [discovery]);

  // مشترک بین دو مسیر:
  // ۱) Native: promptAsync خودش Deep Link را می‌گیرد و همین‌جا Exchange می‌شود.
  // ۲) Web: کل صفحه Redirect واقعی می‌شود (نه Popup+Message) -- بنابراین
  //    نمونه‌ی فعلی AuthProvider از بین می‌رود و app/auth/callback.tsx (بعد
  //    از Reload کامل) این تابع را دوباره صدا می‌زند. چون Code Verifier در
  //    Memory (خودِ Hook) از بین رفته، از قبل در Storage ذخیره شده بود.
  const completeExchange = useCallback(
    async (code: string): Promise<boolean> => {
      if (!discovery) return false;
      const codeVerifier = (await getItem(PENDING_VERIFIER_KEY)) ?? "";
      try {
        const tokenResponse = await AuthSession.exchangeCodeAsync(
          { clientId: CLIENT_ID, code, redirectUri, extraParams: { code_verifier: codeVerifier } },
          discovery,
        );
        await deleteItem(PENDING_VERIFIER_KEY);
        const tokens: StoredTokens = {
          accessToken: tokenResponse.accessToken,
          refreshToken: tokenResponse.refreshToken ?? "",
          expiresAt: Date.now() + (tokenResponse.expiresIn ?? 300) * 1000,
        };
        await setItem(STORAGE_KEY, JSON.stringify(tokens));
        setSession(sessionFromAccessToken(tokens.accessToken));
        return true;
      } catch {
        return false;
      }
    },
    [discovery, redirectUri],
  );

  const login = useCallback(async () => {
    if (!discovery || !request?.codeVerifier) return;
    setItem(PENDING_VERIFIER_KEY, request.codeVerifier);

    // روی Web، promptAsync() سعی می‌کند Popup باز کند -- در مرورگرهای
    // خودکار/سخت‌گیر (و حتی خیلی مرورگرهای واقعی) این با ERR_WEB_BROWSER_BLOCKED
    // رد می‌شود چون بین کلیک کاربر و باز شدن Popup حتی یک Microtask هم فاصله
    // می‌افتد (کشف در Phase 15). Redirect کامل تمام صفحه (بدون Popup) قابل
    // اعتمادتر است و همان چیزی است که واقعاً اتفاق می‌افتد وقتی کاربر برمی‌گردد
    // (طبق app/auth/callback.tsx).
    if (Platform.OS === "web" && request.url) {
      window.location.assign(request.url);
      return;
    }

    const result = await promptAsync();
    if (result.type === "success" && result.params.code) {
      await completeExchange(result.params.code);
    }
  }, [discovery, promptAsync, request, completeExchange]);

  const logout = useCallback(async () => {
    await deleteItem(STORAGE_KEY);
    setSession(null);
  }, []);

  const getValidAccessToken = useCallback(async (): Promise<string | null> => {
    const raw = await getItem(STORAGE_KEY);
    if (!raw || !discovery) return null;
    const tokens = JSON.parse(raw) as StoredTokens;
    if (Date.now() < tokens.expiresAt - 10_000) {
      return tokens.accessToken;
    }
    if (!inFlightRefresh) {
      inFlightRefresh = refreshTokens(discovery, tokens.refreshToken).finally(() => {
        inFlightRefresh = null;
      });
    }
    const refreshed = await inFlightRefresh;
    if (!refreshed) {
      setSession(null);
      return null;
    }
    setSession(sessionFromAccessToken(refreshed.accessToken));
    return refreshed.accessToken;
  }, [discovery]);

  const value = useMemo(
    () => ({ session, isLoading, login, logout, getValidAccessToken, completeExchange, isDiscoveryReady: !!discovery }),
    [session, isLoading, login, logout, getValidAccessToken, completeExchange, discovery],
  );

  return <AuthContext.Provider value={value}>{children}</AuthContext.Provider>;
}

export function useAuth(): AuthContextValue {
  const ctx = useContext(AuthContext);
  if (!ctx) throw new Error("useAuth باید داخل AuthProvider استفاده شود");
  return ctx;
}
