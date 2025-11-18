# Contratos de Comunicación Entre Microservicios

**Última actualización:** 17 de noviembre, 2025  
**Estado:** ✅ Verificado y Corregido

---

## 📋 Tabla de Puertos

| Microservicio | Puerto | URL Base | Container Name |
|--------------|--------|----------|-----------------|
| ms-solicitudes | 8081 | `http://ms-solicitudes:8081` | ms-solicitudes |
| ms-rutas | 8082 | `http://ms-rutas:8082` | ms-rutas |
| ms-camiones | 8083 | `http://ms-camiones:8083` | ms-camiones |
| Keycloak | 8080 | `http://keycloak:8080` | keycloak |
| PostgreSQL | 5432 | `postgresql://db:5432` | db_logistica_tpi |

---

## 🔄 Comunicación Entre Servicios

### 1️⃣ ms-solicitudes → ms-rutas

**Cliente:** `RutasWebClient.java`  
**Base URL:** `http://ms-rutas:8082/rutas`

#### Endpoints Utilizados:

| Método | Endpoint | Descripción | Parámetros | Respuesta |
|--------|----------|-------------|-----------|----------|
| GET | `/tarifas` | Obtener tarifa vigente | - | `TarifaDTO` |
| GET | `/solicitud/{idSolicitud}/costo-real` | Obtener costo real por solicitud | `idSolicitud` (Long) | `CostoTrasladoDTO` |
| GET | `/ruta/{idRuta}/costo-real` | Obtener costo real por ruta | `idRuta` (Long) | `CostoTrasladoDTO` |
| POST | `/distancia` | Calcular distancia (no implementado en ms-rutas) | `CoordenadasRequest` | `DistanciaDTO` |

#### DTOs Intercambiados:

**TarifaDTO:**
```java
{
  "precioLitro": BigDecimal,          // Precio por litro de combustible
  "costoEstadiaDiario": BigDecimal    // Costo de estadía por día
}
```

**CostoTrasladoDTO:**
```java
{
  "costoKm": BigDecimal,              // Costo total por km recorrido
  "costoCombustible": BigDecimal,     // Costo total de combustible
  "costoEstadia": BigDecimal,         // Costo total de estadía
  "costoTotal": BigDecimal            // Suma de todos los costos
}
```

---

### 2️⃣ ms-solicitudes → ms-camiones

**Cliente:** `CamionesWebClient.java`  
**Base URL:** `http://ms-camiones:8083/camiones`

#### Endpoints Utilizados:

| Método | Endpoint | Descripción | Parámetros | Respuesta |
|--------|----------|-------------|-----------|----------|
| GET | `/promedios` | Obtener promedios de costos/consumo | `peso` (Double), `volumen` (Double) | `PromediosDTO` |
| GET | `/{patente}` | Obtener datos básicos de un camión | `patente` (String) | `Camion` |

#### DTOs Intercambiados:

**PromediosDTO:**
```java
{
  "costoPromedioPorKm": BigDecimal,      // Costo promedio por km
  "consumoPromedioPorKm": BigDecimal     // Consumo promedio por km
}
```

---

### 3️⃣ ms-rutas → ms-camiones

**Implementación:** Directa en `RutaServiceImpl.java`  
**Base URL:** `http://ms-camiones:8083/camiones`

#### Endpoints Utilizados:

| Método | Endpoint | Descripción | Parámetros | Respuesta |
|--------|----------|-------------|-----------|----------|
| GET | `/detalle/{patente}` | Obtener detalles de un camión para calcular costos | `patente` (String) | `CamionDetalleDTO` |

#### DTOs Intercambiados:

**CamionDetalleDTO:**
```java
{
  "patente": String,                     // Patente del camión
  "costoPorKm": BigDecimal,              // Costo por km del camión específico
  "consumoCombustibleKm": Double,        // Consumo de combustible por km
  "capacidadPeso": Double,               // Capacidad de peso en kg
  "capacidadVolumen": Double,            // Capacidad de volumen en m³
  "disponibilidad": Boolean              // Si el camión está disponible
}
```

