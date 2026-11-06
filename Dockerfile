# ---- 1. aşama: derleme ----
FROM eclipse-temurin:21-jdk AS build
WORKDIR /workspace

COPY .mvn/ .mvn/
COPY mvnw pom.xml ./
# Windows'ta CRLF ile kaydedilmiş olsa bile mvnw'nin çalışması için satır sonlarını düzelt
RUN sed -i 's/\r$//' mvnw && chmod +x mvnw && ./mvnw -B -q dependency:go-offline

COPY src/ src/
RUN ./mvnw -B -q package -DskipTests

# ---- 2. aşama: çalıştırma ----
FROM eclipse-temurin:21-jre
WORKDIR /app
RUN groupadd --system pos && useradd --system --gid pos --home /app pos

COPY --from=build /workspace/target/restoran-pos-api-*.jar /app/app.jar

USER pos
EXPOSE 8080
ENTRYPOINT ["java", "-XX:MaxRAMPercentage=75", "-jar", "/app/app.jar"]
