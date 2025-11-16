# Usa una imagen base de Java 21, optimizada y segura.
FROM eclipse-temurin:21-jre-alpine

# Define el directorio de trabajo.
WORKDIR /app

# --- CORRECCIÓN ---
# Usamos un comodín (*) para que coincida con CUALQUIER versión que compile Maven
# (Así no falla si cambiás de 1.0.0 a 1.0.1, etc.)
ARG JAR_FILE=target/ms-camiones-*.jar

# Copia el .jar
COPY ${JAR_FILE} app.jar

# Documenta el puerto que usa Spring Boot (8083)
EXPOSE 8083

# Comando que se ejecuta al iniciar el contenedor.
ENTRYPOINT ["java", "-jar", "app.jar"]