#!/usr/bin/env bash
# Prepara el proyecto Flutter: genera las carpetas android/ e ios/ y aplica los permisos que necesita la app.
# Uso (desde esta carpeta):   bash configurar.sh
set -e
if ! command -v flutter >/dev/null 2>&1; then
  echo "❌ Flutter no está instalado o no está en el PATH. Revisa la guía GUIA_FLUTTER_FEDORA44.md"; exit 1
fi

echo "▶ Generando plataformas (android, ios)..."
flutter create . --org com.jjm --project-name jjm_app --platforms=android,ios

echo "▶ Aplicando permisos..."
python3 - << 'PY'
import re, pathlib

# ---------- Android ----------
m = pathlib.Path("android/app/src/main/AndroidManifest.xml")
s = m.read_text(encoding="utf-8")
if "android.permission.INTERNET" not in s:
    s = s.replace("<application", '<uses-permission android:name="android.permission.INTERNET"/>\n    <application', 1)
if "usesCleartextTraffic" not in s:
    s = s.replace("<application", '<application android:usesCleartextTraffic="true"', 1)   # backend local por http
m.write_text(s, encoding="utf-8")

# ---------- iOS ----------
p = pathlib.Path("ios/Runner/Info.plist")
t = p.read_text(encoding="utf-8")
extra = """
	<key>NSPhotoLibraryUsageDescription</key>
	<string>Para elegir las fotos y videos de tus productos.</string>
	<key>NSCameraUsageDescription</key>
	<string>Para tomar fotos y videos de tus productos.</string>
	<key>NSMicrophoneUsageDescription</key>
	<string>Para grabar el audio de los videos de tus productos.</string>
	<key>NSAppTransportSecurity</key>
	<dict><key>NSAllowsLocalNetworking</key><true/></dict>
"""
if "NSPhotoLibraryUsageDescription" not in t:
    t = t.replace("</dict>\n</plist>", extra + "</dict>\n</plist>")
    p.write_text(t, encoding="utf-8")
PY

echo "▶ Descargando dependencias..."
rm -f test/widget_test.dart   # el de plantilla no aplica a esta app
flutter pub get

if [ -f assets/images/logo.png ]; then
  echo "▶ Generando ícono de la app con tu logo..."
  dart run flutter_launcher_icons
else
  echo "ℹ Aún no hay assets/images/logo.png; cuando lo agregues ejecuta:  dart run flutter_launcher_icons"
fi
echo "✅ Listo. Ejecuta:  flutter run   (con el backend encendido)"
