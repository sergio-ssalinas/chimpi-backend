# ---------- Etapa 1: compilar ----------
FROM maven:3.9-eclipse-temurin-17 AS build
WORKDIR /app
# Primero solo el pom: Docker cachea las dependencias y no las baja en cada cambio de codigo.
COPY pom.xml .
RUN mvn -q -B dependency:go-offline
COPY src ./src
RUN mvn -q -B -DskipTests package

# ---------- Etapa 2: imagen liviana para correr ----------
FROM eclipse-temurin:17-jre-jammy
# El sistema usa la hora local (apertura de caja, ventas del dia, reportes): sin esto el
# contenedor corre en UTC y una venta de las 22:00 en Paraguay quedaria con la fecha del dia siguiente.
ENV TZ=America/Asuncion \
    SPRING_PROFILES_ACTIVE=prod \
    JAVA_TOOL_OPTIONS="-XX:MaxRAMPercentage=70 -XX:+UseSerialGC -Xss512k -Duser.timezone=America/Asuncion"
RUN useradd --system --uid 1001 app
WORKDIR /app
COPY --from=build /app/target/lavabarber-api-*.jar app.jar
USER app
EXPOSE 8080
ENTRYPOINT ["java", "-jar", "app.jar"]
