-- ...existing code...
-- Script robusto para insertar datos iniciales detectando columnas dinámicamente.
-- Inserciones directas (estas ya funcionaban)
INSERT INTO estado_tramo (id_estado_tramo, nombre) VALUES
 (1, 'ESTIMADO'), (2, 'ASIGNADO'), (3, 'INICIADO'), (4, 'FINALIZADO')
ON CONFLICT (id_estado_tramo) DO NOTHING;

-- Camiones de ejemplo (ajustar columnas si su esquema difiere)
INSERT INTO camiones (patente, nombre_transportista, capacidad_peso, capacidad_volumen, consumo_combustible_km, costo_por_km, disponibilidad)
VALUES ('ABC123', 'Juan Pérez', 25000, 80, 8.5, 150.00, true)
ON CONFLICT DO NOTHING;

-- Clientes de ejemplo
INSERT INTO clientes (dni, nombre, apellido, email, telefono)
VALUES ('12345678','Cliente','Test','cliente@test.com','+54 9 351 1111111')
ON CONFLICT DO NOTHING;

-- --------------------------------------------------------------------------------
-- Tipo de tramo: detecta columna de texto y hace insert según nombre real
-- --------------------------------------------------------------------------------
DO $$
DECLARE
  txt_col text;
  id_col text;
BEGIN
  IF NOT EXISTS (SELECT 1 FROM information_schema.tables WHERE table_name = 'tipo_tramo') THEN
    RAISE NOTICE 'Table tipo_tramo not found, skipping tipo_tramo inserts';
    RETURN;
  END IF;

  -- buscar columna id típica
  SELECT column_name INTO id_col
    FROM information_schema.columns
   WHERE table_name='tipo_tramo' AND (column_name ILIKE 'id_%' OR column_name = 'id')
   ORDER BY ordinal_position LIMIT 1;

  -- buscar una columna de texto para el nombre/descripcion
  SELECT column_name INTO txt_col
    FROM information_schema.columns
   WHERE table_name='tipo_tramo' AND data_type IN ('character varying','text')
     AND column_name NOT ILIKE '%id%'
   ORDER BY ordinal_position LIMIT 1;

  IF txt_col IS NULL THEN
    RAISE NOTICE 'No text column found in tipo_tramo, skipping inserts';
    RETURN;
  END IF;

  -- usar id_col si existe, sino insertar solo la columna texto (sin id)
  IF id_col IS NOT NULL THEN
    EXECUTE format($f$
      INSERT INTO tipo_tramo (%I, %I) VALUES
        (1, %L), (2, %L), (3, %L), (4, %L)
      ON CONFLICT (%I) DO NOTHING
    $f$, id_col, txt_col, 'ESTIMADO','ASIGNADO','INICIADO','FINALIZADO', id_col);
  ELSE
    EXECUTE format($f$
      INSERT INTO tipo_tramo (%I) VALUES
        (%L), (%L), (%L), (%L)
    $f$, txt_col, 'ESTIMADO','ASIGNADO','INICIADO','FINALIZADO');
  END IF;
END$$;

-- --------------------------------------------------------------------------------
-- Depositos: detectar columnas (id, nombre/text, lat, lon, precio/costo)
-- --------------------------------------------------------------------------------
DO $$
DECLARE
  t text := 'depositos';
  idc text;
  namec text;
  latc text;
  lonc text;
  pricec text;
  col record;
