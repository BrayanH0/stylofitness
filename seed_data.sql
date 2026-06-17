-- ============================================================
-- SEED DATA para pruebas de aceptacion desde cero
-- Ejecutar DESPUES de que Hibernate recree las tablas (ddl-auto=create)
-- ============================================================

-- -----------------------------------------------------------
-- 1. Usuario ADMIN
-- DNI: 99999999
-- Password: Admin123!
-- Rol: ADMIN
-- Estado: ACTIVO
-- -----------------------------------------------------------
INSERT INTO USUARIO (DNI, NOMBRE, APELLIDO, TELEFONO, EMAIL, FECHA_NACIMIENTO, DIRECCION, FECHA_REGISTRO, ESTADO, PASSWORD, ROL, FECHA_CONTRATACION)
VALUES (
    99999999,
    'Admin',
    'Principal',
    '999999999',
    'admin@stylofitness.com',
    '1990-01-01',
    'Sede Central',
    DATE '2026-05-13',
    'ACTIVO',
    '$2b$10$vBnyjvFskWSlqo3QPTgzxez.Y58GzNOZBH4KRUzaqEz1dh.l6d3Du',
    'ADMIN',
    DATE '2026-05-13'
);

-- -----------------------------------------------------------
-- 2. Usuario PERSONAL (entrenador)
-- DNI: 88888888
-- Password: Personal123!
-- Rol: PERSONAL
-- Estado: ACTIVO
-- Este usuario tambien actuara como ID_TRAINER en las clases
-- -----------------------------------------------------------
INSERT INTO USUARIO (DNI, NOMBRE, APELLIDO, TELEFONO, EMAIL, FECHA_NACIMIENTO, DIRECCION, FECHA_REGISTRO, ESTADO, PASSWORD, ROL, FECHA_CONTRATACION)
VALUES (
    88888888,
    'Carlos',
    'Entrenador',
    '888888888',
    'personal@stylofitness.com',
    '1985-06-15',
    'Sede Central',
    DATE '2026-05-13',
    'ACTIVO',
    '$2b$10$iGJBR04O94R2BmF1y0COn.WRSo4KFj4GcQQrKRf77C4d2ksLRaymS',
    'PERSONAL',
    DATE '2026-05-13'
);

COMMIT;

-- ============================================================
-- Notas:
-- - Las tablas deben haber sido creadas por Hibernate primero.
-- - Si hay error de secuencia/IDENTITY en Oracle, asegurate de
--   que las tablas existen con: DESCRIBE USUARIO;
-- - Para CLASES, usa el Portal Personal con DNI 88888888.
-- - El cliente nuevo se crea via Registro en el frontend.
-- ============================================================
