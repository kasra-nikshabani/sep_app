# تصویر Production (Phase 20، ADR-0022). برای توسعه از mvnw spring-boot:run روی Host استفاده
# می‌شود (طبق روال این پروژه از Phase 6) -- این Dockerfile فقط برای Deploy واقعی است.

# ---- Stage 1: Build ----
# دقیقاً همان JDK/Maven که backend/pom.xml نیاز دارد (java.version=21). eclipse-temurin
# ایمیج رسمی OpenJDK با Tag های پایدار و به‌روز است.
FROM eclipse-temurin:21-jdk-alpine AS build
WORKDIR /build

COPY .mvn/ .mvn/
COPY mvnw pom.xml ./
COPY src/ src/
# تست‌ها اینجا اجرا نمی‌شوند -- طبق ADR-0020، تست خودکار مسئولیت CI/اجرای محلی
# (./mvnw test) است، نه Build ایمیج؛ ZibalPaymentProviderTest هم به یک Sandbox خارجی
# وصل می‌شود که در محیط Build ایمیج اصلاً در دسترس نیست.
#
# نکته: dependency:go-offline عمداً استفاده نشد -- کل Reactor (شامل Pluginهای گزارش‌گیری
# هرگز صدا‌زده‌نشده مثل maven-site-plugin) را هم Resolve می‌کند و در عمل با خطای واقعی شبکه
# روی یک Artifact قدیمی/غیرمرتبط شکست خورد؛ package مستقیم فقط همان چیزی را می‌گیرد که
# واقعاً لازم است.
RUN ./mvnw -B package -DskipTests

# ---- Stage 2: Runtime ----
# فقط JRE (نه JDK کامل) -- ایمیج نهایی به‌مراتب کوچک‌تر؛ هیچ ابزار Build/Compile
# در Production لازم نیست.
FROM eclipse-temurin:21-jre-alpine
WORKDIR /app

# curl فقط برای HEALTHCHECK لازم است (ایمیج پایه‌ی Alpine آن را ندارد)؛ هیچ Dependency
# سطح‌اپلیکیشن جدیدی اضافه نمی‌شود.
RUN apk add --no-cache curl \
    && addgroup -S sepahan && adduser -S sepahan -G sepahan

COPY --from=build /build/target/*.jar app.jar
# لاگ JSON ساختاریافته (application.yml: logging.file.name=logs/backend.json.log، Phase 17)
# اینجا نوشته می‌شود -- در docker-compose.prod.yml یک Volume نام‌دار (backend_logs) روی همین
# مسیر Mount می‌شود تا Promtail بتواند از یک Container جدا آن را بخواند.
RUN mkdir -p /app/logs && chown -R sepahan:sepahan /app
USER sepahan

EXPOSE 8081
HEALTHCHECK --interval=10s --timeout=5s --retries=10 --start-period=30s \
    CMD curl -f http://localhost:8081/actuator/health || exit 1

ENTRYPOINT ["java", "-jar", "app.jar"]
