# ⚡ Quick Reference - Colección Postman TPI Logística

> Referencia rápida para usuarios experimentados. Para guía completa, ver `POSTMAN_GUIA.md`

---

## 🚀 Inicio en 3 Pasos

```bash
# 1. Importar archivos JSON a Postman
Import → Seleccionar:
  - TPI_Logistica.postman_collection.json
  - TPI_Logistica.postman_environment.json

# 2. Obtener Tokens (ejecutar en orden)
1. Autenticación > Obtener Token CLIENTE
1. Autenticación > Obtener Token OPERADOR
1. Autenticación > Obtener Token TRANSPORTISTA

# 3. Ejecutar Flujo Completo
2. Flujo Principal > 6 requests en orden
```

---

## 📍 URLs Base

| Servicio | URL |
|----------|-----|
| API Gateway | `http://localhost:8080/api` |
| Keycloak | `http://localhost:8180` |
| ms-solicitudes | `http://localhost:8081` |
| ms-rutas | `http://localhost:8082` |
| ms-camiones | `http://localhost:8083` |

---

## 🔐 Credenciales (Por Defecto)

| Rol | Usuario | Contraseña |
|-----|---------|-----------|
| CLIENTE | `cliente@mail.com` | `1234` |
| OPERADOR | `operador@mail.com` | `1234` |
| TRANSPORTISTA | `transportista@mail.com` | `1234` |

---

## 📋 Estructura de Carpetas

```
1. Autenticación (Keycloak) ..................... 3 requests
   ├── Obtener Token CLIENTE
   ├── Obtener Token OPERADOR
   └── Obtener Token TRANSPORTISTA

2. Flujo Principal (Demo) ....................... 6 requests
   ├── (CLIENTE) Crear Solicitud
   ├── (OPERADOR) Crear Ruta
   ├── (OPERADOR) Asignar Camión a Tramo
   ├── (TRANSPORTISTA) Iniciar Tramo
   ├── (TRANSPORTISTA) Finalizar Tramo
   └── (OPERADOR) Finalizar Solicitud

3. CRUD - ms-solicitudes ....................... 7 requests
4. CRUD - ms-rutas ............................. 12 requests
5. CRUD - ms-camiones .......................... 4 requests

TOTAL: 46 requests
```

---

## 🔑 Variables de Entorno

```
baseUrl=http://localhost:8080/api
keycloakUrl=http://localhost:8180
keycloakRealm=tpi-backend
keycloakClient=tpi-backend-client

user_cliente=cliente@mail.com
pass_cliente=1234
user_operador=operador@mail.com
pass_operador=1234
user_transportista=transportista@mail.com
pass_transportista=1234

token_cliente=                    # Auto-llena tras autenticación
token_operador=                   # Auto-llena tras autenticación
token_transportista=              # Auto-llena tras autenticación
```

---

## 🔄 Variables de Colección (Encadenamiento)

```
id_solicitud_creada    = ID capturado de: POST /solicitudes
id_ruta_creada         = ID capturado de: POST /rutas
id_tramo_1             = ID capturado de: POST /rutas → tramos[0]
```

---

## 🎯 Endpoints Principales

### Solicitudes (ms-solicitudes)

| Método | Endpoint | Rol | Descripción |
|--------|----------|-----|-------------|
| POST | `/solicitudes` | CLIENTE | Crear solicitud |
| GET | `/solicitudes` | OPERADOR | Listar todas |
| GET | `/solicitudes/{id}` | CLIENTE/OPERADOR | Ver una |
| GET | `/solicitudes/{id}/estado` | CLIENTE/OPERADOR | Ver estado |
| PUT | `/solicitudes/{id}/finalizar` | OPERADOR | Finalizar |

### Rutas (ms-rutas)

| Método | Endpoint | Rol | Descripción |
|--------|----------|-----|-------------|
| POST | `/rutas` | OPERADOR | Crear ruta |
| GET | `/rutas` | OPERADOR | Listar rutas |
| GET | `/rutas/{id}` | OPERADOR | Ver ruta |
| GET | `/rutas/solicitud/{id}` | OPERADOR | Rutas de solicitud |
| PUT | `/tramos/{id}/asignar-camion` | OPERADOR | Asignar camión |
| POST | `/tramos/{id}/iniciar` | TRANSPORTISTA | Iniciar tramo |
| POST | `/tramos/{id}/finalizar` | TRANSPORTISTA | Finalizar tramo |
| GET | `/rutas/tarifas` | Público | Ver tarifas |

### Camiones (ms-camiones)

| Método | Endpoint | Rol | Descripción |
|--------|----------|-----|-------------|
| POST | `/camiones` | OPERADOR | Crear camión |
| GET | `/camiones` | OPERADOR | Listar camiones |
| GET | `/camiones/buscar-apto?peso=X&volumen=Y` | OPERADOR | Buscar aptos |
| GET | `/camiones/transportistas/me/tramos` | TRANSPORTISTA | Mis tramos |

---

## 📊 DTOs Rápidos

### SolicitudRequestDTO
```json
{
  "clienteDni": "12345678",
  "pesoContenedor": 500,
  "volumenContenedor": 1000,
  "origenLatitud": -31.4201,
  "origenLongitud": -64.1888,
  "destinoLatitud": -31.5350,
  "destinoLongitud": -64.2637
}
```

