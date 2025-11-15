# 1. Empezá con una imagen base que tenga Java 21 (JDK 21)
FROM openjdk:21-slim

# 2. Argumento para saber dónde está el .jar (que crea Maven)
ARG JAR_FILE=target/*.jar

# 3. Copiá el .jar desde tu PC adentro de la "caja"
#    y renombralo a 'app.jar' para que sea fácil de encontrar
COPY ${JAR_FILE} app.jar

# 4. Exponé el puerto en el que corre tu servicio (el de tu application.yml)
EXPOSE 8081

# 5. El comando para arrancar tu servicio cuando se inicie el contenedor
ENTRYPOINT ["java", "-jar", "/app.jar"]