-- ============================================================
-- JJM Tecnologías Innovadoras, S.A. de C.V.
-- Base de Datos: E-Commerce con Causa Social — v2.0
-- Motor: MySQL 8.0+
-- Esta versión REEMPLAZA a la v1: si ya tenías la base creada, bórrala
-- primero con:  DROP DATABASE jjm_ecommerce;  y vuelve a ejecutar este script.
-- ============================================================

CREATE DATABASE IF NOT EXISTS jjm_ecommerce
  CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
USE jjm_ecommerce;

-- ------------------------------------------------------------
-- 1. SEGURIDAD, ROLES Y USUARIOS
-- Roles: ADMIN (dueño), DISTRIBUIDOR, ALIADO (vendedor), CLIENTE
-- ------------------------------------------------------------
CREATE TABLE roles (
    id_rol INT AUTO_INCREMENT PRIMARY KEY,
    nombre_rol VARCHAR(30) NOT NULL UNIQUE
);

CREATE TABLE usuarios (
    id_usuario INT AUTO_INCREMENT PRIMARY KEY,
    nombre VARCHAR(100) NOT NULL,
    apellidos VARCHAR(100) NOT NULL,
    correo VARCHAR(150) NOT NULL UNIQUE,
    telefono VARCHAR(20),
    password_hash VARCHAR(255) NOT NULL,
    id_rol INT NOT NULL,
    token_2fa VARCHAR(10),
    reset_password_token VARCHAR(100) NULL,        -- recuperación de contraseña por correo
    reset_password_expira DATETIME NULL,
    verificado_ia BOOLEAN DEFAULT FALSE,
    biometria_hash VARCHAR(255) NULL,              -- reservado: reconocimiento facial (fase 2)
    nivel_seguridad VARCHAR(20) DEFAULT 'ESTANDAR',
    estatus VARCHAR(20) DEFAULT 'ACTIVO',
    fecha_registro DATETIME DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (id_rol) REFERENCES roles(id_rol)
);

CREATE TABLE aliados (
    id_aliado INT AUTO_INCREMENT PRIMARY KEY,
    id_usuario INT NOT NULL,
    nombre_comercial VARCHAR(150) NOT NULL,
    causa_social VARCHAR(255) NULL,                -- opcional
    rfc VARCHAR(20),
    porcentaje_convenio DECIMAL(5,2) NOT NULL,
    documentos_validados BOOLEAN DEFAULT FALSE,
    estatus_verificacion VARCHAR(20) DEFAULT 'PENDIENTE',
    fecha_alta DATETIME DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (id_usuario) REFERENCES usuarios(id_usuario)
);

CREATE TABLE bitacora_seguridad (
    id_bitacora BIGINT AUTO_INCREMENT PRIMARY KEY,
    id_usuario INT NOT NULL,
    tipo_evento VARCHAR(50) NOT NULL,
    ip_origen VARCHAR(45),
    detalle VARCHAR(255),
    fecha_evento DATETIME DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (id_usuario) REFERENCES usuarios(id_usuario)
);

-- ------------------------------------------------------------
-- 2. CATÁLOGO: PRODUCTOS Y SERVICIOS
-- ------------------------------------------------------------
CREATE TABLE categorias (
    id_categoria INT AUTO_INCREMENT PRIMARY KEY,
    nombre VARCHAR(80) NOT NULL,
    id_categoria_padre INT NULL,
    FOREIGN KEY (id_categoria_padre) REFERENCES categorias(id_categoria)
);

CREATE TABLE paqueterias (
    id_paqueteria INT AUTO_INCREMENT PRIMARY KEY,
    nombre VARCHAR(60) NOT NULL,
    tiempo_entrega_dias_min INT NOT NULL,
    tiempo_entrega_dias_max INT NOT NULL,
    costo_base DECIMAL(10,2) NOT NULL,             -- costo por envío
    costo_por_kg DECIMAL(10,2) NOT NULL DEFAULT 0, -- + costo por kilo del producto
    activa BOOLEAN DEFAULT TRUE
);

