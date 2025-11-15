# Usa una imagen base de Java 21, optimizada y segura.
FROM openjdk:21-jdk-slim

# Define el directorio de trabajo.
WORKDIR /app

# Copia el archivo .jar compilado. El nombre se forma con el artifactId y la versión.
# Asumo el nombre estándar de Maven.
COPY target/ms-camiones-0.0.1-SNAPSHOT.jar app.jar

# Documenta el puerto que usa Spring Boot (8083, como en docker-compose.yml)
EXPOSE 8083

# Comando que se ejecuta al iniciar el contenedor.
ENTRYPOINT ["java", "-jar", "app.jar"]