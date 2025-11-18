# 🛠️ Troubleshooting y Tips de QA - Colección Postman TPI Logística

---

## 🔧 Troubleshooting General

### ❌ Error: "{{variable}} is not defined"

**Síntomas:**
- Postman muestra un error de variable no definida
- El request falla porque una variable está vacía

**Causas Posibles:**
1. No ejecutaste el request que captura la variable
2. El script de test no se ejecutó correctamente
3. El entorno no está seleccionado

**Solución:**

```javascript
// 1. Verifica que estés usando el entorno correcto
// (esquina superior derecha: "TPI Logística - Entorno")

// 2. Ejecuta los requests que capturan variables
1. Autenticación > Obtener Token CLIENTE
1. Autenticación > Obtener Token OPERADOR
1. Autenticación > Obtener Token TRANSPORTISTA

// 3. Abre la consola (Ctrl+Alt+C) para ver logs
console.log("Token capturado:", pm.environment.get("token_operador"));
```

---

### ❌ Error: "401 Unauthorized"

**Síntomas:**
- Postman devuelve HTTP 401
- Mensaje: "Invalid token" o "Unauthorized"

**Causas Posibles:**
1. Token expirado (vencimiento: típicamente 5-15 minutos)
2. Token incorrecto para el rol
3. Usuario no tiene permisos

**Solución:**

```bash
# Paso 1: Obtén nuevos tokens
1. Autenticación > Obtener Token {CLIENTE/OPERADOR/TRANSPORTISTA}

# Paso 2: Verifica que la respuesta sea 200 OK
# Busca "access_token" en el body

# Paso 3: Si falla, revisa Keycloak
curl -v http://localhost:8180/realms/tpi-backend/.well-known/openid-configuration
```

---

### ❌ Error: "403 Forbidden"

**Síntomas:**
- Postman devuelve HTTP 403
- El usuario NO tiene permisos para ejecutar la operación

**Causas Posibles:**
1. Usuario no tiene el rol requerido
2. Estás usando el token del rol equivocado
3. El usuario no está asignado al rol en Keycloak

**Solución:**

```javascript
// Verifica que usas el token correcto:
// - {{token_cliente}} para endpoints de CLIENTE
// - {{token_operador}} para endpoints de OPERADOR
// - {{token_transportista}} para endpoints de TRANSPORTISTA

// En Keycloak, verifica:
// 1. El usuario existe
// 2. El usuario tiene el rol asignado
// 3. El rol tiene los permisos necesarios
```

---

### ❌ Error: "404 Not Found"

**Síntomas:**
- Postman devuelve HTTP 404
- Recurso no encontrado

**Causas Posibles:**
1. La ID del recurso no existe
2. El recurso fue eliminado
3. Ejecutaste los requests en orden incorrecto

**Solución:**

```javascript
// Verifica que ejecutaste los requests previos:
1. Crear Solicitud → obtén id_solicitud_creada
2. Crear Ruta → usa id_solicitud_creada
3. Acceder a Ruta → usa id_ruta_creada

// Abre la consola para ver las IDs capturadas:
console.log("ID Solicitud:", pm.collectionVariables.get("id_solicitud_creada"));
console.log("ID Ruta:", pm.collectionVariables.get("id_ruta_creada"));
```

---

### ❌ Error: "400 Bad Request"

**Síntomas:**
- Postman devuelve HTTP 400
- Mensaje: "Invalid request" o "Validation failed"

**Causas Posibles:**
1. JSON inválido en el body
2. Tipos de datos incorrectos
3. Campos requeridos faltantes

**Solución:**

```javascript
// 1. Valida el JSON
// Abre https://jsonlint.com/ y pega tu JSON

// 2. Verifica que:
// - Strings están entre comillas: "clienteDni": "12345678"
// - Números SIN comillas: "pesoContenedor": 500
// - Booleanos SIN comillas: "disponible": true

// 3. Verifica que no haya variables vacías
// {{id_solicitud_creada}} debe tener un valor numérico

// Ejemplo CORRECTO:
{
  "clienteDni": "12345678",
  "pesoContenedor": 500,
  "volumenContenedor": 1000,
  "origenLatitud": -31.4201,
  "origenLongitud": -64.1888,
  "destinoLatitud": -31.5350,
  "destinoLongitud": -64.2637
}

// Ejemplo INCORRECTO (verás 400):
{
  "clienteDni": 12345678,              // ❌ Sin comillas
  "pesoContenedor": "500",              // ❌ String en lugar de número
  "origenLatitud": -31.4201,
  "origenLongitud": -64.1888
  // ❌ Falta destinoLatitud y destinoLongitud
}
```

---

### ❌ Error: "500 Internal Server Error"

**Síntomas:**
- Postman devuelve HTTP 500
- Error en el servidor, no en la solicitud

**Causas Posibles:**
1. Error no controlado en el microservicio
2. Base de datos inaccesible
3. Dependencia (otro microservicio) está caída

