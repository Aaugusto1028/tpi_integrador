-- Script de Datos Iniciales para TPI Logística
-- Asegura que existan los datos mínimos necesarios para las pruebas

-- ============================================
-- 1. ESTADOS DE TRAMO (si no existen)
-- ============================================
INSERT INTO estado_tramo (id_estado_tramo, nombre) VALUES 
    (1, 'ESTIMADO'),
    (2, 'ASIGNADO'),
    (3, 'INICIADO'),
    (4, 'FINALIZADO')
ON CONFLICT (id_estado_tramo) DO NOTHING;

-- ============================================
-- 2. TIPOS DE TRAMO (si no existen)
-- ============================================
INSERT INTO tipo_tramo (id_tipo_tramo, nombre) VALUES 
    (1, 'TERRESTRE'),
    (2, 'AEREO'),
    (3, 'MARITIMO')
ON CONFLICT (id_tipo_tramo) DO NOTHING;

-- ============================================
-- 3. DEPÓSITOS (si no existen)
-- ============================================
INSERT INTO depositos (id_deposito, nombre, latitud, longitud, costo_estadio_diario) VALUES 
    (1, 'Depósito Córdoba', -31.4201, -64.1888, 500.00),
    (2, 'Depósito Mendoza', -31.5350, -64.2637, 450.00)
ON CONFLICT (id_deposito) DO NOTHING;

-- ============================================
-- 4. TARIFAS (si no existen)
-- ============================================
INSERT INTO tarifas (id_tarifa, precio_litro, costo_estadio_diario) VALUES 
    (1, 150.00, 500.00)
ON CONFLICT (id_tarifa) DO NOTHING;

-- ============================================
-- 5. CAMIONES (actualizar con datos completos)
-- ============================================
-- Actualizar ABC123 con todos los datos necesarios
UPDATE camiones SET 
    nombre_transportista = 'Juan Pérez',
    telefono_transportista = '+54 9 351 1234567',
    capacidad_peso = 25000,
    capacidad_volumen = 80,
    consumo_combustible_km = 8.5,
    costo_por_km = 150.00,
    disponibilidad = true
WHERE patente = 'ABC123';

-- Si ABC123 no existe, insertarlo
INSERT INTO camiones (patente, nombre_transportista, telefono_transportista, capacidad_peso, capacidad_volumen, consumo_combustible_km, costo_por_km, disponibilidad) 
VALUES ('ABC123', 'Juan Pérez', '+54 9 351 1234567', 25000, 80, 8.5, 150.00, true)
ON CONFLICT (patente) DO NOTHING;

-- Insertar otros camiones de prueba si es necesario
INSERT INTO camiones (patente, nombre_transportista, telefono_transportista, capacidad_peso, capacidad_volumen, consumo_combustible_km, costo_por_km, disponibilidad) 
VALUES 
    ('XYZ789', 'Carlos López', '+54 9 351 9876543', 20000, 60, 10.0, 140.00, true),
    ('DEF456', 'María García', '+54 9 351 5555555', 15000, 50, 9.0, 145.00, true),
    ('GHI012', 'Roberto Martínez', '+54 9 351 6666666', 30000, 100, 12.0, 160.00, true)
ON CONFLICT (patente) DO NOTHING;

-- ============================================
-- 6. CLIENTES DE PRUEBA (si es necesario)
-- ============================================
INSERT INTO clientes (dni, nombre, apellido, email, telefono) 
VALUES 
    ('12345678', 'Cliente', 'Test', 'cliente@test.com', '+54 9 351 1111111')
ON CONFLICT (dni) DO NOTHING;

-- ============================================
-- Verificación de datos insertados
-- ============================================
SELECT 'Verificación de Datos Iniciales:' as info;
SELECT COUNT(*) as total_camiones FROM camiones;
SELECT COUNT(*) as total_depositos FROM depositos;
SELECT COUNT(*) as total_estado_tramo FROM estado_tramo;
SELECT COUNT(*) as total_tipo_tramo FROM tipo_tramo;
SELECT COUNT(*) as total_tarifas FROM tarifas;

-- Ver camión ABC123 completo
SELECT 'Datos del Camión ABC123:' as info;
SELECT patente, nombre_transportista, capacidad_peso, capacidad_volumen, costo_por_km, consumo_combustible_km, disponibilidad 
FROM camiones 
WHERE patente = 'ABC123';
