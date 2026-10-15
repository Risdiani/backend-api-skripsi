# =============================================================
# STAGE 1 - BUILD
# =============================================================
FROM maven:3.9.11-eclipse-temurin-21 AS builder

WORKDIR /app

COPY pom.xml .

RUN mvn dependency:go-offline -B

COPY src ./src

RUN mvn clean package -DskipTests


# =============================================================
# STAGE 2 - RUN
# =============================================================
FROM eclipse-temurin:21-jre

WORKDIR /app

COPY --from=builder /app/target/backend-api-0.0.1-SNAPSHOT.jar app.jar

EXPOSE 10000

ENTRYPOINT ["java", "-jar", "app.jar"]