---

### 4️⃣ ms-rutas → ms-solicitudes

**Implementación:** Directa en `RutaServiceImpl.asignarRuta()`  
**Base URL:** `http://ms-solicitudes:8081/solicitudes`

#### Endpoints Utilizados:

| Método | Endpoint | Descripción | Parámetros | Respuesta |
|--------|----------|-------------|-----------|----------|
| POST | `/{idSolicitud}/confirmar-ruta` | Confirmar que la ruta fue asignada | `idSolicitud` (Long) | Void |

**Nota:** Esta comunicación es "best-effort" (sin manejo de errores críticos).

---

### 5️⃣ ms-camiones → ms-rutas

**Cliente:** `CamionService.getTramosPorTransportista()`  
**Base URL:** Configurada dinámicamente  
**WebClient:** `@Qualifier("webClientRutas")`

#### Endpoints Utilizados:

| Método | Endpoint | Descripción | Parámetros | Respuesta |
|--------|----------|-------------|-----------|----------|
| GET | `/patente/{patenteCamion}/tramos` | Obtener tramos asignados a una patente | `patenteCamion` (String, path) | `List<TramoDTO>` |

#### DTOs Intercambiados:

**TramoDTO:**
```java
{
  "id": Long,                            // ID del tramo
  "origen": String,                      // Nombre del depósito origen
  "destino": String,                     // Nombre del depósito destino
  "estado": String,                      // Estado (ESTIMADO, ASIGNADO, INICIADO, FINALIZADO)
  "patenteCamionAsignado": String,       // Patente del camión asignado
  "distanciaKm": Double,                 // Distancia en km
  "costoAproximado": BigDecimal,         // Costo estimado
  "costoReal": BigDecimal,               // Costo real (si ya fue finalizado)
  "fechaHoraInicio": LocalDateTime,      // Fecha/hora de inicio
  "fechaHoraFin": LocalDateTime,         // Fecha/hora de finalización
  "precioEstadiaOrigen": BigDecimal,     // Precio de estadía en origen
  "precioEstadiaDestino": BigDecimal     // Precio de estadía en destino
}
```

---

## 🔐 Seguridad y Autenticación

### Autenticación OAuth2 + Keycloak

**Servidor:** `http://keycloak:8080/realms/tpi-backend`

Todos los endpoints requieren un token JWT válido en el header:
```
Authorization: Bearer <JWT_TOKEN>
```

**Excepciones (endpoints públicos sin autenticación):**
- ✅ `GET /rutas/tarifas`
- ✅ `GET /rutas/solicitud/{idSolicitud}/costo-real`
- ✅ `GET /rutas/ruta/{idRuta}/costo-real`
- ✅ `GET /rutas/patente/{patenteCamion}/tramos`
- ✅ `GET /camiones/detalle/{patente}`
- ✅ `GET /camiones/promedios`

(Estas excepciones permiten que otros microservicios accedan sin autenticación)

---

## 📊 Flujo de Cálculo de Costos

```
ms-solicitudes
  ├── Crear solicitud
  │   └── LLama a ms-rutas.obtenerTarifas() [para estimar costo]
  │
  └── Finalizar solicitud
      └── Llama a ms-rutas.obtenerCostoTrasladoRealPorSolicitud(idSolicitud)
          │
          └── RutaServiceImpl busca Ruta por idSolicitud
              ├── Para cada Tramo en la Ruta:
              │   └── Llama a ms-camiones.obtenerDetalleCamion(patente)
              │       └── Calcula: costoKm + costoCombustible + costoEstadia
              │
              └── Retorna CostoTrasladoDTO con desglose completo
```

---

## ⚠️ Cambios Realizados en Esta Sesión

### Problemas Identificados y Corregidos:

