-- ============================================================
-- MIGRACION: Unificar modelo - eliminar HOLA_GYM_PERSONAL
-- Ejecutar ANTES de arrancar la app con los nuevos modelos
-- ============================================================

-- 1. Agregar columnas nuevas a USUARIO
ALTER TABLE USUARIO ADD FECHA_CONTRATACION DATE DEFAULT NULL;

-- 2. Migrar datos de HOLA_GYM_PERSONAL a USUARIO
-- Solo migrar personal cuyo DNI no exista ya en USUARIO
INSERT INTO USUARIO (DNI, NOMBRE, APELLIDO, TELEFONO, EMAIL, FECHA_REGISTRO, ESTADO, PASSWORD, ROL, FECHA_CONTRATACION)
SELECT p.DNI, p.NOMBRE, p.APELLIDO, p.TELEFONO, p.EMAIL,
       NVL(p.FECHA_CONTRATACION, SYSDATE),
       NVL(p.ESTADO, 'ACTIVO'),
       p.PASSWORD_HASH,
       'PERSONAL',
       p.FECHA_CONTRATACION
FROM HOLA_GYM_PERSONAL p
WHERE NOT EXISTS (SELECT 1 FROM USUARIO u WHERE u.DNI = p.DNI);

-- 3. Actualizar CLASES.ID_TRAINER para que referencie DNI de USUARIO
-- (Ya que los trainers ahora están en USUARIO, no en HOLA_GYM_PERSONAL)
-- No se necesita cambio si ID_TRAINER ya guarda el DNI

-- 4. Eliminar columna redundante USUARIO_ID de MEMBRESIA_USUARIO
-- (Solo si existe, puede fallar si ya se eliminó)
BEGIN
   EXECUTE IMMEDIATE 'ALTER TABLE MEMBRESIA_USUARIO DROP COLUMN USUARIO_ID';
EXCEPTION
   WHEN OTHERS THEN NULL;
END;
/

-- 5. Eliminar tabla HOLA_GYM_PERSONAL
-- DESCOMENTAR cuando se haya verificado la migración:
-- DROP TABLE HOLA_GYM_PERSONAL;

-- 6. Eliminar tabla de secuencias de Hibernate para personal
-- DESCOMENTAR cuando se haya verificado la migración:
-- DELETE FROM HIBERNATE_SEQUENCES WHERE SEQUENCE_NAME = 'personal_seq';
-- COMMIT;