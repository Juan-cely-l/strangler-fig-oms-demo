FROM maven:3.9.9-eclipse-temurin-21 AS build
WORKDIR /workspace

COPY pom.xml .
COPY common/pom.xml common/pom.xml
COPY strangler-gateway/pom.xml strangler-gateway/pom.xml
COPY legacy-oms-service/pom.xml legacy-oms-service/pom.xml
COPY modern-order-service/pom.xml modern-order-service/pom.xml
COPY inventory-service/pom.xml inventory-service/pom.xml
RUN mvn -pl modern-order-service -am dependency:go-offline

COPY common/src common/src
COPY modern-order-service/src modern-order-service/src
RUN mvn -pl modern-order-service -am package -DskipTests

FROM eclipse-temurin:21-jre-alpine
RUN addgroup -S app && adduser -S app -G app
WORKDIR /app
COPY --from=build /workspace/modern-order-service/target/modern-order-service-*.jar app.jar
USER app
EXPOSE 8082
HEALTHCHECK --interval=30s --timeout=5s --start-period=45s --retries=3 \
  CMD wget -qO- http://localhost:8082/actuator/health >/dev/null || exit 1
ENTRYPOINT ["java", "-jar", "/app/app.jar"]
