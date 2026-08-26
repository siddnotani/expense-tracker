FROM maven:3.9.9-eclipse-temurin-21 AS build
WORKDIR /workspace
COPY . .
RUN mvn -B -pl services/accounts -am package -DskipTests
RUN mkdir -p /layers && java -Djarmode=layertools -jar services/accounts/target/*.jar extract --destination /layers

FROM eclipse-temurin:21-jre
RUN apt-get update && apt-get install -y --no-install-recommends curl && rm -rf /var/lib/apt/lists/*
WORKDIR /app
COPY --from=build /layers/dependencies/ ./
COPY --from=build /layers/spring-boot-loader/ ./
COPY --from=build /layers/snapshot-dependencies/ ./
COPY --from=build /layers/application/ ./
EXPOSE 8082
HEALTHCHECK --interval=10s --timeout=5s --retries=12 CMD curl -fsS http://localhost:8082/actuator/health || exit 1
ENTRYPOINT ["java", "org.springframework.boot.loader.launch.JarLauncher"]
