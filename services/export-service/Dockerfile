FROM eclipse-temurin:17-jre-alpine
WORKDIR /app

LABEL maintainer="DataFlow Platform Team"
LABEL service="export-service"
LABEL version="1.0.0"

COPY target/export-service-*.jar app.jar

HEALTHCHECK --interval=30s --timeout=10s --start-period=60s --retries=3 \
  CMD wget --no-verbose --tries=1 --spider http://localhost:8083/actuator/health || exit 1

EXPOSE 8083

# Export service cần nhiều heap hơn vì xử lý dữ liệu lớn 1M+ records
ENTRYPOINT ["java", \
  "-XX:+UseContainerSupport", \
  "-XX:MaxRAMPercentage=75.0", \
  "-XX:+UseG1GC", \
  "-XX:G1HeapRegionSize=16m", \
  "-Djava.security.egd=file:/dev/./urandom", \
  "-jar", "app.jar"]