CREATE TABLE productos (
    id_producto INT AUTO_INCREMENT PRIMARY KEY,
    id_aliado INT NOT NULL,
    id_categoria INT NOT NULL,
    tipo VARCHAR(15) NOT NULL,                     -- PRODUCTO / SERVICIO
    nombre VARCHAR(150) NOT NULL,
    descripcion TEXT,
    precio DECIMAL(10,2) NOT NULL,                 -- costo normal (e-commerce tradicional)
    peso_kg DECIMAL(6,2) DEFAULT 0.50,             -- para calcular el costo del envío
    existencia INT DEFAULT 0,
    verificado_ia BOOLEAN DEFAULT FALSE,
    estatus VARCHAR(20) DEFAULT 'ACTIVO',
    fecha_publicacion DATETIME DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (id_aliado) REFERENCES aliados(id_aliado),
    FOREIGN KEY (id_categoria) REFERENCES categorias(id_categoria)
);

-- Galería: de 5 a 7 elementos por producto (imágenes y/o videos de hasta 15 s)
CREATE TABLE producto_imagenes (
    id_imagen INT AUTO_INCREMENT PRIMARY KEY,
    id_producto INT NOT NULL,
    tipo VARCHAR(10) NOT NULL DEFAULT 'IMAGEN',    -- IMAGEN / VIDEO
    url_imagen VARCHAR(500) NOT NULL,
    duracion_segundos INT NULL,
    con_audio BOOLEAN NULL,
    orden INT DEFAULT 0,
    FOREIGN KEY (id_producto) REFERENCES productos(id_producto) ON DELETE CASCADE
);

-- Esquemas de mensajería habilitados por producto
CREATE TABLE producto_paqueterias (
    id_producto INT NOT NULL,
    id_paqueteria INT NOT NULL,
    PRIMARY KEY (id_producto, id_paqueteria),
    FOREIGN KEY (id_producto) REFERENCES productos(id_producto) ON DELETE CASCADE,
    FOREIGN KEY (id_paqueteria) REFERENCES paqueterias(id_paqueteria)
);

-- ------------------------------------------------------------
-- 3. PROMOCIONES Y MARKETING (campañas de temporada / causa social)
-- ------------------------------------------------------------
CREATE TABLE promociones (
    id_promocion INT AUTO_INCREMENT PRIMARY KEY,
    id_aliado INT NOT NULL,
    titulo VARCHAR(150) NOT NULL,
    descripcion TEXT,
    porcentaje_descuento DECIMAL(5,2) NOT NULL,    -- 20, 30, 40...
    tipo_temporada VARCHAR(25) DEFAULT 'GENERAL',
    es_causa_social BOOLEAN DEFAULT FALSE,
    fundacion_beneficiaria VARCHAR(150) NULL,      -- asociación civil / fundación
    porcentaje_donacion DECIMAL(5,2) NOT NULL DEFAULT 0,
    fecha_inicio DATE NOT NULL,
    fecha_fin DATE NOT NULL,
    estatus VARCHAR(20) DEFAULT 'ACTIVA',
    FOREIGN KEY (id_aliado) REFERENCES aliados(id_aliado)
);

CREATE TABLE promocion_productos (
    id_promocion INT NOT NULL,
    id_producto INT NOT NULL,
    PRIMARY KEY (id_promocion, id_producto),
    FOREIGN KEY (id_promocion) REFERENCES promociones(id_promocion) ON DELETE CASCADE,
    FOREIGN KEY (id_producto) REFERENCES productos(id_producto) ON DELETE CASCADE
);

CREATE TABLE promocion_interacciones (
    id_interaccion BIGINT AUTO_INCREMENT PRIMARY KEY,
    id_promocion INT NOT NULL,
    id_usuario INT NOT NULL,
    tipo VARCHAR(20) NOT NULL,
    fecha DATETIME DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (id_promocion) REFERENCES promociones(id_promocion),
    FOREIGN KEY (id_usuario) REFERENCES usuarios(id_usuario)
);