### CrearRutaRequest
```json
{
  "idSolicitud": 1,
  "tramos": [
    {
      "idDepositoOrigen": 1,
      "idDepositoDestino": 2,
      "idTipoTramo": 1
    }
  ]
}
```

### AsignarCamionRequest
```json
{
  "patenteCamion": "ABC123",
  "pesoContenedor": 500,
  "volumenContenedor": 1000
}
```

---

## 🔴 Códigos de Error Comunes

| Código | Causa | Solución |
|--------|-------|----------|
| 401 | Token inválido/expirado | Re-ejecuta autenticación |
| 403 | Sin permisos para el rol | Usa el token correcto |
| 404 | Recurso no encontrado | Ejecuta requests previos |
| 400 | Body JSON inválido | Valida JSON en jsonlint.com |
| 500 | Error en servidor | Revisa logs: `docker logs <servicio>` |
| 502 | Bad Gateway (servicio caído) | `docker-compose restart` |

---

## 💡 Tips Útiles

### Ejecutar Carpeta Completa
```
Clic derecho en carpeta → Run Folder → Collection Runner
```

### Inspeccionar Variables
```javascript
// En Test Script:
console.log("Token:", pm.environment.get("token_operador"));
console.log("ID:", pm.collectionVariables.get("id_solicitud_creada"));
```

### Ver Logs Completos
```
Postman: Ctrl+Alt+C → Console → Ver todos los logs
```

### Exportar como cURL
```
Request → Code (esquina superior derecha) → cURL → Copy
```

---

## 🧪 Test Scripts Importantes

### Capturar Token
```javascript
var jsonData = pm.response.json();
pm.environment.set("token_operador", jsonData.access_token);
```

### Capturar ID
```javascript
var jsonData = pm.response.json();
pm.collectionVariables.set("id_solicitud_creada", jsonData.id);
```

### Validar Respuesta
```javascript
pm.test("Test Name", function() {
    pm.expect(pm.response.code).to.equal(200);
    pm.expect(pm.response.json().id).to.exist;
});
```

---

## 🔄 Flujo Típico (7 Pasos)

```
1. POST /solicitudes (CLIENTE)
   → Respuesta: {"id": 1, "estado": "CREADA"}
   → Captura: id_solicitud_creada = 1

2. POST /rutas (OPERADOR, usa id_solicitud_creada)
   → Respuesta: {"id": 42, "tramos": [{id: 77}]}
   → Captura: id_ruta_creada = 42, id_tramo_1 = 77

3. PUT /tramos/{id}/asignar-camion (OPERADOR, usa id_tramo_1)
   → Respuesta: {"id": 77, "estado": "ASIGNADO"}

4. POST /tramos/{id}/iniciar (TRANSPORTISTA, usa id_tramo_1)
   → Respuesta: {"id": 77, "estado": "EN_TRANSITO"}

5. POST /tramos/{id}/finalizar (TRANSPORTISTA, usa id_tramo_1)
   → Respuesta: {"id": 77, "estado": "FINALIZADO"}

6. PUT /solicitudes/{id}/finalizar (OPERADOR, usa id_solicitud_creada)
   → Respuesta: {"id": 1, "estado": "FINALIZADA"}

7. GET /solicitudes/{id}/estado (cualquiera, usa id_solicitud_creada)
   → Respuesta: {"estado": "FINALIZADA", ...}
```

---

## 🚦 Checklist Rápido

- [ ] ¿Están corriendo todos los servicios? `docker ps`
- [ ] ¿Están importados los archivos JSON?
- [ ] ¿Está seleccionado el entorno correcto?
- [ ] ¿Obtuviste los 3 tokens? (Test Results = verde)
- [ ] ¿Flujo principal ejecutado? (6 requests sin errores)
- [ ] ¿Variables capturadas? (abre Entorno y Colección)

---

## 🔗 Links Útiles

- **Keycloak Admin**: `http://localhost:8180/admin`
- **API Gateway Docs**: `http://localhost:8080/api/swagger-ui.html` (si aplica)
- **Validador JSON**: `https://jsonlint.com`
- **Docs Postman**: `https://learning.postman.com`

---

## 🆘 SOS Rápido

| Problema | Acción |
|----------|--------|
| No hay tokens | `1. Autenticación > Obtener Token CLIENTE` |
| Variable vacía | Abre Test Results del request que captura |
| 404 Not Found | Ejecuta el request anterior que crea el recurso |
| JSON inválido | Valida en jsonlint.com |
| Servicio caído | `docker logs <servicio>` |

---

## 📝 Notas Rápidas

**Ambientes Disponibles** (en `POSTMAN_ENV_CONFIG.json`):
- Desarrollo Local
- Docker Compose
- Staging
- Producción

**Más Documentación**:
- `POSTMAN_GUIA.md` - Guía completa
- `POSTMAN_EJEMPLOS.md` - 7 escenarios
- `POSTMAN_README.md` - Overview
- `POSTMAN_TROUBLESHOOTING.md` - Problemas y soluciones

---

**Versión**: 1.0 | **Última Actualización**: 17/11/2025
