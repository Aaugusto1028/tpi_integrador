-- ==================================================================================
-- SCRIPT DE DATOS INICIALES - TPI LOGÍSTICA (VERSIÓN FINAL)
-- ==================================================================================

-- 1. ESTADOS DE TRAMO
INSERT INTO estado_tramo (id_estado_tramo, nombre) VALUES
 (1, 'ESTIMADO'), 
 (2, 'ASIGNADO'), 
 (3, 'INICIADO'), 
 (4, 'FINALIZADO')
ON CONFLICT (id_estado_tramo) DO NOTHING;

-- 2. TIPOS DE TRAMO
INSERT INTO tipo_tramo (id_tipo_tramo, descripcion) VALUES
  (1, 'TERRESTRE'),
  (2, 'AEREO'),
  (3, 'MARITIMO')
ON CONFLICT (id_tipo_tramo) DO NOTHING;

-- 3. TARIFAS BASE
INSERT INTO tarifas (id_tarifa, precio_litro) VALUES
  (1, 150.00)
ON CONFLICT (id_tarifa) DO NOTHING;

-- 4. CLIENTES DE PRUEBA
INSERT INTO clientes (dni, nombre, apellido, email, telefono) VALUES
  ('12345678','Cliente','Test','cliente@test.com','+54 9 351 1111111'),
  ('87654321','Cliente2','Prueba','cliente2@test.com','+54 9 351 2222222')
ON CONFLICT (dni) DO NOTHING;

-- 5. DEPÓSITOS (Red Troncal + Intermedios para evitar tramos > 600km)
INSERT INTO depositos (id_deposito, nombre, calle, id_ciudad, latitud, longitud, precio_estadia) VALUES
  -- Zona Centro / Cuyo
  (1, 'Depósito Córdoba', 'Av. Colón 100', 1, -31.4201, -64.1888, 500.00),
  (2, 'Depósito Mendoza', 'Calle San Martín 200', 2, -32.8908, -68.8272, 450.00),
  (3, 'Depósito Buenos Aires', 'Av. 9 de Julio 300', 3, -34.6037, -58.3816, 600.00),
  (4, 'Depósito La Plata', 'Calle 7 #800', 3, -34.9211, -57.9545, 480.00),
  (13, 'Depósito San Luis', 'Ruta Nacional 7 Km 320', 12, -33.2950, -66.3356, 470.00),
  
  -- Zona Litoral (Críticos para conectar Norte con Centro)
  (5, 'Depósito Rosario', 'Av. Pellegrini 2500', 4, -32.9442, -60.6509, 520.00),
  (6, 'Depósito Santa Fe', 'Ruta Nacional 9 Km 470', 5, -31.6109, -60.7084, 490.00),
  (7, 'Depósito Paraná', 'Av. Ramírez 1200', 6, -31.7330, -60.5247, 475.00),
  (21, 'Depósito Corrientes', 'Av. Tres de Abril 850', 20, -27.4814, -58.8221, 495.00),
  (22, 'Depósito Entre Ríos', 'Calle Urquiza 1100', 6, -32.3881, -60.6754, 485.00),
  (20, 'Depósito Misiones', 'Ruta Nacional 12 Km 2340', 19, -27.4305, -55.5019, 510.00),

  -- Zona Norte
  (8, 'Depósito Salta', 'Ruta Nacional 9 Km 1234', 7, -24.7859, -65.4107, 550.00),
  (9, 'Depósito Jujuy', 'Calle Gorriti 450', 8, -24.1856, -65.2995, 530.00),
  (10, 'Depósito Tucumán', 'Av. Aconquija 3000', 9, -26.8083, -65.2176, 510.00),
  (11, 'Depósito La Rioja', 'Calle Pelagio Luna 800', 10, -29.4119, -66.8654, 450.00),
  (12, 'Depósito Catamarca', 'Av. Virgen del Valle 1500', 11, -28.4667, -65.4833, 480.00),
  (19, 'Depósito Formosa', 'Ruta Nacional 9 Km 1850', 18, -25.5606, -60.9744, 500.00),
  (23, 'Depósito Santiago del Estero', 'Av. Libertad 2200', 21, -27.7975, -64.2614, 470.00),

  -- Zona Sur (Patagonia)
  (14, 'Depósito Neuquén', 'Av. Olascoaga 2800', 13, -38.9521, -68.0585, 520.00),
  (15, 'Depósito Río Negro', 'Ruta Nacional 5 Km 160', 14, -41.1337, -71.3089, 540.00),
  (16, 'Depósito Chubut', 'Calle Rivadavia 1200', 15, -42.7628, -65.0383, 560.00),
  (17, 'Depósito Santa Cruz', 'Av. Kirchner 3500', 16, -50.3680, -72.5411, 580.00),
  (18, 'Depósito Tierra del Fuego', 'Ruta Nacional 3 Km 20', 17, -54.8019, -68.3304, 600.00)