-- ------------------------------------------------------------
-- 4. PEDIDOS, PAGOS Y DISPERSIÓN
-- ------------------------------------------------------------
CREATE TABLE pedidos (
    id_pedido INT AUTO_INCREMENT PRIMARY KEY,
    id_usuario INT NOT NULL,
    subtotal DECIMAL(10,2) NOT NULL,
    costo_envio DECIMAL(10,2) NOT NULL DEFAULT 0,
    monto_causa DECIMAL(10,2) NOT NULL DEFAULT 0,  -- total donado a asociaciones/fundaciones
    total DECIMAL(10,2) NOT NULL,
    estatus VARCHAR(20) DEFAULT 'PENDIENTE',
    direccion_envio VARCHAR(255) NULL,
    cp_envio VARCHAR(10) NULL,
    fecha_pedido DATETIME DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (id_usuario) REFERENCES usuarios(id_usuario)
);

CREATE TABLE detalle_pedido (
    id_detalle INT AUTO_INCREMENT PRIMARY KEY,
    id_pedido INT NOT NULL,
    id_producto INT NOT NULL,
    id_aliado INT NOT NULL,
    cantidad INT NOT NULL,
    modalidad VARCHAR(15) NOT NULL DEFAULT 'NORMAL', -- NORMAL / PROMOCION
    id_promocion INT NULL,
    precio_lista DECIMAL(10,2) NOT NULL,
    precio_unitario DECIMAL(10,2) NOT NULL,          -- precio realmente cobrado
    porcentaje_descuento DECIMAL(5,2) NOT NULL DEFAULT 0,
    porcentaje_aplicado DECIMAL(5,2) NOT NULL,       -- % de JJM por convenio
    monto_aliado DECIMAL(10,2) NOT NULL,
    monto_empresa DECIMAL(10,2) NOT NULL,
    monto_causa DECIMAL(10,2) NOT NULL DEFAULT 0,    -- donación a la fundación
    FOREIGN KEY (id_pedido) REFERENCES pedidos(id_pedido) ON DELETE CASCADE,
    FOREIGN KEY (id_producto) REFERENCES productos(id_producto),
    FOREIGN KEY (id_aliado) REFERENCES aliados(id_aliado),
    FOREIGN KEY (id_promocion) REFERENCES promociones(id_promocion)
);

CREATE TABLE metodos_pago (
    id_metodo INT AUTO_INCREMENT PRIMARY KEY,
    nombre VARCHAR(50) NOT NULL
);

CREATE TABLE pagos (
    id_pago INT AUTO_INCREMENT PRIMARY KEY,
    id_pedido INT NOT NULL,
    id_metodo INT NOT NULL,
    referencia_pasarela VARCHAR(120),
    monto DECIMAL(10,2) NOT NULL,
    estatus VARCHAR(20) DEFAULT 'PROCESANDO',
    fecha_pago DATETIME DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (id_pedido) REFERENCES pedidos(id_pedido),
    FOREIGN KEY (id_metodo) REFERENCES metodos_pago(id_metodo)
);

CREATE TABLE dispersiones (
    id_dispersion INT AUTO_INCREMENT PRIMARY KEY,
    id_pago INT NOT NULL,
    id_aliado INT NOT NULL,
    monto_aliado DECIMAL(10,2) NOT NULL,
    monto_empresa DECIMAL(10,2) NOT NULL,
    estatus VARCHAR(20) DEFAULT 'PENDIENTE',
    fecha_dispersion DATETIME NULL,
    FOREIGN KEY (id_pago) REFERENCES pagos(id_pago),
    FOREIGN KEY (id_aliado) REFERENCES aliados(id_aliado)
);

