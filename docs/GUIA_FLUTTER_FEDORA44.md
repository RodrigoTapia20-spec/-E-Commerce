# App móvil JJM (Flutter) — Android + iOS con un solo código

Reemplaza a la app anterior en Java. **Un solo proyecto** (`flutter-app/`) genera la app de Android y la de iPhone.

## ⚠ Lo que Fedora sí puede y lo que no
| Plataforma | Desde Fedora 44 |
|---|---|
| Android (emulador o celular) | ✅ Sí |
| iOS (iPhone) | ❌ **No se puede compilar en Linux.** Apple exige **macOS + Xcode**. El mismo código sirve, pero el paso de compilar/instalar en iPhone se hace en una Mac (o con un servicio en la nube como Codemagic). |

## 1. Instalar Flutter
```bash
sudo dnf install -y git curl unzip xz zip mesa-libGLU clang cmake ninja-build pkgconf-pkg-config gtk3-devel
cd ~ && git clone https://github.com/flutter/flutter.git -b stable
echo 'export PATH="$HOME/flutter/bin:$PATH"' >> ~/.bashrc && source ~/.bashrc
flutter --version
```
Necesitas también **Android Studio** (para el SDK de Android y el emulador; ya lo tienes de la guía anterior):
```bash
flutter config --android-sdk ~/Android/Sdk
flutter doctor --android-licenses     # acepta todo con "y"
flutter doctor                        # Android debe salir con ✓ (Xcode/iOS aparecerá ✗ en Linux: es normal)
```

## 2. Preparar el proyecto
```bash
cd flutter-app
bash configurar.sh
```
Ese script genera las carpetas `android/` e `ios/`, agrega los permisos (internet, galería, cámara, micrófono) y descarga las dependencias.

## 3. Ejecutar (con el backend encendido)
- **Emulador Android**: `flutter run` (usa `10.0.2.2:8080`, que es tu computadora).
- La dirección del servidor también se cambia dentro de la app: ícono ⚙ en *Iniciar sesión*.
- **Celular Android físico** (USB con depuración, misma red Wi-Fi):
  ```bash
  sudo firewall-cmd --add-port=8080/tcp          # permite el acceso al backend
  ip -4 addr | grep inet                          # tu IP, ej. 192.168.1.50
  flutter run --dart-define=API_URL=http://192.168.1.50:8080/api
  ```
  Para que el enlace del correo de recuperación funcione desde el celular arranca el backend con `export WEBAPP_URL='http://192.168.1.50:5500'` (o, dentro de la app, pega el enlace del correo en la pantalla *Recuperar contraseña*).

## 4. Generar el APK
```bash
flutter build apk --release --dart-define=API_URL=http://TU_SERVIDOR:8080/api
# resultado: build/app/outputs/flutter-apk/app-release.apk
```

## 5. iPhone (en una Mac con Xcode)
```bash
cd flutter-app && bash configurar.sh
open ios/Runner.xcworkspace        # firma con tu Apple ID en Signing & Capabilities
flutter run --dart-define=API_URL=http://IP_DE_TU_MAC:8080/api
```
Para publicar en App Store hace falta la cuenta Apple Developer (de paga).

## 6. Qué incluye la app
Cliente: catálogo, buscador y categorías, galería con **imágenes y video**, **costo normal vs promoción**, **paquetería** con costo y días, carrito, pago, **pedidos con guía y seguimiento**, **factura**, **calificar vendedor**, quejas.
Vendedor: resumen y ranking, **publicar con 5–7 archivos** (fotos/videos ≤ 15 s), productos, **campañas de temporada / causa social**.
Distribuidor: actualizar guías. Todos: **chatbot 💬 en cada pantalla**, login con 👁, **recuperar contraseña**.
El **Administrador** usa el panel web (aprobar vendedores, usuarios, reportes).