**Solución:**

```bash
# 1. Verifica que todos los servicios están corriendo
docker ps
# Debería mostrar: ms-gateway, ms-solicitudes, ms-rutas, ms-camiones, postgres, keycloak

# 2. Revisa los logs del servicio que falló
docker logs ms-solicitudes
docker logs ms-rutas
docker logs ms-camiones

# 3. Intenta nuevamente después de verificar los logs
```

---

### ❌ Error: "502 Bad Gateway"

**Síntomas:**
- Postman devuelve HTTP 502
- El API Gateway no puede conectar con los microservicios

**Causas Posibles:**
1. El microservicio está caído
2. La red entre API Gateway y microservicio está rota
3. Mala configuración del enrutamiento

**Solución:**

```bash
# 1. Verifica que el API Gateway está corriendo
curl http://localhost:8080/api

# 2. Verifica que los microservicios están corriendo
curl http://localhost:8081  # ms-solicitudes
curl http://localhost:8082  # ms-rutas
curl http://localhost:8083  # ms-camiones

# 3. Reinicia el API Gateway
docker restart ms-gateway

# 4. Si persiste, reinicia todos los servicios
docker-compose down
docker-compose up -d
```

---

## 🔍 Debug Avanzado

### 📋 Ver Variables de Entorno en Tiempo Real

```javascript
// Agrega esto en cualquier Test Script para inspeccionar variables

// Variables de Entorno
console.log("=== VARIABLES DE ENTORNO ===");
console.log("baseUrl:", pm.environment.get("baseUrl"));
console.log("token_operador:", pm.environment.get("token_operador"));

// Variables de Colección
console.log("=== VARIABLES DE COLECCIÓN ===");
console.log("id_solicitud_creada:", pm.collectionVariables.get("id_solicitud_creada"));
console.log("id_ruta_creada:", pm.collectionVariables.get("id_ruta_creada"));

// Respuesta actual
console.log("=== RESPUESTA ACTUAL ===");
console.log(pm.response.json());
```

### 📊 Inspeccionar Headers

```javascript
// En la pestaña Test de cualquier request

// Ver todos los headers de la respuesta
console.log("=== HEADERS ===");
var headers = pm.response.headers;
headers.members.forEach(function(header) {
    console.log(header.key + ":", header.value);
});

// Buscar un header específico
var contentType = pm.response.headers.get("Content-Type");
console.log("Content-Type:", contentType);
```

### 🔐 Inspeccionar JWT

```javascript
// Decodifica el JWT para ver los claims

function decodeJWT(token) {
    var parts = token.split('.');
    var payload = parts[1];
    var decodedPayload = atob(payload);
    return JSON.parse(decodedPayload);
}

var token = pm.environment.get("token_operador");
var decoded = decodeJWT(token);

console.log("=== JWT DECODED ===");
console.log("User:", decoded.preferred_username);
console.log("Email:", decoded.email);
console.log("Roles:", decoded.realm_access.roles);
console.log("Exp:", new Date(decoded.exp * 1000));
```

---

## 📈 Tips de Rendimiento

### ⚡ Optimizar Tiempos de Respuesta

```javascript
// Mide el tiempo de respuesta del request

pm.test("Performance Check", function() {
    var responseTime = pm.response.responseTime;
    
    console.log("Tiempo de respuesta:", responseTime + "ms");
    
    // Alerta si tarda más de 1 segundo
    if (responseTime > 1000) {
        console.warn("⚠️ Request lento:", responseTime + "ms");
    }
});
```

### 📉 Monitorear Paginación

```javascript
// Para requests con paginación, verifica que los datos sean consistentes

pm.test("Pagination Validation", function() {
    var response = pm.response.json();
    
    console.log("Total Elementos:", response.totalElements);
    console.log("Página Actual:", response.number);
    console.log("Tamaño Página:", response.size);
    console.log("Elementos en Página:", response.content.length);
    
    // Valida que no hay datos inconsistentes
    pm.expect(response.content.length).to.be.below(response.size + 1);
});
```

---

## 🔐 Tips de Seguridad

### ✅ Buenas Prácticas

```javascript
// 1. NUNCA hagas commit de tokens reales en Git
// .gitignore debe incluir:
// TPI_Logistica.postman_environment.json (con tokens)
// .env

// 2. Usa variables de entorno para credenciales sensibles
// ❌ MALO:
var username = "cliente@mail.com";
var password = "1234";

// ✅ BUENO:
var username = pm.environment.get("user_cliente");
var password = pm.environment.get("pass_cliente");

// 3. Verifica que los tokens no se expongan en logs
// ❌ MALO:
console.log("Full Response:", pm.response.json());  // Expone token

// ✅ BUENO:
console.log("User:", pm.response.json().preferred_username);  // Seguro
```

### 🛡️ Validar Certificados SSL en Producción

