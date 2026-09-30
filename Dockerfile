# DroidSiege lab backend — local/educational use ONLY.
# The SECURE_MODE=off default serves intentionally vulnerable endpoints;
# never expose this container or image to any public network.

FROM gradle:8.9-jdk17 AS build
WORKDIR /workspace
COPY gradle/libs.versions.toml gradle/libs.versions.toml
COPY settings.gradle.kts settings.gradle.kts
COPY build.gradle.kts build.gradle.kts
COPY backend backend
# backend-only build: drop the android module inside the image so the
# Android SDK is never needed (repo files are untouched)
RUN sed -i 's/include(":app")//' settings.gradle.kts \
    && gradle :backend:installDist --no-daemon

FROM eclipse-temurin:17-jre
WORKDIR /app
COPY --from=build /workspace/backend/build/install/backend ./
ENV PORT=8080 \
    SECURE_MODE=off
EXPOSE 8080
CMD ["bin/backend"]