-- ------------------------------------------------------------
-- 5. ENVÍOS (una guía por cada producto del pedido)
-- ------------------------------------------------------------
CREATE TABLE envios (
    id_envio INT AUTO_INCREMENT PRIMARY KEY,
    id_detalle INT NOT NULL,
    id_paqueteria INT NOT NULL,
    numero_guia VARCHAR(60) NOT NULL,
    costo_envio DECIMAL(10,2) NOT NULL,
    tiempo_entrega_estimado_dias INT NOT NULL,
    fecha_estimada_entrega DATE NULL,
    estatus VARCHAR(20) DEFAULT 'PENDIENTE',        -- PENDIENTE / EN_TRANSITO / ENTREGADO
    fecha_generacion DATETIME DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (id_detalle) REFERENCES detalle_pedido(id_detalle) ON DELETE CASCADE,
    FOREIGN KEY (id_paqueteria) REFERENCES paqueterias(id_paqueteria)
);

-- ------------------------------------------------------------
-- 5.1 CUENTAS DE COBRO DEL VENDEDOR (dónde recibe sus pagos)
-- ------------------------------------------------------------
CREATE TABLE cuentas_cobro_aliado (
    id_cuenta INT AUTO_INCREMENT PRIMARY KEY,
    id_aliado INT NOT NULL UNIQUE,
    titular VARCHAR(150) NOT NULL,
    banco VARCHAR(100) NOT NULL,
    tipo_cuenta VARCHAR(20) NOT NULL,          -- CLABE / TARJETA
    numero_enmascarado VARCHAR(30) NOT NULL,   -- sólo los últimos 4 dígitos, nunca el número completo
    fecha_actualizacion DATETIME DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (id_aliado) REFERENCES aliados(id_aliado)
);

-- ------------------------------------------------------------
-- 6. FACTURACIÓN
-- ------------------------------------------------------------
CREATE TABLE facturas (
    id_factura INT AUTO_INCREMENT PRIMARY KEY,
    id_pedido INT NOT NULL UNIQUE,
    id_usuario INT NOT NULL,
    rfc_receptor VARCHAR(20) NOT NULL,
    razon_social VARCHAR(150) NOT NULL,
    uso_cfdi VARCHAR(10) DEFAULT 'G03',
    regimen_fiscal VARCHAR(10) NULL,
    cp_fiscal VARCHAR(10) NULL,
    correo_envio VARCHAR(150) NULL,
    folio_interno VARCHAR(30) NOT NULL,
    subtotal DECIMAL(10,2) NOT NULL,
    iva DECIMAL(10,2) NOT NULL,
    total DECIMAL(10,2) NOT NULL,
    estatus VARCHAR(25) DEFAULT 'PENDIENTE_TIMBRADO',
    uuid_fiscal VARCHAR(50) NULL,
    fecha_emision DATETIME DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (id_pedido) REFERENCES pedidos(id_pedido),
    FOREIGN KEY (id_usuario) REFERENCES usuarios(id_usuario)
);

-- ------------------------------------------------------------
-- 7. REPUTACIÓN, RESEÑAS Y EXPERIENCIA DE USUARIO
-- ------------------------------------------------------------
CREATE TABLE resenas (
    id_resena INT AUTO_INCREMENT PRIMARY KEY,
    id_producto INT NOT NULL,
    id_usuario INT NOT NULL,
    calificacion TINYINT NOT NULL CHECK (calificacion BETWEEN 1 AND 5),
    comentario TEXT,
    fecha DATETIME DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (id_producto) REFERENCES productos(id_producto),
    FOREIGN KEY (id_usuario) REFERENCES usuarios(id_usuario)
);

-- Ranking de vendedores
CREATE TABLE calificaciones_vendedor (
    id_calificacion INT AUTO_INCREMENT PRIMARY KEY,
    id_aliado INT NOT NULL,
    id_usuario INT NOT NULL,
    id_pedido INT NULL,
    calificacion TINYINT NOT NULL CHECK (calificacion BETWEEN 1 AND 5),
    comentario TEXT,
    fecha DATETIME DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (id_aliado) REFERENCES aliados(id_aliado),
    FOREIGN KEY (id_usuario) REFERENCES usuarios(id_usuario),
    FOREIGN KEY (id_pedido) REFERENCES pedidos(id_pedido),
    UNIQUE KEY unico_por_pedido (id_aliado, id_usuario, id_pedido)
);

