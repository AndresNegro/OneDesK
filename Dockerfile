# Imagen para publicar la aplicacion (Render la arma sola desde este archivo).
# Se compila adentro con Maven y despues se copia solo el jar a una imagen con el JRE,
# asi la imagen final no carga Maven ni el codigo fuente.
FROM maven:3.9-eclipse-temurin-17 AS compilacion
WORKDIR /app
COPY pom.xml .
RUN mvn -q dependency:go-offline
COPY src src
RUN mvn -q package -DskipTests

FROM eclipse-temurin:17-jre
WORKDIR /app
COPY --from=compilacion /app/target/*.jar app.jar
# El plan gratis da 512 MB en total: la JVM se queda con el 60% para el heap y deja el resto
# para el metaspace, los hilos y el sistema. SerialGC es el que menos memoria propia usa.
ENV JAVA_TOOL_OPTIONS="-XX:MaxRAMPercentage=60 -XX:+UseSerialGC -Xss512k -XX:TieredStopAtLevel=1"
# Render avisa en PORT por donde tiene que escuchar
CMD ["sh", "-c", "java -Dserver.port=${PORT:-8080} -jar app.jar"]