1. **❌ Puerto incorrecto en ms-rutas**
   - Problema: `ms-camiones:8082` 
   - Solución: Cambiar a `ms-camiones:8083` ✅

2. **❌ Puerto incorrecto en ms-solicitudes**
   - Problema: `ms-rutas:8081` / `ms-camiones:8082`
   - Solución: Cambiar a `ms-rutas:8082` / `ms-camiones:8083` ✅

3. **❌ Endpoint incorrecto en ms-rutas**
   - Problema: Llamaba a `/camiones/{patente}` 
   - Solución: Cambiar a `/camiones/detalle/{patente}` ✅

4. **❌ Conflicto de rutas en RutaController**
   - Problema: `GET /{idRuta}/costo-real` conflictua con `GET /{id}` y otros
   - Solución: Cambiar a `GET /ruta/{idRuta}/costo-real` ✅

5. **❌ Endpoint de tramos inconsistente**
   - Problema: `GET /tramos?patenteCamion={patente}` 
   - Solución: Cambiar a `GET /patente/{patenteCamion}/tramos` ✅

6. **❌ CamionService llamaba a endpoint incorrecto**
   - Problema: Llamaba a `/tramos?patenteCamion=...`
   - Solución: Cambiar a `/patente/{patenteCamion}/tramos` ✅

7. **❌ RutaController llamaba a puerto incorrecto de ms-solicitudes**
   - Problema: `ms-solicitudes:8082`
   - Solución: Cambiar a `ms-solicitudes:8081` ✅

---

## ✅ Estado Actual de Contratos

| Comunicación | Endpoint | Estado | Observaciones |
|--------------|----------|--------|--------------|
| ms-solicitudes → ms-rutas | `/tarifas` | ✅ OK | Completamente implementado |
| ms-solicitudes → ms-rutas | `/solicitud/{idSolicitud}/costo-real` | ✅ OK | Con cálculo de costos |
| ms-solicitudes → ms-rutas | `/ruta/{idRuta}/costo-real` | ✅ OK | Alternativa por ID de ruta |
| ms-solicitudes → ms-camiones | `/promedios` | ✅ OK | Para cálculos de estimados |
| ms-solicitudes → ms-camiones | `/{patente}` | ✅ OK | Consulta básica de camión |
| ms-rutas → ms-camiones | `/detalle/{patente}` | ✅ OK | Para cálculo de costo real |
| ms-rutas → ms-solicitudes | `/confirmar-ruta` | ⚠️ Best-effort | Sin manejo de errores críticos |
| ms-camiones → ms-rutas | `/patente/{patenteCamion}/tramos` | ✅ OK | Para transportista |

---

## 📝 DTOs Disponibles

### En ms-solicitudes
- `TarifaDTO`
- `CostoTrasladoDTO`
- `PromediosDTO`
- `CamionDTO`
- `TramoDTO`

### En ms-rutas
- `TarifaDTO`
- `CostoTrasladoDTO`
- `TramoDTO`

### En ms-camiones
- `PromediosDTO`
- `CamionDetalleDTO`
- `TramoDTO`

---

## 🚀 Próximos Pasos Recomendados

1. **Compilar y testear** todos los microservicios
   ```bash
   mvn -pl ms-solicitudes,ms-rutas,ms-camiones -am clean package
   ```

2. **Verificar con Docker Compose**
   ```bash
   docker-compose up
   ```

3. **Testear endpoints públicos**
   - `curl http://localhost:8082/rutas/tarifas`
   - `curl http://localhost:8083/camiones/promedios?peso=100&volumen=50`

4. **Testear flujo completo** de creación y finalización de solicitudes

---

## 📚 Referencias Rápidas

- **Base de Datos:** PostgreSQL en `jdbc:postgresql://db:5432/db_logistica`
- **Autenticación:** Keycloak en `http://keycloak:8080/realms/tpi-backend`
- **Gateway:** http://localhost:8080 (puerto público)
- **Configuración Docker:** Ver `docker-compose.yml`
