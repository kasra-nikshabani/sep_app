# تصویر Production (Phase 20، ADR-0022) -- الگوی رسمی Next.js برای output: "standalone"
# (تأیید شده از node_modules/next/dist/docs همین نسخه‌ی نصب‌شده، طبق هشدار خودِ AGENTS.md
# این پوشه درباره‌ی تفاوت نسخه‌های Next.js با دانش عمومی). npm run dev محلی بدون تغییر باقی می‌ماند.

# ---- Stage 1: Dependencies ----
FROM node:22-alpine AS deps
WORKDIR /build
COPY package.json package-lock.json ./
RUN npm ci

# ---- Stage 2: Build ----
FROM node:22-alpine AS build
WORKDIR /build
COPY --from=deps /build/node_modules ./node_modules
COPY . .
# NEXT_PUBLIC_* در زمان Build داخل Bundle جاسازی می‌شوند (طبق مستندات Self-Hosting) --
# این پروژه فعلاً هیچ NEXT_PUBLIC_* ندارد (BFF -- مرورگر هرگز مستقیم Backend را صدا نمی‌زند).
RUN npm run build

# ---- Stage 3: Runtime ----
FROM node:22-alpine
WORKDIR /app
RUN addgroup -S nextjs && adduser -S nextjs -G nextjs

# فقط فایل‌های واقعاً لازم برای اجرا -- طبق مستندات output: standalone، public/.next/static
# به‌صورت خودکار داخل standalone/ کپی نمی‌شوند و باید دستی اضافه شوند.
COPY --from=build --chown=nextjs:nextjs /build/.next/standalone ./
COPY --from=build --chown=nextjs:nextjs /build/.next/static ./.next/static
COPY --from=build --chown=nextjs:nextjs /build/public ./public

USER nextjs
ENV PORT=3000 HOSTNAME=0.0.0.0
EXPOSE 3000

HEALTHCHECK --interval=10s --timeout=5s --retries=10 --start-period=15s \
    CMD node -e "require('http').get('http://127.0.0.1:3000',r=>process.exit(r.statusCode<500?0:1)).on('error',()=>process.exit(1))"

CMD ["node", "server.js"]