ON CONFLICT (id_deposito) DO NOTHING;

-- 6. FLOTA DE CAMIONES COMPLETA
INSERT INTO camiones (patente, nombre_transportista, telefono_transportista, capacidad_peso, capacidad_volumen, consumo_combustible_km, costo_por_km, disponibilidad)
VALUES
  -- == FLOTA ESTÁNDAR (Originales) ==
  ('ABC123', 'Juan Pérez', '+54 9 351 1234567', 25000, 80, 8.5, 150.00, true),
  ('XYZ789', 'Carlos López', '+54 9 351 9876543', 20000, 60, 10.0, 140.00, true),
  ('DEF456', 'María García', '+54 9 351 5555555', 15000, 50, 9.0, 145.00, true),
  
  -- == FLOTA ESPECIALIZADA (Para pruebas de lógica de selección) ==
  -- Camión Liviano (Barato, poca carga. El sistema NO debería elegirlo para cargas grandes)
  ('LIV001', 'Pedro González', '+54 9 351 1112222', 5000, 20, 5.0, 90.00, true),

  -- Camión Mediano (Balanceado)
  ('MED002', 'Ana Martínez', '+54 9 351 3334444', 12000, 45, 7.5, 120.00, true),

  -- Camión Pesado (Caro, mucha carga. El sistema debería elegirlo SOLO si es necesario)
  ('PES003', 'Roberto Díaz', '+54 9 351 5556666', 30000, 100, 14.0, 200.00, true),

  -- Camión Larga Distancia (Optimizado en consumo, costo medio)
  ('LAR004', 'Laura Sánchez', '+54 9 351 7778888', 28000, 90, 11.0, 180.00, true),

  -- Camión "Backup" (Caro e ineficiente. Solo debería usarse si no hay otros)
  ('CAR005', 'Miguel Torres', '+54 9 351 9990000', 25000, 80, 10.0, 250.00, true),

  -- == CASOS DE BORDE (Volumen vs Peso) ==
  -- Mucho volumen, poco peso (ej: espuma/telgopor). Prueba límite de volumen.
  ('VOL006', 'Transporte Espumita', '+54 9 351 2223333', 5000, 120, 12.0, 160.00, true),

  -- Mucho peso, poco volumen (ej: metales). Prueba límite de peso.
  ('DEN007', 'Transporte Hierro', '+54 9 351 4445555', 35000, 30, 16.0, 220.00, true),

  -- == FLOTA DE REFUERZO (Para evitar "No hay camiones" en pruebas concurrentes) ==
  ('STD008', 'Flota Común SA', '+54 9 351 6667777', 25000, 80, 8.5, 150.00, true),
  ('STD009', 'Flota Común SA', '+54 9 351 6667778', 25000, 80, 8.5, 150.00, true),
  ('STD010', 'Flota Común SA', '+54 9 351 6667779', 25000, 80, 8.5, 150.00, true)

ON CONFLICT (patente) DO NOTHING;

-- 7. CONTENEDORES DE PRUEBA (Opcional)
DO $$
BEGIN
  IF EXISTS (SELECT 1 FROM information_schema.tables WHERE table_name='contenedores') THEN
    -- Insertar contenedor ligado al cliente principal si existe
    INSERT INTO contenedores (peso, volumen, id_cliente_asociado)
    SELECT 500.0, 50.0, id_cliente FROM clientes WHERE dni='12345678' LIMIT 1
    ON CONFLICT DO NOTHING;
  END IF;
END$$;