BEGIN
  IF NOT EXISTS (SELECT 1 FROM information_schema.tables WHERE table_name = t) THEN
    RAISE NOTICE 'Table % not found, skipping depositos inserts', t;
    RETURN;
  END IF;

  -- id candidate
  SELECT column_name INTO idc FROM information_schema.columns
   WHERE table_name=t AND (column_name ILIKE 'id_%' OR column_name='id')
   ORDER BY ordinal_position LIMIT 1;

  -- name/text candidate
  SELECT column_name INTO namec FROM information_schema.columns
   WHERE table_name=t AND data_type IN ('character varying','text')
     AND column_name NOT ILIKE '%id%' ORDER BY ordinal_position LIMIT 1;

  -- lat / lon candidates
  SELECT column_name INTO latc FROM information_schema.columns
   WHERE table_name=t AND (column_name ILIKE '%lat%' OR column_name ILIKE '%latitude%')
   ORDER BY ordinal_position LIMIT 1;

  SELECT column_name INTO lonc FROM information_schema.columns
   WHERE table_name=t AND (column_name ILIKE '%lon%' OR column_name ILIKE '%lng%' OR column_name ILIKE '%longitude%')
   ORDER BY ordinal_position LIMIT 1;

  -- price candidate: prefer names with estadio/estadia/costo/precio
  SELECT column_name INTO pricec FROM information_schema.columns
   WHERE table_name=t AND data_type IN ('numeric','double precision','real','integer','numeric')
     AND (column_name ILIKE '%estadi%' OR column_name ILIKE '%estadio%' OR column_name ILIKE '%costo%' OR column_name ILIKE '%precio%')
   ORDER BY ordinal_position LIMIT 1;

  -- fallback: any numeric column different from lat/lon/id
  IF pricec IS NULL THEN
    SELECT column_name INTO pricec FROM information_schema.columns
     WHERE table_name=t AND data_type IN ('numeric','double precision','real','integer')
       AND column_name NOT IN (COALESCE(latc,''), COALESCE(lonc,''), COALESCE(idc,''))
     ORDER BY ordinal_position LIMIT 1;
  END IF;

  IF namec IS NULL THEN
    RAISE NOTICE 'No text column found in depositos, skipping deposit insert';
    RETURN;
  END IF;

  -- Construir dynamically la sentencia
  IF idc IS NOT NULL AND latc IS NOT NULL AND lonc IS NOT NULL AND pricec IS NOT NULL THEN
    EXECUTE format($q$
      INSERT INTO depositos (%I, %I, %I, %I, %I)
      VALUES (1, %L, %L, %L, %L)
      ON CONFLICT (%I) DO NOTHING
    $q$, idc, namec, latc, lonc, pricec,
         'Depósito Córdoba', '-31.4201', '-64.1888', '500.00', idc);
  ELSIF latc IS NOT NULL AND lonc IS NOT NULL AND pricec IS NOT NULL THEN
    -- sin id
    EXECUTE format($q$
      INSERT INTO depositos (%I, %I, %I, %I)
      VALUES (%L, %L, %L, %L)
    $q$, namec, latc, lonc, pricec,
         'Depósito Córdoba', '-31.4201', '-64.1888', '500.00');
  ELSE
    RAISE NOTICE 'depositos table present but required columns not found (lat/lon/price). Skipping.';
  END IF;
END$$;

-- --------------------------------------------------------------------------------
-- Tarifas: detectar columnas y hacer INSERT
-- --------------------------------------------------------------------------------
DO $$
DECLARE
  t text := 'tarifas';
  idc text;
  pricec text;
  stayc text;
BEGIN
  IF NOT EXISTS (SELECT 1 FROM information_schema.tables WHERE table_name = t) THEN
    RAISE NOTICE 'Table % not found, skipping tarifas inserts', t;
    RETURN;
  END IF;

  SELECT column_name INTO idc FROM information_schema.columns
   WHERE table_name=t AND (column_name ILIKE 'id_%' OR column_name = 'id')
   ORDER BY ordinal_position LIMIT 1;

  -- columnas numéricas: buscar precio_litro y costo_estadia_diario o parecidas
  SELECT column_name INTO pricec FROM information_schema.columns
   WHERE table_name=t AND data_type IN ('numeric','double precision','real','integer')
     AND (column_name ILIKE '%precio%' OR column_name ILIKE '%litro%' OR column_name ILIKE '%precio_litro%')
   ORDER BY ordinal_position LIMIT 1;

  SELECT column_name INTO stayc FROM information_schema.columns
   WHERE table_name=t AND data_type IN ('numeric','double precision','real','integer')
     AND (column_name ILIKE '%estadi%' OR column_name ILIKE '%estadio%' OR column_name ILIKE '%costo%')
   ORDER BY ordinal_position LIMIT 1;

  -- Fallbacks
  IF pricec IS NULL THEN
    SELECT column_name INTO pricec FROM information_schema.columns
     WHERE table_name=t AND data_type IN ('numeric','double precision','real','integer')
     ORDER BY ordinal_position LIMIT 1;
  END IF;

  IF stayc IS NULL THEN
    -- buscar otra numeric distinta de pricec
    SELECT column_name INTO stayc FROM information_schema.columns
     WHERE table_name=t AND data_type IN ('numeric','double precision','real','integer')
       AND column_name <> COALESCE(pricec,'')
     ORDER BY ordinal_position LIMIT 1;
  END IF;

  IF pricec IS NULL THEN
    RAISE NOTICE 'No numeric column found in tarifas, skipping';
    RETURN;
  END IF;

  IF idc IS NOT NULL AND stayc IS NOT NULL THEN
    EXECUTE format($f$
      INSERT INTO tarifas (%I, %I, %I) VALUES
       (1, %L, %L)
      ON CONFLICT (%I) DO NOTHING
    $f$, idc, pricec, stayc, '150.00', '500.00', idc);
  ELSIF idc IS NOT NULL THEN
    EXECUTE format($f$
      INSERT INTO tarifas (%I, %I) VALUES
       (1, %L)
      ON CONFLICT (%I) DO NOTHING
    $f$, idc, pricec, '150.00', idc);
  ELSE
    EXECUTE format($f$
      INSERT INTO tarifas (%I, %L) VALUES (%L, %L)
    $f$, pricec, '150.00', '150.00', '500.00');
  END IF;
END$$;

