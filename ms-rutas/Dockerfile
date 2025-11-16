# 1. Empezá con una imagen base que tenga Java 21 (JDK 21)
FROM eclipse-temurin:21-jre-alpine

# 2. Argumento para saber dónde está el .jar (que crea Maven)
# Usamos un comodín (*) para que coincida con cualquier versión
ARG JAR_FILE=target/ms-rutas-*.jar

# 3. Copiá el .jar desde tu PC adentro de la "caja"
#    y renombralo a 'app.jar' para que sea fácil de encontrar
COPY ${JAR_FILE} app.jar

# 4. Exponé el puerto en el que corre tu servicio (el de tu application.properties)
# ms-rutas corre en el 8082
EXPOSE 8082

# 5. El comando para arrancar tu servicio cuando se inicie el contenedor
ENTRYPOINT ["java", "-jar", "/app.jar"]