FROM maven:3.9.11-eclipse-temurin-17 AS build
WORKDIR /workspace

COPY pom.xml .
COPY src src

RUN mvn clean package -DskipTests

FROM eclipse-temurin:17-jre-jammy
WORKDIR /app

RUN addgroup --system spring && adduser --system --ingroup spring spring

COPY --from=build /workspace/target/*.jar app.jar
RUN mkdir -p /app/images && chown -R spring:spring /app

USER spring
EXPOSE 9090

ENTRYPOINT ["java", "-jar", "/app/app.jar"]
