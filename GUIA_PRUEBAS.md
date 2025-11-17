# 🚀 GUÍA DE PRUEBAS - Circuit Breaker & Comunicación entre Microservicios

## ✅ PASO 1: PREPARAR LOS CAMBIOS (Circuit Breaker + Resilience4J)

### 1.1 Agregar dependencias a `ms-camiones/pom.xml`

Necesitas agregar Resilience4J al pom.xml de ms-camiones. Busca la sección `</dependencies>` y ANTES de ella, agrega:

```xml
<!-- Resilience4J para Circuit Breaker, Retry y Timeout -->
<dependency>
    <groupId>io.github.resilience4j</groupId>
    <artifactId>resilience4j-spring-boot3</artifactId>
    <version>2.1.0</version>
</dependency>
<dependency>
    <groupId>io.github.resilience4j</groupId>
    <artifactId>resilience4j-circuitbreaker</artifactId>
    <version>2.1.0</version>
</dependency>
<dependency>
    <groupId>io.github.resilience4j</groupId>
    <artifactId>resilience4j-retry</artifactId>
    <version>2.1.0</version>
</dependency>
<dependency>
    <groupId>io.github.resilience4j</groupId>
    <artifactId>resilience4j-timelimiter</artifactId>
    <version>2.1.0</version>
</dependency>
<dependency>
    <groupId>io.github.resilience4j</groupId>
    <artifactId>resilience4j-core</artifactId>
    <version>2.1.0</version>
</dependency>
```

### 1.2 Agregar configuración a `ms-camiones/src/main/resources/application.properties`

Agrega al final del archivo:

```properties
# ==============================================================================
# 5. CONFIGURACIÓN DE RESILIENCE4J (Circuit Breaker, Retry, Timeout)
# ==============================================================================
# Circuit Breaker para ms-rutas
resilience4j.circuitbreaker.instances.ms-rutas.register-health-indicator=true
resilience4j.circuitbreaker.instances.ms-rutas.failure-rate-threshold=50
resilience4j.circuitbreaker.instances.ms-rutas.slow-call-rate-threshold=50
resilience4j.circuitbreaker.instances.ms-rutas.wait-duration-in-open-state=10000
resilience4j.circuitbreaker.instances.ms-rutas.permitted-number-of-calls-in-half-open-state=3
resilience4j.circuitbreaker.instances.ms-rutas.slow-call-duration-threshold=2000
resilience4j.circuitbreaker.instances.ms-rutas.automatic-transition-from-open-to-half-open-enabled=true

# Retry para ms-rutas
resilience4j.retry.instances.ms-rutas.max-attempts=3
resilience4j.retry.instances.ms-rutas.wait-duration=1000
resilience4j.retry.instances.ms-rutas.retry-exceptions=java.io.IOException,java.net.ConnectException,org.springframework.web.client.ResourceAccessException

# TimeLimiter para ms-rutas (timeout)
resilience4j.timelimiter.instances.ms-rutas.timeout-duration=5000
resilience4j.timelimiter.instances.ms-rutas.cancel-running-future=true
```

---

## ✅ PASO 2: COMPILAR LOS MICROSERVICIOS

```bash
# Desde la carpeta raíz del proyecto
cd c:\Users\Usuario\Documents\tpi_integrador

# Compilar todos los microservicios
mvn clean install -DskipTests

# O solo ms-camiones si quieres ser más rápido
cd ms-camiones && mvn clean package -DskipTests
```

---

## ✅ PASO 3: LEVANTAR LOS SERVICIOS CON DOCKER

```bash
# En la carpeta raíz
docker-compose up -d

# Verificar que todos los servicios estén corriendo
docker-compose ps
```

Esperado:
```
CONTAINER ID   IMAGE           PORTS
xxxxx          ms-gateway      0.0.0.0:8080
xxxxx          ms-camiones     0.0.0.0:8083
xxxxx          ms-rutas        0.0.0.0:8082
xxxxx          ms-solicitudes  0.0.0.0:8081
xxxxx          keycloak        0.0.0.0:8180
xxxxx          postgres        0.0.0.0:5432
```

---

## ✅ PASO 4: VERIFICAR CONECTIVIDAD

### 4.1 Verificar que PostgreSQL está arriba
```bash
psql -h localhost -U tpi_user -d db_logistica -c "SELECT 1;"
```

Deberías ver: `?column?` `1`

