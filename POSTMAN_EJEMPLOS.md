# Ejemplos de Uso - Colección Postman TPI Logística

## 📌 Escenario 1: Prueba Rápida del Flujo Completo

**Objetivo**: Validar que toda la arquitectura funciona correctamente

### Paso a Paso:

1. **Abre Postman** y selecciona el entorno **"TPI Logística - Entorno"**

2. **Ejecuta Autenticación** (3 requests):
   ```
   1. Autenticación > Obtener Token CLIENTE
   1. Autenticación > Obtener Token OPERADOR
   1. Autenticación > Obtener Token TRANSPORTISTA
   ```
   - Verifica que todos muestren `access_token` en verde ✓

3. **Ejecuta el Flujo Completo** (6 requests en orden):
   ```
   2. Flujo Principal > (CLIENTE) Crear Solicitud
   2. Flujo Principal > (OPERADOR) Crear Ruta
   2. Flujo Principal > (OPERADOR) Asignar Camión a Tramo
   2. Flujo Principal > (TRANSPORTISTA) Iniciar Tramo
   2. Flujo Principal > (TRANSPORTISTA) Finalizar Tramo
   2. Flujo Principal > (OPERADOR) Finalizar Solicitud
   ```

4. **Verifica los resultados**:
   - Cada request debe devolver HTTP 200 o 201
   - Los test scripts deben mostrar "Passed" en verde

---

## 📌 Escenario 2: Crear Múltiples Solicitudes

**Objetivo**: Simular múltiples clientes creando solicitudes simultáneamente

### Pasos:

1. Obtén el token del CLIENTE (si no lo tienes)

2. Ejecuta **"3. CRUD - ms-solicitudes > Crear Solicitud"** varias veces:
   - Cada ejecución crea una nueva solicitud
   - Modifica el `clienteDni` en el body de cada una para que sean diferentes

3. Luego ejecuta **"3. CRUD - ms-solicitudes > Listar Solicitudes"** (con rol OPERADOR) para ver todas

### Ejemplo de DNIs diferentes:

```json
// Primera solicitud
{
  "clienteDni": "12345678",
  ...
}

// Segunda solicitud
{
  "clienteDni": "87654321",
  ...
}

// Tercera solicitud
{
  "clienteDni": "11223344",
  ...
}
```

---

## 📌 Escenario 3: Búsqueda de Camiones Aptos

**Objetivo**: Validar que el sistema encuentra camiones que cumplan con peso y volumen

### Pasos:

1. Obtén token OPERADOR

2. Ejecuta **"5. CRUD - ms-camiones > Crear Camión"** al menos dos veces con diferentes capacidades:

```json
// Camión 1: Pequeño
{
  "patente": "ABC123",
  "modelo": "Ford Transit",
  "capacidadPeso": 3500,
  "capacidadVolumen": 10,
  "disponible": true
}

// Camión 2: Grande
{
  "patente": "XYZ789",
  "modelo": "Volvo FH16",
  "capacidadPeso": 25000,
  "capacidadVolumen": 80,
  "disponible": true
}
```

3. Ejecuta **"5. CRUD - ms-camiones > Obtener Camiones Aptos"** con diferentes parámetros:

```
Búsqueda 1: peso=500 & volumen=1000
→ Debería encontrar ambos camiones

Búsqueda 2: peso=5000 & volumen=5000
→ Debería encontrar solo el camión grande (XYZ789)

Búsqueda 3: peso=30000 & volumen=100
→ No debería encontrar ninguno (excedementes capacidades)
```

---

## 📌 Escenario 4: Gestión de Rutas con Múltiples Tramos

**Objetivo**: Crear una ruta compleja con varios tramos

### Pasos:

1. Obtén token OPERADOR y CLIENTE

2. Crea una solicitud con **"(CLIENTE) Crear Solicitud"** y anota el ID

3. Crea una ruta con múltiples tramos usando **"4. CRUD - ms-rutas > Crear Ruta"**:

```json
{
  "idSolicitud": 1,
  "tramos": [
    {
      "idDepositoOrigen": 1,
      "idDepositoDestino": 2,
      "idTipoTramo": 1
    },
    {
      "idDepositoOrigen": 2,
      "idDepositoDestino": 3,
      "idTipoTramo": 2
    },
    {
      "idDepositoOrigen": 3,
      "idDepositoDestino": 4,
      "idTipoTramo": 1
    }
  ]
}
```

4. Verifica la ruta creada con **"4. CRUD - ms-rutas > Obtener Rutas por Solicitud"**

5. Consulta el costo con **"4. CRUD - ms-rutas > Obtener Costo de Traslado Real"**

---

## 📌 Escenario 5: Simulación de Ciclo de Vida Completo

**Objetivo**: Simular el ciclo completo de una solicitud desde creación hasta finalización

### Cronología de Eventos:

```
T1: CLIENTE crea solicitud
T2: OPERADOR crea ruta con tramos
T3: OPERADOR asigna camión a tramo
T4: TRANSPORTISTA inicia tramo
T5: TRANSPORTISTA finaliza tramo
T6: OPERADOR finaliza solicitud
```

### Implementación con Postman:

1. **T1**: Ejecuta **(CLIENTE) Crear Solicitud**
   - Guarda el ID devuelto

2. **T2**: Ejecuta **(OPERADOR) Crear Ruta**
   - Usa la ID de la solicitud
   - Guarda el ID de la ruta y tramo

3. **T3**: Ejecuta **(OPERADOR) Asignar Camión a Tramo**
   - Usa el ID del tramo
   - Especifica la patente del camión

4. **T4**: Ejecuta **(TRANSPORTISTA) Iniciar Tramo**
   - Usa el ID del tramo

5. **T5**: Ejecuta **(TRANSPORTISTA) Finalizar Tramo**
   - Usa el ID del tramo

6. **T6**: Ejecuta **(OPERADOR) Finalizar Solicitud**
   - Usa el ID de la solicitud

7. **Validación**: Ejecuta **"3. CRUD - ms-solicitudes > Obtener Estado de Solicitud"**
   - El estado debe ser "FINALIZADA"

---

## 📌 Escenario 6: Pruebas de Autorización por Rol

**Objetivo**: Validar que los permisos por rol funcionan correctamente

### Intentos que DEBEN fallar (403 Forbidden):

1. **CLIENTE intenta crear ruta**:
   - Usa token_cliente en **"4. CRUD - ms-rutas > Crear Ruta"**
   - Resultado esperado: 403 Forbidden ✓

2. **TRANSPORTISTA intenta listar solicitudes**:
   - Usa token_transportista en **"3. CRUD - ms-solicitudes > Listar Solicitudes"**
   - Resultado esperado: 403 Forbidden ✓

3. **CLIENTE intenta asignar camión**:
   - Usa token_cliente en **"4. CRUD - ms-rutas > Asignar Camión a Tramo"**
   - Resultado esperado: 403 Forbidden ✓

### Intentos que DEBEN funcionar (200 OK):

1. **CLIENTE crea solicitud**: 200 OK ✓
2. **OPERADOR lista solicitudes**: 200 OK ✓
3. **TRANSPORTISTA inicia tramo**: 200 OK ✓

---

## 📌 Escenario 7: Endpoints Públicos (Sin Autenticación)

**Objetivo**: Validar que los endpoints públicos funcionan sin token

### Requests sin autenticación:

1. **"4. CRUD - ms-rutas > Obtener Tarifas Vigentes"**
   - No requiere token
   - Devuelve tarifas vigentes

2. **"4. CRUD - ms-rutas > Obtener Costo de Traslado Real"**
   - No requiere autenticación
   - Devuelve costo real para una solicitud

3. **"4. CRUD - ms-rutas > Obtener Tramos por Patente (públicos)"**
   - Acceso público
   - Devuelve tramos asignados a una patente

### Uso:

- En la pestaña **Authorization** de estos requests, verás **"Type: No Auth"**
- Ejecuta sin necesidad de token previo

---