```javascript
// Para pruebas en HTTPS (producción)

pm.test("SSL Certificate Valid", function() {
    // Postman verifica automáticamente SSL
    // Si el certificado es inválido, el request fallará
    pm.expect(pm.response.code).to.not.be.oneOf([0]);
});
```

---

## 📊 Testing Avanzado

### 🧪 Chaining Condicional

```javascript
// Ejecuta una acción solo si la anterior fue exitosa

pm.test("Conditional Chaining", function() {
    if (pm.response.code === 201) {
        var id = pm.response.json().id;
        pm.collectionVariables.set("created_id", id);
        console.log("✓ ID capturado:", id);
    } else {
        console.error("✗ Request falló, no se capturó ID");
    }
});
```

### ⚡ Validación Compleja

```javascript
// Valida múltiples condiciones en una solicitud

pm.test("Complex Validation", function() {
    var response = pm.response.json();
    
    // Valida estructura
    pm.expect(response).to.have.property("id");
    pm.expect(response).to.have.property("estado");
    pm.expect(response.estado).to.be.oneOf(["CREADA", "EN_TRANSITO", "FINALIZADA"]);
    
    // Valida valores numéricos
    pm.expect(response.pesoContenedor).to.be.above(0);
    pm.expect(response.volumenContenedor).to.be.above(0);
    
    // Valida fechas
    var fecha = new Date(response.fechaCreacion);
    pm.expect(fecha).to.be.valid;
});
```

---

## 🔄 Flujos de Prueba Especializados

### ✅ Validar Flujo Completo de Solicitud

```javascript
// Test integrado que valida el ciclo completo

// 1. CLIENTE crea solicitud
POST {{baseUrl}}/solicitudes
Test:
pm.collectionVariables.set("id_solicitud", pm.response.json().id);
pm.expect(pm.response.code).to.equal(201);

// 2. OPERADOR crea ruta
POST {{baseUrl}}/rutas
Body: {"idSolicitud": {{id_solicitud}}, ...}
Test:
pm.collectionVariables.set("id_ruta", pm.response.json().id);
pm.expect(pm.response.code).to.equal(201);

// 3. Verificar que ruta está asociada
GET {{baseUrl}}/rutas/solicitud/{{id_solicitud}}
Test:
pm.expect(pm.response.json().length).to.be.above(0);
pm.expect(pm.response.json()[0].id).to.equal(pm.collectionVariables.get("id_ruta"));
```

---

## 📝 Logging y Reportes

### 📋 Exportar Resultados

```bash
# Ejecutar colección completa y guardar reporte
1. Haz clic en "Run" en cualquier carpeta
2. Se abre el Collection Runner
3. Ejecuta los requests
4. Haz clic en "Export Results" → Descarga JSON

# Ver el reporte:
- Abre el JSON en un editor
- O cópialo en https://www.jsoncrack.com/ para visualizar
```

### 📊 Crear Dashboards Personalizados

```javascript
// En el Test Script, usa Postman Visualizer

pm.visualizer.set(`
  <div style="padding: 20px; font-family: Arial;">
    <h1>Resumen de Solicitud</h1>
    <table border="1" cellpadding="10">
      <tr><td><b>ID</b></td><td>{{response.id}}</td></tr>
      <tr><td><b>Estado</b></td><td>{{response.estado}}</td></tr>
      <tr><td><b>Peso</b></td><td>{{response.pesoContenedor}} kg</td></tr>
      <tr><td><b>Volumen</b></td><td>{{response.volumenContenedor}} L</td></tr>
    </table>
  </div>
`);
```

---

## 🎯 Checklist de QA Completo

### ✓ Pre-Ejecución

- [ ] Todos los servicios están corriendo (`docker ps`)
- [ ] Postman tiene los 2 archivos JSON importados
- [ ] El entorno correcto está seleccionado
- [ ] Las credenciales en el entorno son correctas
- [ ] La base de datos está limpia o reseteda

### ✓ Durante Ejecución

- [ ] Obtén los 3 tokens de autenticación
- [ ] Ejecuta el flujo principal en orden
- [ ] Verifica que cada request devuelva el código HTTP esperado
- [ ] Comprueba que las variables se capturan correctamente
- [ ] Los test scripts pasan (verde ✓)

### ✓ Post-Ejecución

- [ ] La solicitud se creó exitosamente
- [ ] La ruta se creó con los tramos correctos
- [ ] El camión fue asignado al tramo
- [ ] El tramo fue iniciado y finalizado
- [ ] La solicitud está marcada como finalizada
- [ ] No hay errores en los logs de los microservicios

---

## 📞 Contacto Rápido

| Problema | Acción |
|----------|--------|
| Token expirado | Re-ejecuta autenticación |
| Variable no definida | Ejecuta request anterior |
| Servicio caído | `docker-compose restart` |
| JSON inválido | Valida en jsonlint.com |
| Tests fallan | Abre "Test Results" y lee el error |

---

**Versión**: 1.0  
**Última Actualización**: 17/11/2025  
**Autor**: QA Senior - Sistema TPI Logística
