FROM gradle:8.10-jdk21 AS build
WORKDIR /app
COPY settings.gradle.kts build.gradle.kts ./
RUN gradle dependencies --no-daemon || true
COPY . .
RUN set -eux; \
    for i in 1 2 3 4 5; do \
      gradle bootJar --no-daemon --refresh-dependencies && exit 0; \
      echo "Gradle bootJar failed (attempt $i), retrying..."; \
      sleep $((i * 10)); \
    done; \
    exit 1

FROM eclipse-temurin:21-jre
WORKDIR /app
COPY --from=build /app/build/libs/*.jar app.jar
EXPOSE 8080
ENTRYPOINT ["java","-jar","/app/app.jar"]