### 4.2 Verificar que los microservicios responden
```bash
# Gateway
curl http://localhost:8080/actuator/health

# ms-camiones
curl http://localhost:8083/actuator/health

# ms-rutas
curl http://localhost:8082/actuator/health

# ms-solicitudes
curl http://localhost:8081/actuator/health
```

---

## ✅ PASO 5: POBLAR LA BASE DE DATOS CON DATOS DE PRUEBA

### 5.1 Crear script SQL con datos de prueba

Crea un archivo `scripts/populate-db.sql`:

```sql
-- Tabla: camiones
INSERT INTO camion (patente, marca, modelo, capacidad_peso, capacidad_volumen, disponibilidad) VALUES
('ABC123', 'Volvo', 'FH16', 25000.00, 100.00, true),
('DEF456', 'Scania', 'R450', 22000.00, 95.00, true),
('GHI789', 'Mercedes', 'Actros', 20000.00, 90.00, true);

-- Tabla: tramos (dependiendo de tu estructura)
INSERT INTO tramo (id, km_inicio, km_final, disponible) VALUES
(1, 0, 100, true),
(2, 100, 200, true),
(3, 200, 300, true);
```

### 5.2 Ejecutar el script
```bash
psql -h localhost -U tpi_user -d db_logistica -f scripts/populate-db.sql
```

O conectarte manualmente:
```bash
psql -h localhost -U tpi_user -d db_logistica

# Dentro de psql:
\dt  -- Ver tablas
SELECT * FROM camion;  -- Ver camiones
```

---

## ✅ PASO 6: PROBAR ENDPOINTS CON POSTMAN/CURL

### 6.1 Obtener todos los camiones
```bash
curl -X GET http://localhost:8083/camiones
```

### 6.2 Obtener camiones disponibles
```bash
curl -X GET "http://localhost:8083/camiones?disponibilidad=true"
```

### 6.3 Obtener camión por patente
```bash
curl -X GET http://localhost:8083/camiones/ABC123
```

### 6.4 Obtener camiones aptos (con capacidad mínima)
```bash
curl -X GET "http://localhost:8083/camiones/aptos?pesoRequerido=10000&volumenRequerido=50"
```

### 6.5 Obtener tramos del transportista (con ms-rutas)
```bash
# Primero obtén un JWT token de Keycloak (o usa un token válido)
curl -X GET "http://localhost:8083/camiones/ABC123/tramos" \
  -H "Authorization: Bearer TU_JWT_TOKEN"
```

---

## ✅ PASO 7: VERIFICAR CIRCUIT BREAKER FUNCIONANDO

### 7.1 Ver estado del Circuit Breaker
```bash
# Endpoint de health con detalles
curl http://localhost:8083/actuator/health/detailed
```

### 7.2 Simular fallo de ms-rutas
```bash
# Detener ms-rutas
docker-compose stop ms-rutas

# Hacer llamadas a camiones/tramos
curl "http://localhost:8083/camiones/ABC123/tramos" \
  -H "Authorization: Bearer TOKEN"

# Deberías ver que el Circuit Breaker se abre después de N fallos
```

### 7.3 Recuperarse del fallo
```bash
# Levantar ms-rutas nuevamente
docker-compose start ms-rutas

# El Circuit Breaker debería entrar en HALF_OPEN y luego cerrar (CLOSED)
```

---

## 🔍 TROUBLESHOOTING

| Problema | Solución |
|----------|----------|
| `Connection refused` | Docker no está corriendo. Ejecuta `docker-compose up -d` |
| `psql: command not found` | Instala PostgreSQL client o usa `docker exec` |
| Circuit Breaker abierto | Es normal si ms-rutas falla. Espera 10s o reinicia el servicio |
| JWT token inválido | Usa Keycloak para obtener un token válido primero |

---

## 📊 URLS ÚTILES

| Servicio | URL | Descripción |
|----------|-----|-----------|
| Keycloak | http://localhost:8180 | Admin panel (usuario: admin, pass: admin123) |
| ms-camiones API | http://localhost:8083/swagger-ui.html | Documentación Swagger |
| ms-rutas API | http://localhost:8082/swagger-ui.html | Documentación Swagger |
| PostgreSQL | localhost:5432 | Base de datos |

---

## 🚀 PRÓXIMOS PASOS

Una vez que todo funcione:
1. Implementar Circuit Breaker en **ms-solicitudes** (llama a ms-camiones)
2. Implementar Circuit Breaker en **ms-rutas** (llama a Google Maps API)
3. Crear módulo `commons` para compartir la lógica de Resilience4J
