# Usa una imagen base de Java 21, optimizada y segura.
FROM eclipse-temurin:21-jre-alpine

# Define el directorio de trabajo.
WORKDIR /app

# Argumento para el JAR de Maven
ARG JAR_FILE=target/ms-gateway-*.jar

# Copia el .jar
COPY ${JAR_FILE} app.jar

# Documenta el puerto (8080)
EXPOSE 8080

# Comando que se ejecuta al iniciar el contenedor.
ENTRYPOINT ["java", "-jar", "/app.jar"]