-- --------------------------------------------------------------------------------
-- Resumen de verificación (intenta contar si las tablas existen)
-- --------------------------------------------------------------------------------
SELECT 'Verificación de Datos Iniciales:' as info;

SELECT COUNT(*) FILTER (WHERE true) AS total_camiones FROM (
  SELECT 1 FROM pg_class c JOIN pg_namespace n ON n.oid=c.relnamespace
  WHERE c.relname='camiones' LIMIT 1
) s JOIN LATERAL (SELECT 1) t ON true;

-- Conteos seguros (si las tablas existen)
SELECT (CASE WHEN EXISTS (SELECT 1 FROM information_schema.tables WHERE table_name='depositos') THEN
         (SELECT COUNT(*) FROM depositos) ELSE 0 END) AS total_depositos;

SELECT (CASE WHEN EXISTS (SELECT 1 FROM information_schema.tables WHERE table_name='estado_tramo') THEN
         (SELECT COUNT(*) FROM estado_tramo) ELSE 0 END) AS total_estado_tramo;

SELECT (CASE WHEN EXISTS (SELECT 1 FROM information_schema.tables WHERE table_name='tipo_tramo') THEN
         (SELECT COUNT(*) FROM tipo_tramo) ELSE 0 END) AS total_tipo_tramo;

SELECT (CASE WHEN EXISTS (SELECT 1 FROM information_schema.tables WHERE table_name='tarifas') THEN
         (SELECT COUNT(*) FROM tarifas) ELSE 0 END) AS total_tarifas;

-- ...existing code...

-- ==================================================================================
-- INSERTS EXPLÍCIOS (compatibles con el esquema actual detectado en la BD)
-- Estas inserciones usan los nombres de columnas reales: depositos(id_deposito,nombre,calle,id_ciudad,latitud,longitud,precio_estadia),
-- tipo_tramo(id_tipo_tramo, descripcion), tarifas(id_tarifa, precio_litro), camiones(...), clientes(...)
-- ==================================================================================

-- Tipo de tramo (usa columna 'descripcion')
INSERT INTO tipo_tramo (id_tipo_tramo, descripcion) VALUES
  (1, 'TERRESTRE'),
  (2, 'AEREO'),
  (3, 'MARITIMO')
ON CONFLICT (id_tipo_tramo) DO NOTHING;

-- Tarifas (columna precio_litro)
INSERT INTO tarifas (id_tarifa, precio_litro) VALUES
  (1, 150.00)
ON CONFLICT (id_tarifa) DO NOTHING;

-- Depósitos: insertar al menos 3 depósitos (1,2,3) que el flujo de pruebas usa
-- columnas: id_deposito, nombre, calle, id_ciudad, latitud, longitud, precio_estadia
INSERT INTO depositos (id_deposito, nombre, calle, id_ciudad, latitud, longitud, precio_estadia) VALUES
  (1, 'Depósito Córdoba', 'Av. Colón 100', 1, -31.4201, -64.1888, 500.00),
  (2, 'Depósito Mendoza', 'Calle San Martín 200', 2, -32.8908, -68.8272, 450.00),
  (3, 'Depósito Buenos Aires', 'Av. 9 de Julio 300', 3, -34.6037, -58.3816, 600.00)
ON CONFLICT (id_deposito) DO NOTHING;

-- Camiones adicionales (si no están): asegurar disponibilidad de camiones de prueba
INSERT INTO camiones (patente, nombre_transportista, telefono_transportista, capacidad_peso, capacidad_volumen, consumo_combustible_km, costo_por_km, disponibilidad)
VALUES
  ('ABC123', 'Juan Pérez', '+54 9 351 1234567', 25000, 80, 8.5, 150.00, true),
  ('XYZ789', 'Carlos López', '+54 9 351 9876543', 20000, 60, 10.0, 140.00, true),
  ('DEF456', 'María García', '+54 9 351 5555555', 15000, 50, 9.0, 145.00, true)
ON CONFLICT (patente) DO NOTHING;

-- Clientes de prueba (asegurar existencia por dni usado en Postman y pruebas)
INSERT INTO clientes (dni, nombre, apellido, email, telefono) VALUES
  ('12345678','Cliente','Test','cliente@test.com','+54 9 351 1111111'),
  ('87654321','Cliente2','Prueba','cliente2@test.com','+54 9 351 2222222')
ON CONFLICT (dni) DO NOTHING;

-- Contenedores de prueba asociados a clientes (solo si la tabla y columna de FK existen)
DO $$
BEGIN
  IF EXISTS (SELECT 1 FROM information_schema.tables WHERE table_name='contenedores') THEN
    -- intentar insertar un contenedor ligado al cliente con dni 12345678 si existe
    INSERT INTO contenedores (peso, volumen, id_cliente_asociado)
    SELECT 500.0, 50.0, id_cliente FROM clientes WHERE dni='12345678' LIMIT 1
    ON CONFLICT DO NOTHING;
  END IF;
END$$;

-- Fin de inserts explícitos
