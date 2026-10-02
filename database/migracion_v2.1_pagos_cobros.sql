-- ============================================================
-- Migración incremental v2.1 — NO borra nada de tu base de datos actual.
-- Agrega la tabla que necesita el punto 10 (Pagos y Cobros del vendedor).
-- Ejecútala así (con tu base de datos ya creada):
--   mysql -u root -p jjm_ecommerce < database/migracion_v2.1_pagos_cobros.sql
-- ============================================================
USE jjm_ecommerce;

CREATE TABLE IF NOT EXISTS cuentas_cobro_aliado (
    id_cuenta INT AUTO_INCREMENT PRIMARY KEY,
    id_aliado INT NOT NULL UNIQUE,
    titular VARCHAR(150) NOT NULL,
    banco VARCHAR(100) NOT NULL,
    tipo_cuenta VARCHAR(20) NOT NULL,          -- CLABE / TARJETA
    numero_enmascarado VARCHAR(30) NOT NULL,   -- sólo los últimos 4 dígitos, nunca el número completo
    fecha_actualizacion DATETIME DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (id_aliado) REFERENCES aliados(id_aliado)
);
