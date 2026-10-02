# Cómo poner el logo azul de la empresa

Guarda tu imagen como **`logo.png`** (PNG; ideal cuadrada de 1024×1024 px, o mínimo 512 px de alto).

## Web
Copia el archivo a `webapp/img/logo.png`. Listo: aparece en el encabezado de todas las páginas y como ícono de la pestaña.
> Como tu logo es azul y el encabezado es azul marino, la web lo muestra sobre un recuadro blanco para que se vea bien.

## App móvil (Android + iOS)
1. Copia el archivo a `flutter-app/assets/images/logo.png`.
2. Ejecuta dentro de `flutter-app/`:
   ```bash
   flutter pub get
   dart run flutter_launcher_icons
   ```
   Eso genera el **ícono de la app** en Android y iOS y el logo del encabezado de la app.

## ¿Quieres cambiar los colores de la marca al azul del logo?
Dímelo y ajusto la paleta (web y app) para que combine con tu logo.