CREATE TABLE encuestas_experiencia (
    id_encuesta INT AUTO_INCREMENT PRIMARY KEY,
    id_usuario INT NOT NULL,
    id_pedido INT NULL,
    puntuacion_nps TINYINT,
    comentario TEXT,
    fecha DATETIME DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (id_usuario) REFERENCES usuarios(id_usuario),
    FOREIGN KEY (id_pedido) REFERENCES pedidos(id_pedido)
);

-- ------------------------------------------------------------
-- 8. QUEJAS Y SUGERENCIAS
-- ------------------------------------------------------------
CREATE TABLE quejas_sugerencias (
    id_queja INT AUTO_INCREMENT PRIMARY KEY,
    id_usuario INT NOT NULL,
    tipo VARCHAR(15) NOT NULL,                      -- QUEJA / SUGERENCIA
    asunto VARCHAR(150) NOT NULL,
    descripcion TEXT NOT NULL,
    estatus VARCHAR(20) DEFAULT 'ABIERTA',
    respuesta TEXT,
    fecha_creacion DATETIME DEFAULT CURRENT_TIMESTAMP,
    fecha_resolucion DATETIME NULL,
    FOREIGN KEY (id_usuario) REFERENCES usuarios(id_usuario)
);

-- ------------------------------------------------------------
-- 9. CHATBOT / IA
-- ------------------------------------------------------------
CREATE TABLE chatbot_conversaciones (
    id_conversacion BIGINT AUTO_INCREMENT PRIMARY KEY,
    id_usuario INT NULL,
    canal VARCHAR(20) DEFAULT 'WEB',
    contexto VARCHAR(20) DEFAULT 'CLIENTE',        -- CLIENTE / VENDEDOR / DISTRIBUIDOR / ADMIN
    fecha_inicio DATETIME DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (id_usuario) REFERENCES usuarios(id_usuario)
);

CREATE TABLE chatbot_mensajes (
    id_mensaje BIGINT AUTO_INCREMENT PRIMARY KEY,
    id_conversacion BIGINT NOT NULL,
    emisor VARCHAR(10) NOT NULL,                    -- USUARIO / BOT
    mensaje TEXT NOT NULL,
    fecha DATETIME DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (id_conversacion) REFERENCES chatbot_conversaciones(id_conversacion) ON DELETE CASCADE
);

-- ------------------------------------------------------------
-- DATOS INICIALES
-- ------------------------------------------------------------
INSERT INTO roles (nombre_rol) VALUES ('CLIENTE'), ('ALIADO'), ('DISTRIBUIDOR'), ('ADMIN');

INSERT INTO metodos_pago (nombre) VALUES
 ('TARJETA_CREDITO'), ('TARJETA_DEBITO'), ('TRANSFERENCIA'),
 ('OXXO'), ('SEVEN'), ('VALE_DESPENSA'), ('BILLETERA_DIGITAL');

INSERT INTO categorias (nombre) VALUES
 ('Alimentos y Bebidas'), ('Artesanías'), ('Servicios Profesionales'),
 ('Hogar'), ('Ropa y Accesorios'), ('Otros con Causa');

-- costo = costo_base + (costo_por_kg × peso del producto × cantidad)
INSERT INTO paqueterias (nombre, tiempo_entrega_dias_min, tiempo_entrega_dias_max, costo_base, costo_por_kg) VALUES
 ('DHL Express',        1, 3, 149.00, 25.00),
 ('FedEx',              2, 4, 139.00, 22.00),
 ('Estafeta',           3, 5, 109.00, 15.00),
 ('Paquetexpress',      3, 6,  99.00, 12.00),
 ('Correos de México',  5, 10, 69.00, 8.00);
