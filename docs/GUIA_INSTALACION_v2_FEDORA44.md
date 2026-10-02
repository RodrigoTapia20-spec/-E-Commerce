# JJM con Causa v2 — Instalación (Fedora Linux 44)

Contenido del proyecto: `database/` (MySQL) · `backend/` (Java Spring Boot) · `webapp/` (HTML/CSS/JS) · `flutter-app/` (app Android + iOS).
Esta guía cubre **base de datos + backend + web**. La app móvil está en `GUIA_FLUTTER_FEDORA44.md`.

## 1. Requisitos (una sola vez)
```bash
sudo dnf install -y java-17-openjdk-devel maven community-mysql-server git
sudo systemctl enable --now mysqld
sudo mysql_secure_installation      # define la contraseña de root de MySQL
java -version && mvn -version       # deben mostrar Java 17
```

## 2. Base de datos (⚠ la v2 REEMPLAZA a la v1)
Si ya tenías la base de la v1, bórrala primero (solo tenía datos de prueba):
```bash
mysql -u root -p -e "DROP DATABASE IF EXISTS jjm_ecommerce;"
mysql -u root -p < database/schema.sql
```

## 3. Backend
```bash
cd backend
export DB_PASSWORD='tu_contraseña_de_mysql'
export ADMIN_EMAIL='admin@jjm.com'          # cuenta del dueño (se crea sola la 1ª vez)
export ADMIN_PASSWORD='UnaClaveSegura123!'  # ¡cámbiala!
mvn spring-boot:run
```
Verás en consola: `Cuenta de ADMINISTRADOR creada`. La API queda en `http://localhost:8080/api`.
Los archivos que suban los vendedores se guardan en `backend/uploads/`.

**Correo (recuperación de contraseña y 2FA).** Sin configurar SMTP el sistema **no se rompe**: el enlace de recuperación se imprime en la consola del backend (`[MODO DEV]`). Para correo real con Gmail necesitas una *Contraseña de aplicación* (no la normal):
```bash
export MAIL_USER='tucorreo@gmail.com'
export MAIL_PASSWORD='abcdefghijklmnop'     # contraseña de aplicación de 16 letras
export WEBAPP_URL='http://localhost:5500'   # a dónde apunta el enlace del correo
export TWOFACTOR_ENABLED=false              # true = pide código por correo al iniciar sesión
```

## 4. Web
```bash
cd webapp
python3 -m http.server 5500
```
Abre `http://localhost:5500`. (No abras los .html con doble clic: el navegador bloquearía las llamadas a la API.)
Si el backend está en otra IP, en cada HTML antes de `api.js` puedes definir `window.JJM_API_BASE_URL`.

## 5. Ruta de prueba de las 10 funciones
1. **Admin** (`admin@jjm.com`) → *Usuarios y roles* → crea un **Distribuidor**.
2. **Registrar** un **Vendedor** (RFC de 12–13 caracteres). Si queda *PENDIENTE*: Admin → *Vendedores por verificar* → Aprobar.
3. **Vendedor** → *Publicar producto*: sube **5 a 7 archivos** (imágenes y/o videos de máx. 15 s), elige paqueterías → Publicar.
4. **Vendedor** → *Campañas*: Navidad, 30 %, y (opcional) 💚 fundación con % de donación.
5. **Cliente** (registrar otro): abre el producto → elige **Costo normal** o **Promoción** + **paquetería** → carrito → dirección → Pagar (pago simulado).
6. *Mis pedidos*: ves **guía, paquetería, días de entrega**. Solicita **factura** y **califica** al vendedor.
7. **Distribuidor** → marca la guía *En tránsito* → *Entregado* (el cliente lo ve en su pedido).
8. **Recuperar contraseña**: Login → “¿Olvidaste tu contraseña?”.
9. **Chatbot** 💬: aparece en todas las pantallas y responde distinto a cliente y vendedor.
10. Ranking: Inicio → *Mejores vendedores* y Admin → *Ranking*.

## 6. Notas honestas (qué es real y qué es simulado)
- **Pagos**: simulados (modo demostración). Se conecta una pasarela real cambiando `PasarelaPagoService`.
- **Guías de envío**: el número se genera localmente y el Distribuidor actualiza el estatus a mano. Para guías reales hay que contratar las APIs de DHL/FedEx/etc. (`EnvioService.generarNumeroGuia`).
- **Factura**: se registra la solicitud con desglose de IVA y estatus *PENDIENTE_TIMBRADO*. La factura con validez fiscal (CFDI) requiere contratar un **PAC** (Facturama, FacturAPI…) y los certificados de la empresa.
- **Video**: el límite de 15 s lo valida el navegador/app al elegir el archivo; el servidor limita el tamaño (30 MB).
- **Reconocimiento facial**: pospuesto por tu decisión; hoy existe recuperación por correo. El campo `biometria_hash` queda reservado.
- **Chatbot**: responde con reglas (modo MOCK) y datos reales del catálogo; se puede conectar un proveedor de IA en `ChatbotIaService`.
