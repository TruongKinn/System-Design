FROM eclipse-temurin:17-jre-alpine
WORKDIR /app

LABEL maintainer="DataFlow Platform Team"
LABEL service="import-service"
LABEL version="1.0.0"

COPY target/import-service-*.jar app.jar

HEALTHCHECK --interval=30s --timeout=10s --start-period=60s --retries=3 \
  CMD wget --no-verbose --tries=1 --spider http://localhost:8084/actuator/health || exit 1

EXPOSE 8084

# Import service cần nhiều heap hơn vì xử lý file Excel/CSV lớn
ENTRYPOINT ["java", \
  "-XX:+UseContainerSupport", \
  "-XX:MaxRAMPercentage=75.0", \
  "-XX:+UseG1GC", \
  "-XX:G1HeapRegionSize=16m", \
  "-Djava.security.egd=file:/dev/./urandom", \
  "-jar", "app.jar"]