## 🔄 Automatización: Ejecutar Toda una Carpeta

**Objetivo**: Ejecutar todos los requests de un componente automáticamente

### Pasos:

1. Haz clic derecho en una carpeta (ej. "2. Flujo Principal")

2. Selecciona **"Run Folder"** o **"Run Collection"** (según versión de Postman)

3. Se abrirá el **Collection Runner**

4. Configura:
   - **Environment**: Selecciona "TPI Logística - Entorno"
   - **Iterations**: 1
   - **Delay**: 1000ms (para dar tiempo a procesar)

5. Haz clic en **"Run"**

6. Postman ejecutará todos los requests en orden y mostrará un resumen con:
   - ✓ Requests pasados
   - ✗ Requests fallidos
   - Tiempos de respuesta
   - Variables capturadas

---

## 📊 Monitoreo de Respuestas

### Ver detalles de una respuesta:

1. Después de ejecutar un request, Postman muestra 4 pestañas:

   | Pestaña | Contenido |
   |---------|-----------|
   | **Body** | JSON de la respuesta |
   | **Headers** | Headers HTTP |
   | **Cookies** | Cookies recibidas |
   | **Tests** | Resultados de los test scripts |

2. **Ejemplo de visualización**:
   ```json
   {
     "id": 123,
     "estado": "CREADA",
     "clienteDni": "12345678",
     "fechaCreacion": "2025-11-17T18:00:00Z"
   }
   ```

### Usar Postman Visualizer (Avanzado):

En la pestaña **Tests**, puedes agregar un script para visualizar datos:

```javascript
pm.visualizer.set(`
  <h1>Solicitud Creada</h1>
  <p>ID: {{response.id}}</p>
  <p>Estado: {{response.estado}}</p>
  <p>Cliente: {{response.clienteDni}}</p>
`);
```

---

## 🚨 Casos de Error Comunes

### Error: "Variable {{token_operador}} is not set"

**Causa**: No ejecutaste el request de autenticación del OPERADOR

**Solución**:
1. Ve a **1. Autenticación > Obtener Token OPERADOR**
2. Ejecuta el request
3. Espera a que termine y muestre el token
4. Intenta el request nuevamente

---

### Error: "404 Not Found"

**Causa**: La ID del recurso no existe

**Solución**:
1. Verifica que ejecutaste el request anterior que crea el recurso
2. Comprueba que el ID se capturó correctamente en la variable
3. Abre el **Console** (Postman) con `Ctrl+Alt+C` para ver los logs

---

### Error: "400 Bad Request"

**Causa**: Body JSON inválido

**Solución**:
1. Abre la pestaña **Body** del request
2. Valida que el JSON sea correcto (usa un validador online si es necesario)
3. Asegúrate de que todas las comillas estén correctas
4. Verifica que los tipos de datos sean correctos (números sin comillas, strings con comillas)

---

## 💾 Exportar Resultados

### Exportar un request como cURL:

1. Haz clic en el botón **Code** (parte derecha)
2. Selecciona el lenguaje (cURL, Python, JavaScript, etc.)
3. Copia el código

### Exportar un reporte de pruebas:

1. Ejecuta una carpeta con **Collection Runner**
2. Al terminar, haz clic en **Export** (esquina superior derecha)
3. Descarga el reporte en JSON o HTML

---

## 🎯 Checklist de Validación Completa

Usa este checklist para validar que todo funciona:

- [ ] Tokens obtenidos para los 3 roles
- [ ] Solicitud creada por CLIENTE
- [ ] Ruta creada por OPERADOR
- [ ] Camión asignado a tramo
- [ ] Tramo iniciado por TRANSPORTISTA
- [ ] Tramo finalizado por TRANSPORTISTA
- [ ] Solicitud finalizada por OPERADOR
- [ ] Roles validados (intentos de acceso denegado funcionan)
- [ ] Endpoints públicos accesibles sin token
- [ ] Variables de encadenamiento capturadas correctamente

---

**Versión**: 1.0  
**Última Actualización**: 17/11/2025
