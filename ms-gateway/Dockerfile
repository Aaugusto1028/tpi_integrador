# Usa una imagen base de Java 21, optimizada y segura.
FROM eclipse-temurin:21-jre-alpine

# Define el directorio de trabajo.
WORKDIR /app

# Copiamos el JAR explícito generado por Maven (menos propenso a globs ambiguos)
# Asegúrate de ejecutar `mvn package` antes de construir la imagen si no usas multi-stage.
COPY target/ms-gateway-1.0.0-SNAPSHOT.jar /app/app.jar

# Documenta el puerto (8080)
EXPOSE 8080

# Comando que se ejecuta al iniciar el contenedor.
ENTRYPOINT ["java", "-jar", "/app/app.jar"]